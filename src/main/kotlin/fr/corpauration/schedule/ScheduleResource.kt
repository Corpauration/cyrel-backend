package fr.corpauration.schedule

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.homework.HomeworkEntity
import fr.corpauration.homework.HomeworkRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Uni
import java.time.LocalDate
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.BadRequestException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/schedule")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class ScheduleResource {
    @Inject
    @RepositoryGenerator(table = "courses", id = String::class, entity = CourseEntity::class)
    lateinit var courseRepository: CourseRepository

    @POST
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getFromTo(json: JsonNode): Uni<List<CourseEntity>> {
        if (!json.hasNonNull("group") || !json.get("group").isInt || !json.hasNonNull(
                "start"
            ) || !json.get("start").isTextual || !json.hasNonNull("end") || !json.get("end").isTextual
        ) throw BadRequestException()
        val start = LocalDate.parse(json.get("start").asText())
        val end = LocalDate.parse(json.get("end").asText())
        return courseRepository.findBy(json.get("group").asInt(), "group").collect().asList().onItem() // FIXME
            .transform {
                it.filter { it.start >= start && it.start <= end }
            }
    }
}