package fr.corpauration.user

import com.clevercloud.biscuit.token.Biscuit
import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.conf.ConfResource
import fr.corpauration.conf.RegisterWebhookConf
import fr.corpauration.group.ADMIN
import fr.corpauration.group.GroupRepository
import fr.corpauration.security.WrongEmailDomain
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.BaseResource
import fr.corpauration.utils.NeedToBeInGroups
import fr.corpauration.utils.RepositoryGenerator
import fr.corpauration.utils.webhook.Webhook
import fr.corpauration.utils.webhook.discord.DiscordWebhook
import fr.corpauration.utils.webhook.discord.DiscordWebhookData
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.oidc.UserInfo
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.context.control.ActivateRequestContext
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/user")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class UserResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @Inject
    @RepositoryGenerator(table = "users", id = UUID::class, entity = UserEntity::class)
    lateinit var userRepository: UserRepository

    @Inject
    @RepositoryGenerator(
        table = "students", id = UUID::class, entity = StudentEntity::class
    )
    lateinit var studentRepository: StudentRepository

    @Inject
    @RepositoryGenerator(
        table = "professors", id = UUID::class, entity = ProfessorEntity::class
    )
    lateinit var professorRepository: ProfessorRepository

    @Inject
    @RepositoryGenerator(
        table = "cytech_students", id = Int::class, entity = CytechStudent::class
    )
    lateinit var cytechStudentRepository: CytechStudentRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var registry: MeterRegistry

    @Inject
    lateinit var confResource: ConfResource

    @Inject
    lateinit var webhookConf: RegisterWebhookConf

    val webhook: Webhook = DiscordWebhook()

    @GET
    @AccountExist
    @NeedToBeInGroups(ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): Multi<UserEntity> {
        return userRepository.getAll()
    }

    @GET
    @AccountExist
    @NeedToBeInGroups(ADMIN)
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: UUID): Uni<UserEntity> {
        return userRepository.findById(id)
    }

    @GET
    @AccountExist
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    fun getMe(): Uni<UserEntity> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem().transform {
            it.first()
        }
    }

    @GET
    @Path("/isRegistered")
    fun isRegistered(): Uni<Boolean> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem().transform {
            it.size == 1
        }
    }

    @ActivateRequestContext
    @POST
    fun register(json: JsonNode): Uni<Unit> {
        if (!identity.principal.name.endsWith("@cy-tech.fr")) throw WrongEmailDomain()
        if (!json.hasNonNull("person_type") || !json.get("person_type").isInt || (json.get("person_type")
                .asInt() != UserType.STUDENT.ordinal && json.get("person_type")
                .asInt() != UserType.PROFESSOR.ordinal) || (json.get("person_type")
                .asInt() == UserType.STUDENT.ordinal && (!json.hasNonNull("student_id") || !json.get("student_id").isInt))
        ) throw BadRequestException("Malformed request")
        return userRepository.findBy(identity.principal.name, "email").collect().asList().flatMap {
            if (it.size == 0) {
                val userInfo = (identity.attributes["userinfo"]!! as UserInfo)
                val user = UserEntity(
                    email = identity.principal.name,
                    firstname = userInfo.getString("given_name"),
                    lastname = userInfo.getString("family_name"),
                    birthday = if (json.get("birthday") != null && !json.get("birthday").isNull) LocalDate.parse(
                        json.get("birthday").asText()
                    ) else null,
                    type = json.get("person_type").asInt()
                )
                when (json.get("person_type").asInt()) {
                    UserType.STUDENT.ordinal -> cytechStudentRepository.findById(json.get("student_id").asInt())
                        .onFailure().transform { UnknownStudentId() }

                    UserType.PROFESSOR.ordinal -> Uni.createFrom().voidItem()
                    else -> Uni.createFrom().failure<Void?>(UnknownPersonType()).replaceWithVoid()
                }.flatMap {
                    userRepository.save(user)
                }.flatMap {
                    when (json.get("person_type").asInt()) {
                        UserType.STUDENT.ordinal -> studentRepository.save(
                            StudentEntity(
                                id = user.id, student_id = json.get("student_id").asInt()
                            )
                        ).onItem().transform { registry.counter("cyrel_backend_registered_users").increment() }

                        UserType.PROFESSOR.ordinal -> professorRepository.save(ProfessorEntity(id = user.id))
                            .onItem().transform { registry.counter("cyrel_backend_registered_users").increment() }

                        else -> throw UnknownPersonType()
                    }
                }.flatMap { confResource.webhookUrl }.map { url ->
                    runBlocking {
                        webhook.send(
                            DiscordWebhookData(
                                url,
                                """**${if (user.type == UserType.STUDENT.ordinal) "Student" else "Professor"} `${user.id}` registered**
                                            |${if (user.type == UserType.STUDENT.ordinal) webhookConf.student() else webhookConf.professor()}
                                        """.trimMargin()
                            )
                        )
                    }
                }
            } else throw AlreadyRegistered()
        }
    }

    private fun getPreregistrationBiscuit(b64: String): Uni<PreregistrationBiscuit> {
        return confResource.referentRegisteringPublicKey.flatMap {
            val biscuit = try {
                Biscuit.from_b64url(b64).verify(it)
            } catch (e: Exception) {
                throw PreregistrationCorruptBiscuit()
            }
            val facts = biscuit.authorizer().query("data(\$promo, \$group) <- promo(\$promo), group(\$group)")
            val terms = facts.first().terms()
            val preregistrationBiscuit =
                PreregistrationBiscuit(promo = terms[0].toString().toInt(), group = terms[1].toString().toInt())

            groupRepository.findById(preregistrationBiscuit.group).map {
                if (it.referent == null) preregistrationBiscuit
                else throw PreregistrationBiscuitExpired()
            }
        }
    }

    @GET
    @Path("/preregistration/{biscuit}")
    fun getPreregistrationInfo(@PathParam("biscuit") b64: String): Uni<PreregistrationBiscuit> {
        if (!identity.principal.name.endsWith("@cy-tech.fr")) throw WrongEmailDomain()
        return getPreregistrationBiscuit(b64)
    }

    @POST
    @Path("/preregistration/{biscuit}")
    fun register(@PathParam("biscuit") b64: String, json: JsonNode): Uni<Unit> {
        if (!identity.principal.name.endsWith("@cy-tech.fr")) throw WrongEmailDomain()
        if (!json.hasNonNull("student_id") || !json.get("student_id").isInt) throw BadRequestException("Malformed request")

        return getPreregistrationBiscuit(b64).flatMap { biscuit ->
            userRepository.findBy(identity.principal.name, "email").collect().asList().flatMap {
                if (it.size == 0) {
                    val userInfo = (identity.attributes["userinfo"]!! as UserInfo)
                    val user = UserEntity(
                        email = identity.principal.name,
                        firstname = userInfo.getString("given_name"),
                        lastname = userInfo.getString("family_name"),
                        birthday = if (json.get("birthday") != null && !json.get("birthday").isNull) LocalDate.parse(
                            json.get("birthday").asText()
                        ) else null,
                        type = UserType.STUDENT.ordinal,
                        tags = mapOf("promo" to biscuit.promo.toString(), "group" to biscuit.group.toString())
                    )
                    cytechStudentRepository.findById(json.get("student_id").asInt()).onFailure()
                        .transform { UnknownStudentId() }.flatMap {
                            userRepository.save(user)
                        }.flatMap {
                            studentRepository.save(
                                StudentEntity(
                                    id = user.id, student_id = json.get("student_id").asInt()
                                )
                            ).onItem().transform { registry.counter("cyrel_backend_registered_users").increment() }
                        }.flatMap { groupRepository.findById(biscuit.group) }.flatMap { group ->
                            group.referent = user.id
                            groupRepository.update(group)
                        }.flatMap { confResource.webhookUrl }.map { url ->
                            runBlocking {
                                webhook.send(
                                    DiscordWebhookData(
                                        url, """**Student `${user.id}` registered with preregistration biscuit**
                                            |${webhookConf.student()}
                                        """.trimMargin()
                                    )
                                )
                            }
                        }
                } else throw AlreadyRegistered()
            }
        }
    }
}
