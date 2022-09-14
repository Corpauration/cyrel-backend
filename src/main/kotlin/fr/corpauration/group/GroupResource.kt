package fr.corpauration.group

import fr.corpauration.user.UserRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.BaseResource
import fr.corpauration.utils.RepositoryGenerator
import fr.corpauration.utils.UserNotRegistered
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/group")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class GroupResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @Inject
    @RepositoryGenerator(
        table = "groups",
        id = Int::class,
        entity = GroupEntity::class,
        additionalDataSource = "quartz"
    )
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @ServerExceptionMapper
    fun mapException(x: UserNotRegistered): RestResponse<String>? {
        return RestResponse.status(Response.Status.PAYMENT_REQUIRED, "User is not registered")
    }

    @GET
    fun hello() = "Hello from Cyrel Api"

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: Int): Uni<GroupEntity> {
        return groupRepository.findById(id)
    }

    @GET
    @Path("/{id}/children")
    @Produces(MediaType.APPLICATION_JSON)
    fun getChildren(@PathParam("id") id: Int): Multi<GroupEntity> {
        return groupRepository.findBy(id, "parent")
    }

    @GET
    @Path("/{id}/join")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun join(@PathParam("id") id: Int): Uni<Boolean> {
        return groupRepository.findById(id).flatMap { group ->
            if (!group.private) userRepository.findBy(identity.principal.name, "email").collect().asList().flatMap {
                if (it.size == 1) {
                    if (it[0].groups.none { it.id == group.id }) {
                        it[0].groups = it[0].groups.plus(group)
                        userRepository.update(it[0]).flatMap { Uni.createFrom().item(true) }
                    } else Uni.createFrom().item(false)
                } else throw UserNotRegistered()
            } else Uni.createFrom().item(false)
        }
    }
}

@Path("/groups")
@Authenticated
class GroupsResource : BaseResource() {

    @Inject
    lateinit var client: PgPool

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(): Multi<GroupEntity>? {
        return groupRepository.findBy(false, "private")
    }

    @GET
    @Path("/ids")
    @Produces(MediaType.APPLICATION_JSON)
    fun getids(): Multi<Int>? {
        return groupRepository.getIds()
    }

    @GET
    @Path("/parents")
    @Produces(MediaType.APPLICATION_JSON)
    fun getParents(): Multi<GroupEntity> {
        return groupRepository.findBy(null, "parent").skip().where { it.private }
    }

    @GET
    @Path("/my")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getMyGroups(): Uni<List<GroupEntity>> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem().transform { it[0].groups }
    }
}