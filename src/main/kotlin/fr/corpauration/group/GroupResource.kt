package fr.corpauration.group

import fr.corpauration.utils.*
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/group")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class GroupResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @RepositoryGenerator(table = "groups", id = Int::class, entity = GroupEntity::class)
    val groupRepository: Any by lazy {
        GroupRepository(client)
    }

    @GET
    fun hello() = "Hello from Cyrel Api"

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: Int): Uni<GroupEntity> {
        return (groupRepository as GroupRepository).findById(id)
    }
}

@Path("/groups")
@Authenticated
class GroupsResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @Inject
    lateinit var groupRepository: GroupRepository

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(): Multi<GroupEntity>? {
        return groupRepository.getAll()
    }

    @GET
    @Path("/ids")
    @Produces(MediaType.APPLICATION_JSON)
    fun getids(): Multi<Int>? {
        return groupRepository.getIds()
    }
}