package fr.corpauration.alerts.schedule

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.GroupRepository
import fr.corpauration.homework.UnauthorizedGroupTarget
import fr.corpauration.user.UserRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.BadRequestException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/alert/schedule")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class CourseAlertResource {
    @Inject
    @RepositoryGenerator(
        table = "courses_alerts", id = String::class, entity = CourseAlertEntity::class
    )
    lateinit var courseAlertRepository: CourseAlertRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @POST
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun get(json: JsonNode): Uni<List<CourseAlertEntity>> {
        if (!json.hasNonNull("group") || !json.get("group").isInt) throw BadRequestException()
        val time = if (json.hasNonNull("time") && json.get("time").isTextual) LocalDateTime.parse(
            json.get("time").asText()
        ) else null
        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem().transform { it[0] }
            .flatMap {
                if (it.tags["group"].equals(json.get("group").asInt().toString())) courseAlertRepository.findBy(
                    json.get("group").asInt(),
                    "group"
                ).collect().asList().onItem().transform {
                    if (time != null) it.filter { it.time >= time }
                    else it
                }
                else throw UnauthorizedGroupTarget()
            }
    }
}