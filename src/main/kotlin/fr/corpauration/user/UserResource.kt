package fr.corpauration.user

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.GroupRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.BaseResource
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.oidc.UserInfo
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.mutiny.pgclient.PgPool
import java.time.LocalDate
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
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
    @RepositoryGenerator(table = "students", id = UUID::class, entity = StudentEntity::class)
    lateinit var studentRepository: StudentRepository

    @Inject
    @RepositoryGenerator(table = "professors", id = UUID::class, entity = ProfessorEntity::class)
    lateinit var professorRepository: ProfessorRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var groupRepository: GroupRepository

    @GET
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): Multi<UserEntity> {
        return userRepository.getAll()
    }

    @GET
    @AccountExist
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: UUID): Uni<UserEntity> {
        return userRepository.findById(id)
    }

    @GET
    @Path("/isRegistered")
    fun isRegistered(): Uni<Boolean> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem().transform {
            it.size == 1
        }
    }

    @POST
    suspend fun register(json: JsonNode): Any? {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().flatMap {
            if (it.size == 0) {
                val userInfo = (identity.attributes["userinfo"]!! as UserInfo)
                val user = UserEntity(
                    email = identity.principal.name,
                    firstname = userInfo.getString("given_name"),
                    lastname = userInfo.getString("family_name"),
                    birthday = if (!json.get("birthday").isNull()) LocalDate.parse(
                        json.get("birthday").asText()
                    ) else null,
                    type = userInfo.getLong("person_type").toInt()
                )
                userRepository.save(user).flatMap {
                    when (userInfo.getLong("person_type").toInt()) {
                        UserType.STUDENT.ordinal -> studentRepository.save(StudentEntity(id = user.id, student_id = userInfo.getLong("student_id").toInt()))
                        UserType.PROFESSOR.ordinal -> professorRepository.save(ProfessorEntity(id = user.id))
                        else -> throw Exception("Unknown person type")
                    }
                }
            } else throw Exception("User already registered")
        }.awaitSuspending()
    }
}
