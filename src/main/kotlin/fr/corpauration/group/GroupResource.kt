package fr.corpauration.group

import fr.corpauration.utils.BaseEntity
import fr.corpauration.utils.BaseRepository
import fr.corpauration.utils.BaseResource
import io.vertx.mutiny.pgclient.PgPool
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/group")
class GroupResource : BaseResource() {

    @Inject
    lateinit var client: PgPool
    @GET
    fun hello(): String {
        val repo: BaseRepository<Int, BaseEntity> = BaseRepository(client, "test")
        repo.findAll()
        return "Hello from /group"
    }
}

@Path("/groups")
class GroupsResource : BaseResource() {
    @GET
    fun hello() = "Hello from /groups"
}


//  CustomRepository -> BaseRepository -> Quarkus