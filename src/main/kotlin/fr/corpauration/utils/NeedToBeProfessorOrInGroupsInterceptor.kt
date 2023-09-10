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
@NeedToBeProfessorOrInGroups()
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
class NeedToBeProfessorOrInGroupsInterceptor {
    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var professorRepository: ProfessorRepository

    @AroundInvoke
    fun intercept(context: InvocationContext): Any {
        val user = userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { if (it.size == 0) throw UserNotRegistered() else it[0] }

        val groups = context.method.getAnnotation(NeedToBeProfessorOrInGroups::class.java).groups.asList()

        return when (val proceeded = context.proceed()) {
            is Uni<*> -> user.flatMap {
                if (it.type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.id)
                    .flatMap { if (it.authorized) proceeded else throw ProfessorNotAuthorized() }
                else if (it.isInGroups(*groups.toIntArray())) proceeded
                else throw UserNotAllowed()
            }

            is Multi<*> -> user.toMulti().flatMap {
                if (it.type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.id).toMulti()
                    .flatMap { if (it.authorized) proceeded else throw ProfessorNotAuthorized() }
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