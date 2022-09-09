package fr.corpauration.schedule

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.GroupRepository
import fr.corpauration.homework.UnauthorizedGroupTarget
import fr.corpauration.user.UserRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.CustomSql
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.BadRequestException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/schedule")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class ScheduleResource {
    @Inject
    @RepositoryGenerator(table = "courses", id = String::class, entity = CourseEntity::class)
    lateinit var courseRepository: CourseRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    @ServerExceptionMapper
    fun mapException(x: UnauthorizedGroupTarget): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "Unauthorized group target")
    }

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

        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { it[0] }
            .flatMap {
                if (it.groups.map { it.id }.contains(json.get("group").asInt()))
                    wrapperRetrieveScheduleForGroupBetweenDate(json.get("group").asInt(), start, end).collect().asList()
                else throw UnauthorizedGroupTarget()
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
}