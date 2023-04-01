package fr.corpauration.schedule

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.ADMIN
import fr.corpauration.group.GroupRepository
import fr.corpauration.user.UserRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.CustomSql
import fr.corpauration.utils.NeedToBeProfessorOrInGroups
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/schedule")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class ScheduleResource {
    @Inject
    @RepositoryGenerator(
        table = "courses",
        id = String::class,
        entity = CourseEntity::class
    )
    lateinit var courseRepository: CourseRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    @GET
    @Path("/{id}")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("id") id: String): Uni<CourseEntity> = courseRepository.findById(id)

    @POST
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getFromTo(json: JsonNode): Uni<List<CourseEntity>> {
        if (!json.hasNonNull("group") || !json.get("group").isInt || !json.hasNonNull(
                "start"
            ) || !json.get("start").isTextual || !json.hasNonNull("end") || !json.get("end").isTextual
        ) throw BadRequestException()
        val start = LocalDateTime.parse(json.get("start").asText())
        val end = LocalDateTime.parse(json.get("end").asText())

        return wrapperRetrieveScheduleForGroupBetweenDate(json.get("group").asInt(), start, end).collect().asList()
    }

    @GET
    @Path("/professors")
    @NeedToBeProfessorOrInGroups(ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    fun getScheduleProfessors(): Uni<List<String>> {
        return wrapperRetrieveScheduleProfessors().collect().asList()
    }

    @POST
    @Path("/professors")
    @NeedToBeProfessorOrInGroups(ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfessorScheduleFromTo(json: JsonNode): Uni<List<CourseEntity>> {
        if (!json.hasNonNull("professor") || !json.get("professor").isTextual || !json.hasNonNull(
                "start"
            ) || !json.get("start").isTextual || !json.hasNonNull("end") || !json.get("end").isTextual
        ) throw BadRequestException()
        val start = LocalDateTime.parse(json.get("start").asText())
        val end = LocalDateTime.parse(json.get("end").asText())

        return wrapperRetrieveScheduleProfessors().collect().asList().flatMap {
            if (it.contains(json.get("professor").asText()))
                wrapperRetrieveScheduleForProfessorBetweenDate(json.get("professor").asText(), start, end).collect()
                    .asList().onItem().transform {
                        it.distinctBy { Pair(it.start, it.subject) }
                    }
            else throw Exception()
        }
    }

    @CustomSql(
        """
            select c.* from courses as c
            join courses_groups as gc on c.id = gc.id
            where gc.ref = $1 and c.start >= $2 and c."end" <= $3
    """, entity = CourseEntity::class
    )
    fun wrapperRetrieveScheduleForGroupBetweenDate(
        group: Int,
        start: LocalDateTime,
        end: LocalDateTime
    ): Multi<CourseEntity> {
        return courseRepository.wrapperRetrieveScheduleForGroupBetweenDate(group, start, end)
    }

    @CustomSql(
        """
        select distinct regexp_split_to_table(teachers, ',') from courses where teachers <> '' and teachers not ilike '%vac_tempo_%' ORDER BY regexp_split_to_table(teachers, ',') asc 
    """, entity = CourseEntity::class
    )
    fun wrapperRetrieveScheduleProfessors(): Multi<String> {
        return courseRepository.wrapperRetrieveScheduleProfessors()
    }

    @CustomSql(
        """
        select c.* from courses c, courses_groups cg
        where c.id = cg.id and c.teachers ilike '%'||$1||'%' and c.start >= $2 and c."end" <= $3
    """, entity = CourseEntity::class
    )
    fun wrapperRetrieveScheduleForProfessorBetweenDate(
        professor: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): Multi<CourseEntity> {
        return courseRepository.wrapperRetrieveScheduleForProfessorBetweenDate(professor, start, end)
    }
}