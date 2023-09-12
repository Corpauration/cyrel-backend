package fr.corpauration.utils

import fr.corpauration.user.ProfessorRepository
import fr.corpauration.user.UserRepository
import fr.corpauration.user.UserType
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

        return when (val proceeded = context.proceed()) {
            is Uni<*> -> list.flatMap {
                if (it.size == 1 && it.first().type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.first().id)
                    .flatMap { if (it.authorized) proceeded else throw ProfessorNotAuthorized() }
                else if (it.size == 1) proceeded
                else throw UserNotRegistered()
            }

            is Multi<*> -> list.toMulti().flatMap {
                if (it.size == 1 && it.first().type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.first().id)
                    .toMulti()
                    .flatMap { if (it.authorized) proceeded else throw ProfessorNotAuthorized() }
                else if (it.size == 1) proceeded
                else throw UserNotRegistered()
            }

            else -> {
                Log.error("Not Reactive Friendly -> ${proceeded::class.simpleName} | ${context.method}")
                throw NotReactiveFriendly()
            }
        }
    }
}