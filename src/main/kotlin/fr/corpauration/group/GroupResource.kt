package fr.corpauration.group

import fr.corpauration.utils.BaseRepository
import fr.corpauration.utils.BaseResource
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/group")
class GroupResource : BaseResource() {
    @GET
    fun hello() = "Hello from /group"
}

@Path("/groups")
class GroupsResource : BaseResource() {
    @GET
    fun hello() = "Hello from /groups"
}


//  CustomRepository -> BaseRepository -> Quarkus