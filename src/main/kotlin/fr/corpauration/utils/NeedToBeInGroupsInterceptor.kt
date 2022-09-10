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
@NeedToBeInGroups()
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
class NeedToBeInGroupsInterceptor {
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
        val user = userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { if (it.size == 0) throw UserNotRegistered() else it[0] }

        val groups = context.method.getAnnotation(NeedToBeInGroups::class.java).groups.asList()

        val proceeded = context.proceed()


        return when (proceeded::class.simpleName) {
            "UniOnItemTransformToUni" -> user.flatMap {
                if (it.groups.map { it.id }.containsAll(groups)) (proceeded as UniOnItemTransformToUni<*, *>)
                else throw UserNotAllowed()
            }

            "UniOnItemTransformToMulti" -> user.toMulti().flatMap {
                if (it.groups.map { it.id }.containsAll(groups)) (proceeded as UniOnItemTransformToMulti<*, *>)
                else throw UserNotAllowed()
            }

            else -> throw NotReactiveFriendly()
        }
    }
}