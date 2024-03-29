package fr.corpauration.group

import fr.corpauration.user.UserRepository
import fr.corpauration.utils.*
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

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
        entity = GroupEntity::class
    )
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var identity: SecurityIdentity

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
                if (it.size == 1 && group.tags.contains("type")) {
                    val join = { type: String, value: Int ->
                        if (it[0].tags.contains(type))
                            Uni.createFrom().item(false)
                        else {
                            it[0].tags += mapOf(type to value.toString())
                            userRepository.update(it[0]).flatMap { Uni.createFrom().item(true) }
                        }
                    }

                    when (val t = group.tags["type"]) {
                        "promo" -> join(t, group.id)
                        "group" -> join(t, group.id)
                        "english" -> join(t, group.id)
                        else -> Uni.createFrom().item(false)
                    }
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
        return groupRepository.findBy(null, "parent").skip().where { it.private }.flatMap { group ->
            groupRepository.findBy(group.id, "parent").skip()
                .where { it.referent != null || it.tags["type"] == "english" }.collect().asList().map {
                    Pair(
                        group, it.isNotEmpty()
                    )
                }.toMulti()
        }.skip().where { it.second }.map { it.first }
    }

    @GET
    @Path("/my")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getMyGroups(): Uni<List<GroupEntity>> {
        return userRepository.findBy(identity.principal.name, "email").collect().asList().map {
            val list = mutableListOf<Int>()
            val g = { k: String -> if (it[0].tags.contains(k)) list.add(it[0].tags[k]!!.toInt()) }
            val sg = { k: SpecialGroup -> if (it[0].tags[k.name.lowercase()] == "true") list.add(k.id) }
            g("promo")
            g("group")
            g("english")
            sg(SpecialGroup.ADMIN)
            sg(SpecialGroup.HOMEWORK_RESP)
            sg(SpecialGroup.DELEGATE)

            list
        }.flatMap {
            Mutiny.sequentialCreateUniFromItems(it) { id ->
                groupRepository.findById(id)
            }
        }
    }
}