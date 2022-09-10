package fr.corpauration.utils

import fr.corpauration.user.UserRepository
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.operators.uni.UniOnItemTransformToMulti
import io.smallrye.mutiny.operators.uni.UniOnItemTransformToUni
import javax.annotation.Priority
import javax.inject.Inject
import javax.interceptor.AroundInvoke
import javax.interceptor.Interceptor
import javax.interceptor.InvocationContext

@Interceptor
@AccountExist
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
class AccountExistInterceptor {
    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    /*@ServerExceptionMapper
    fun mapException(x: UserNotAllowed): RestResponse<String>? {
        return RestResponse.status(Response.Status.FORBIDDEN, "User is not allowed")
    }

    @ServerExceptionMapper
    fun mapException(x: NotReactiveFriendly): RestResponse<String>? {
        return RestResponse.status(
            Response.Status.INTERNAL_SERVER_ERROR,
            "This server is missing some zest of reactive"
        )
    }*/

    @AroundInvoke
    fun intercept(context: InvocationContext): Any {
        val list = userRepository.findBy(identity.principal.name, "email").collect().asList()

        val proceeded = context.proceed()

        return when (proceeded::class.simpleName) {
            "UniOnItemTransformToUni" -> list.flatMap {
                if (it.size == 1) (proceeded as UniOnItemTransformToUni<*, *>)
                else throw UserNotRegistered()
            }

            "UniOnItemTransformToMulti" -> list.toMulti().flatMap {
                if (it.size == 1) (proceeded as UniOnItemTransformToMulti<*, *>)
                else throw UserNotRegistered()
            }

            else -> throw NotReactiveFriendly()
        }
    }
}