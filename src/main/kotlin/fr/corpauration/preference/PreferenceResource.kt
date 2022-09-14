package fr.corpauration.preference

import fr.corpauration.group.GroupRepository
import fr.corpauration.theme.ThemeEntity
import fr.corpauration.user.UserRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/preference")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class PreferenceResource {
    @Inject
    @RepositoryGenerator(
        table = "preferences",
        id = UUID::class,
        entity = PreferenceEntity::class,
        additionalDataSource = ""
    )
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    @GET
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun get(): Uni<PreferenceEntity> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { it[0] }
            .flatMap {
                preferenceRepository.findById(it.id).onFailure().recoverWithUni { _ ->
                    Uni.createFrom().item(PreferenceEntity(it.id, ThemeEntity(id = 0)))
                }
            }
    }

    @POST
    @AccountExist
    fun save(pref: PreferenceEntity): Uni<Void> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { it[0] }
            .flatMap {
                pref.id = it.id
                preferenceRepository.save(pref).onFailure().recoverWithUni { _ ->
                    preferenceRepository.update(pref)
                }
            }
    }
}