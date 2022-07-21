package fr.corpauration.user

import fr.corpauration.group.GroupRepository
import fr.corpauration.utils.BaseResource
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
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
    lateinit var groupRepository: GroupRepository

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): Multi<UserEntity> {
        return (userRepository as UserRepository).getAll()
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: UUID): Uni<UserEntity> {
        return (userRepository as UserRepository).findById(id)
    }
}