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

        val proceeded = context.proceed()


        return when (proceeded::class.simpleName) {
            "UniOnItemTransformToUni" -> user.flatMap {
                if (it.type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.id)
                    .flatMap { if (it.authorized) (proceeded as UniOnItemTransformToUni<*, *>) else throw ProfessorNotAuthorized() }
                else if (it.groups.map { it.id }.containsAll(groups)) (proceeded as UniOnItemTransformToUni<*, *>)
                else throw UserNotAllowed()
            }

            "UniOnItemTransformToMulti" -> user.toMulti().flatMap {
                if (it.type == UserType.PROFESSOR.ordinal) professorRepository.findById(it.id).toMulti()
                    .flatMap { if (it.authorized) (proceeded as UniOnItemTransformToMulti<*, *>) else throw ProfessorNotAuthorized() }
                if (it.groups.map { it.id }.containsAll(groups)) (proceeded as UniOnItemTransformToMulti<*, *>)
                else throw UserNotAllowed()
            }

            else -> throw NotReactiveFriendly()
        }
    }
}