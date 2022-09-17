package fr.corpauration.utils

import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.ws.rs.core.Response

class UserNotRegistered : RuntimeException()

class UserNotAllowed : RuntimeException()

class NotReactiveFriendly : RuntimeException()

class CommonExceptionMapper {
    @ServerExceptionMapper
    fun mapException(x: UserNotRegistered): RestResponse<String>? {
        return RestResponse.status(Response.Status.PAYMENT_REQUIRED, "User is not registered")
    }

    @ServerExceptionMapper
    fun mapException(x: UserNotAllowed): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "User is not allowed")
    }

    @ServerExceptionMapper
    fun mapException(x: NotReactiveFriendly): RestResponse<String>? {
        return RestResponse.status(
            Response.Status.INTERNAL_SERVER_ERROR,
            "This server is missing some zest of reactive"
        )
    }
}