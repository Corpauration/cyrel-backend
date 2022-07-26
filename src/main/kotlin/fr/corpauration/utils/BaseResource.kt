package fr.corpauration.utils

import io.smallrye.mutiny.Uni
import javax.ws.rs.GET
import javax.ws.rs.Path

open class BaseResource {
    @Path("/ping")
    @GET
    fun ping() = Uni.createFrom().item("PONG!")
}