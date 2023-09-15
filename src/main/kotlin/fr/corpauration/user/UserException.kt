package fr.corpauration.user

import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.ws.rs.core.Response

class UnknownPersonType : RuntimeException()

class AlreadyRegistered : RuntimeException()

class UnknownStudentId : RuntimeException()

class PreregistrationCorruptBiscuit : RuntimeException()

class PreregistrationBiscuitExpired : RuntimeException()

class UserExceptionMapper {
    @ServerExceptionMapper
    fun mapException(x: UnknownPersonType): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "Unknown person type")
    }

    @ServerExceptionMapper
    fun mapException(x: AlreadyRegistered): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "User is already registered")
    }

    @ServerExceptionMapper
    fun mapException(x: UnknownStudentId): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "Student id not authorized")
    }

    @ServerExceptionMapper
    fun mapException(x: PreregistrationCorruptBiscuit): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "Corrupt biscuit")
    }

    @ServerExceptionMapper
    fun mapException(x: PreregistrationBiscuitExpired): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "Biscuit expired")
    }
}