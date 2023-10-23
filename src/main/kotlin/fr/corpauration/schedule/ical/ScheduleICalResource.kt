package fr.corpauration.schedule.ical

import biscuit.format.schema.Schema
import com.clevercloud.biscuit.crypto.KeyPair
import com.clevercloud.biscuit.crypto.PublicKey
import com.clevercloud.biscuit.token.Biscuit
import fr.corpauration.group.GroupRepository
import fr.corpauration.schedule.CourseCategory
import fr.corpauration.schedule.CourseRepository
import fr.corpauration.schedule.toColor
import fr.corpauration.user.UserEntity
import fr.corpauration.user.UserRepository
import fr.corpauration.user.UserType
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.RepositoryGenerator
import fr.corpauration.utils.UserNotRegistered
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.LocationType
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.TimeZoneRegistry
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VLocation
import net.fortuna.ical4j.model.parameter.Cn
import net.fortuna.ical4j.model.parameter.CuType
import net.fortuna.ical4j.model.parameter.Role
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion
import java.net.URI
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/schedule/ical")
@ApplicationScoped
class ScheduleICalResource {

    @Inject
    @RepositoryGenerator(
        table = "icaltokens", id = String::class, entity = ICalTokenEntity::class
    )
    lateinit var iCalTokenRepository: ICalTokenRepository

    @Inject
    lateinit var courseRepository: CourseRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @POST
    @Authenticated
    @AccountExist
    @Produces(MediaType.TEXT_PLAIN)
    fun createToken(): Uni<String> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().map { it[0] }
            .flatMap { user ->
                iCalTokenRepository.findBy(user.id, "user").collect().asList().map {
                    if (it.isEmpty()) user
                    else throw ScheduleICalNeedRecoverException(user, it.first())
                }
            }.flatMap {
                val keypair = KeyPair()
                buildSaveBiscuit(it, keypair)
            }.onFailure(ScheduleICalNeedRecoverException::class.java)
            .recoverWithUni { it -> refreshToken((it as ScheduleICalNeedRecoverException).user, it.token) }
    }

    private fun buildSaveBiscuit(it: UserEntity, keypair: KeyPair): Uni<String> {
        val biscuitBuilder = Biscuit.builder(keypair).add_authority_fact("user(\"${it.id}\")")
        val biscuit = when (it.type) {
            UserType.STUDENT.ordinal -> {
                biscuitBuilder.add_authority_fact("professor(\"false\")")
                    .add_authority_fact("group(\"${it.tags["group"]}\")")
            }

            UserType.PROFESSOR.ordinal -> {
                biscuitBuilder.add_authority_fact("professor(\"true\")").add_authority_fact("group(\"-100\")")
            }

            else -> throw UserNotRegistered()
        }.build()
        return iCalTokenRepository.save(
            ICalTokenEntity(
                id = biscuit.revocation_identifiers().first().serialize_b64url(),
                user = it,
                private = keypair.toHex(),
                public = keypair.public_key().toHex()
            )
        ).map { biscuit.serialize_b64url() }
    }

    private fun refreshToken(user: UserEntity, token: ICalTokenEntity): Uni<String> {
        return iCalTokenRepository.delete(token).flatMap {
            val keypair = KeyPair(token.private)
            buildSaveBiscuit(user, keypair)
        }
    }

    @GET
    @Path("/{biscuit}")
    @Produces("text/calendar")
    fun getICal(@PathParam("biscuit") b64: String): Uni<String> {
        val biscuit = Biscuit.from_b64url(b64)
        val bid = biscuit.revocation_identifiers().first().serialize_b64url()
        return iCalTokenRepository.findById(bid).flatMap {
            val b = try {
                biscuit.verify(PublicKey(Schema.PublicKey.Algorithm.Ed25519, it.public))
            } catch (e: Exception) {
                throw ScheduleICalCorruptedTokenException()
            }
            val facts = b.authorizer()
                .query("data(\$id, \$professor, \$group) <- user(\$id), professor(\$professor), group(\$group)")
            val terms = facts.first().terms()
            val p = object {
                val id = UUID.fromString(terms[0].toString().replace("\"", ""))
                val professor = terms[1].toString().replace("\"", "") == "true"
                val group = terms[2].toString().replace("\"", "").toInt()
            }

            var monday = LocalDateTime.now().withHour(1).withMinute(0).withSecond(0)
            monday = monday.minusDays(monday.dayOfWeek.ordinal.toLong())
            val start = monday.minusWeeks(1)
            val end = monday.plusWeeks(3)

            if (p.professor) {
                userRepository.findById(p.id).flatMap {
                    courseRepository.wrapperRetrieveScheduleForProfessorBetweenDate(
                        "${it.firstname} ${it.lastname}", start, end
                    ).collect().asList()
                }.map {
                    it.distinctBy { Pair(it.start, it.subject) }
                }
            } else {
                courseRepository.wrapperRetrieveScheduleForGroupBetweenDate(p.group, start, end).collect().asList()
            }
        }.map {
            val calendar = Calendar()
            calendar.add(ProdId("-//Corpauration//Cyrel//FR"))
            calendar.add(ImmutableVersion.VERSION_2_0)
            calendar.add(ImmutableCalScale.GREGORIAN)
            val registry: TimeZoneRegistry = CalendarBuilder().registry
            val timezone: TimeZone = registry.getTimeZone("Europe/Paris")
            for (course in it) {
                val start = ZonedDateTime.of(course.start, timezone.toZoneId())
                val end = ZonedDateTime.of(course.end, timezone.toZoneId())
                val event = VEvent(start, end, course.subject)
                event.add(Uid(course.id))
                for (t in course.teachers.split(",")) {
                    val organizer = Organizer()
                    organizer.add<Property>(Cn(t))
                    event.add(organizer)
                    val attendee = Attendee(URI.create("mailto:${t.replace(" ", ".")}@localhost"))
                    attendee.add<Property>(Role.CHAIR)
                    attendee.add<Property>(Cn(t))
                    attendee.add<Property>(CuType("INDIVIDUAL"))
                    event.add(attendee)
                }
                val attendee = Attendee(URI.create("mailto:you@localhost"))
                attendee.add<Property>(Role.OPT_PARTICIPANT)
                attendee.add<Property>(Cn("YOU"))
                attendee.add<Property>(CuType("INDIVIDUAL"))
                event.add(attendee)
                event.add(Categories(CourseCategory.values()[course.category].name))
                val color = Color()
                color.value = CourseCategory.values()[course.category].toColor()
                event.add(color)
                val vLocation = VLocation()
                vLocation.add(LocationType(LocationType.classroom.name))
                vLocation.add(Name(course.rooms))
                event.add(Location(vLocation))
                calendar.add(event)
            }

            calendar.toString()
        }.onFailure(NullPointerException::class.java).transform {
            ScheduleICalTokenNotFoundException()
        }
    }
}