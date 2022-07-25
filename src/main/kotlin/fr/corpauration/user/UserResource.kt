package fr.corpauration.user

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.GroupRepository
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
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var groupRepository: GroupRepository

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): Multi<UserEntity> {
        return userRepository.getAll()
    }

    @GET
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
                    birthday = if (json.get("birthday") != null) LocalDate.parse(
                        json.get("birthday").asText()
                    ) else null,
                    type = userInfo.getLong("person_type").toInt()
                )
                userRepository.save(user)
            } else throw Exception("User already registered")
        }.awaitSuspending()
//        return (identity.attributes["userinfo"]!! as UserInfo).getLong("student_id")
//        return (identity.attributes["userinfo"]!! as UserInfo)
    }
}