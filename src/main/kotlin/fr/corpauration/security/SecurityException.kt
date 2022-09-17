package fr.corpauration.security

import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.ws.rs.core.Response

class WrongEmailDomain : RuntimeException()

class SecurityExceptionMapper {
    @ServerExceptionMapper
    fun mapException(x: WrongEmailDomain): RestResponse<String>? {
        return RestResponse.status(Response.Status.UNAUTHORIZED, "Wrong email domain")
    }
}
