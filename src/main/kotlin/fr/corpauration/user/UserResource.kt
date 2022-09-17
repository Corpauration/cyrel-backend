package fr.corpauration.user

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.ADMIN
import fr.corpauration.group.GroupRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.BaseResource
import fr.corpauration.utils.NeedToBeInGroups
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.oidc.UserInfo
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.mutiny.pgclient.PgPool
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import java.time.LocalDate
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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
        table = "students",
        id = UUID::class,
        entity = StudentEntity::class
    )
    lateinit var studentRepository: StudentRepository

    @Inject
    @RepositoryGenerator(
        table = "professors",
        id = UUID::class,
        entity = ProfessorEntity::class
    )
    lateinit var professorRepository: ProfessorRepository

    @Inject
    @RepositoryGenerator(
        table = "cytech_students",
        id = Int::class,
        entity = CytechStudent::class
    )
    lateinit var cytechStudentRepository: CytechStudentRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var groupRepository: GroupRepository

    @ServerExceptionMapper
    fun mapException(x: UnknownPersonType): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "Unknown person type")
    }

    @ServerExceptionMapper
    fun mapException(x: AlreadyRegistered): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "User is already registered")
    }

    @ServerExceptionMapper
    fun mapException(x: UnknownStudentId): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "Student id not authorized")
    }

    @GET
    @AccountExist
    @NeedToBeInGroups(ADMIN)
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
    suspend fun register(json: JsonNode) {
        if (!json.hasNonNull("person_type") || !json.get("person_type").isInt || json.get("person_type")
                .asInt() != UserType.STUDENT.ordinal || !json.hasNonNull("student_id") || !json.get("student_id").isInt
        ) throw BadRequestException("Malformed request")
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
                    type = json.get("person_type").asInt()
                )
                when (json.get("person_type").asInt()) {
                    UserType.STUDENT.ordinal -> cytechStudentRepository.findById(json.get("student_id").asInt())
                        .onFailure().transform { UnknownStudentId() }

                    UserType.PROFESSOR.ordinal -> Uni.createFrom().voidItem()
                    else -> Uni.createFrom().failure<Void?>(UnknownPersonType()).replaceWithVoid()
                }.flatMap {
                    userRepository.save(user).flatMap {
                        when (json.get("person_type").asInt()) {
                            UserType.STUDENT.ordinal -> studentRepository.save(
                                StudentEntity(
                                    id = user.id,
                                    student_id = json.get("student_id").asInt()
                                )
                            )

                            UserType.PROFESSOR.ordinal -> professorRepository.save(ProfessorEntity(id = user.id))
                            else -> throw UnknownPersonType()
                        }
                    }
                }
            } else throw AlreadyRegistered()
        }.onItem().transform { "not null" }.awaitSuspending()
    }
}
