package fr.corpauration.group

import fr.corpauration.utils.BaseEntity
import fr.corpauration.utils.BaseRepository
import fr.corpauration.utils.BaseResource
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/group")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class GroupResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @GET
    fun hello() = "Hello from Cyrel Api"

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: String): Uni<GroupsResource> {
        val repo: BaseRepository<Int, BaseEntity> = BaseRepository(client, "test")
        return repo.findById(id.toInt()) as Uni<GroupsResource>
    }
}

@Path("/groups")
class GroupsResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(): Multi<BaseEntity>? {
        val repo: BaseRepository<Int, BaseEntity> = BaseRepository(client, "test")
        return repo.getAll()
    }

    @GET
    @Path("/ids")
    @Produces(MediaType.APPLICATION_JSON)
    fun getids(): Multi<Int>? {
        val repo: BaseRepository<Int, BaseEntity> = BaseRepository(client, "test")
        return repo.getIds()
    }
}