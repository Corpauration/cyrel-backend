package fr.corpauration.utils

import fr.corpauration.user.ProfessorRepository
import fr.corpauration.user.UserRepository
import fr.corpauration.user.UserType
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

    @Inject
    lateinit var professorRepository: ProfessorRepository

    @AroundInvoke
    fun intercept(context: InvocationContext): Any {
        val list = userRepository.findBy(identity.principal.name, "email").collect().asList()

        val proceeded = context.proceed()

        return when (proceeded::class.simpleName) {
            "UniOnItemTransformToUni" -> list.flatMap {
                if (it.size == 1 && it.first().type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.first().id)
                    .flatMap { if (it.authorized) (proceeded as UniOnItemTransformToUni<*, *>) else throw ProfessorNotAuthorized() }
                else if (it.size == 1) (proceeded as UniOnItemTransformToUni<*, *>)
                else throw UserNotRegistered()
            }

            "UniOnItemTransformToMulti" -> list.toMulti().flatMap {
                if (it.size == 1 && it.first().type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.first().id)
                    .toMulti()
                    .flatMap { if (it.authorized) (proceeded as UniOnItemTransformToMulti<*, *>) else throw ProfessorNotAuthorized() }
                else if (it.size == 1) (proceeded as UniOnItemTransformToMulti<*, *>)
                else throw UserNotRegistered()
            }

            else -> throw NotReactiveFriendly()
        }
    }
}