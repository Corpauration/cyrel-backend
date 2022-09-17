package fr.corpauration.homework

import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.ws.rs.core.Response

class HomeworkMalformed : RuntimeException()

class UnauthorizedGroupTarget : RuntimeException()

class HomeworkExceptionMapper {
    @ServerExceptionMapper
    fun mapException(x: HomeworkMalformed): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "Homework is badly formatted")
    }

    @ServerExceptionMapper
    fun mapException(x: UnauthorizedGroupTarget): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "Unauthorized group target")
    }
}
