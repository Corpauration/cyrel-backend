package fr.corpauration.schedule.ical

import fr.corpauration.user.UserEntity
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.ws.rs.core.Response

class ScheduleICalNeedRecoverException(val user: UserEntity, val token: ICalTokenEntity) : RuntimeException()

class ScheduleICalTokenNotFoundException() : RuntimeException()

class ScheduleICalCorruptedTokenException() : RuntimeException()

class ScheduleICalExceptionMapper {
    @ServerExceptionMapper
    fun mapException(x: ScheduleICalTokenNotFoundException): RestResponse<String>? {
        return RestResponse.status(Response.Status.NOT_FOUND, "Token invalid")
    }

    @ServerExceptionMapper
    fun mapException(x: ScheduleICalCorruptedTokenException): RestResponse<String>? {
        return RestResponse.status(Response.Status.UNAUTHORIZED, "Token invalid")
    }
}
