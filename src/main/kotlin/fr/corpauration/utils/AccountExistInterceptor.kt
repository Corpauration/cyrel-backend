package fr.corpauration.utils

import fr.corpauration.user.UserRepository
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.coroutines.awaitSuspending
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

    @AroundInvoke
    fun intercept(context: InvocationContext): Any {
        val list = userRepository.findBy(identity.principal.name, "email").collect().asList()

        val proceeded = context.proceed()

        return when (proceeded::class.simpleName) {
            "UniOnItemTransformToUni" -> list.flatMap {
                if (it.size == 1) (proceeded as UniOnItemTransformToUni<*, *>)
                else throw Exception("User is not registered")
            }
            "UniOnItemTransformToMulti" -> list.toMulti().flatMap {
                if (it.size == 1) (proceeded as UniOnItemTransformToMulti<*, *>)
                else throw Exception("User is not registered")
            }
            else -> throw Exception("Return type is not reactive friendly")
        }
    }
}