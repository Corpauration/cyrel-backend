package fr.corpauration.group

import fr.corpauration.utils.BaseEntity
import fr.corpauration.utils.BaseRepository
import fr.corpauration.utils.BaseResource
import io.smallrye.mutiny.Multi
import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/group")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class GroupResource : BaseResource() {

    @Inject
    lateinit var client: PgPool
    @GET
    fun hello(): Multi<BaseEntity>? {
        val repo: BaseRepository<Int, BaseEntity> = BaseRepository(client, "test")
        return repo.oskour()
    }
}

@Path("/groups")
class GroupsResource : BaseResource() {
    @GET
    fun hello() = "Hello from /groups"
}


//  CustomRepository -> BaseRepository -> Quarkus