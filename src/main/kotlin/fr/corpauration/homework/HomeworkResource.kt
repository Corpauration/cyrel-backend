package fr.corpauration.homework

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.GroupRepository
import fr.corpauration.group.HOMEWORK_RESP
import fr.corpauration.user.UserRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.NeedToBeInGroups
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import java.time.LocalDate
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Path("/homework")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class HomeworkResource {
    @Inject
    @RepositoryGenerator(table = "homeworks", id = UUID::class, entity = HomeworkEntity::class)
    lateinit var homeworkRepository: HomeworkRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    @ServerExceptionMapper
    fun mapException(x: HomeworkMalformed): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "Homework is badly formatted")
    }

    @ServerExceptionMapper
    fun mapException(x: UnauthorizedGroupTarget): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "Unauthorized group target")
    }

    @GET
    @Path("/{id}")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: UUID): Uni<HomeworkEntity> {
        return homeworkRepository.findById(id).flatMap { it.loadLazy() }
    }

    @POST
    @AccountExist
    @NeedToBeInGroups(HOMEWORK_RESP)
    fun createHomework(json: JsonNode): Uni<Void> {
        if (json.hasNonNull("title") || !json.get("title").isTextual || json.hasNonNull(
                "type"
            ) || !json.get("type").isInt || json.hasNonNull("content") || !json.get("content").isTextual || json.hasNonNull(
                "date"
            ) || !json.get("date").isTextual || json.hasNonNull("group") || !json.get("group").isInt
        ) throw HomeworkMalformed()

        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { it[0] }
            .flatMap {
                if (it.groups.map { it.id }.contains(json.get("group").asInt()))
                    homeworkRepository.save(
                        HomeworkEntity(
                            title = json.get("title").asText(),
                            content = json.get("content").asText(),
                            type = json.get("type").asInt(),
                            date = LocalDate.parse(json.get("date").asText()),
                            group = it.groups.filter { it.id == json.get("group").asInt() }[0]
                        )
                    )
                else throw UnauthorizedGroupTarget()
            }
    }
}

@Path("/homeworks")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class HomeworksResource {
    @Inject
    lateinit var homeworkRepository: HomeworkRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @GET
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): Multi<HomeworkEntity> {
        return homeworkRepository.getAll()
    }
}