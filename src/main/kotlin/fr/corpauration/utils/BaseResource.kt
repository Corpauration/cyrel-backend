package fr.corpauration.utils

import javax.ws.rs.GET
import javax.ws.rs.Path

open class BaseResource {
    @Path("/ping")
    @GET
    fun ping() = "PONG!"
}