package fr.corpauration.utils

import io.smallrye.mutiny.Uni
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

open class BaseResource {
    @Path("/ping")
    @GET
    fun ping() = Uni.createFrom().item("PONG!")
}