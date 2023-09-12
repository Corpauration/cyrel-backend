package fr.corpauration.utils

import fr.corpauration.user.UserRepository
import io.quarkus.logging.Log
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
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

    @AroundInvoke
    fun intercept(context: InvocationContext): Any {
        val user = userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { if (it.size == 0) throw UserNotRegistered() else it[0] }

        val groups = context.method.getAnnotation(NeedToBeInGroups::class.java).groups.asList()

        return when (val proceeded = context.proceed()) {
            is Uni<*> -> user.flatMap {
                if (it.isInGroups(*groups.toIntArray())) proceeded
                else throw UserNotAllowed()
            }

            is Multi<*> -> user.toMulti().flatMap {
                if (it.isInGroups(*groups.toIntArray())) proceeded
                else throw UserNotAllowed()
            }

            else -> {
                Log.error("Not Reactive Friendly -> ${proceeded::class.simpleName} | ${context.method}")
                throw NotReactiveFriendly()
            }
        }
    }
}