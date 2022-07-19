package fr.corpauration.user

import fr.corpauration.utils.*
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import java.util.UUID
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class UserResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @RepositoryGenerator(table = "users", id = UUID::class, entity = UserEntity::class)
    val userRepository: Any by lazy {
        UserRepository(client)
    }

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