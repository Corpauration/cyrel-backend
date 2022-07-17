package fr.corpauration.utils

import fr.corpauration.group.GroupsResource
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/test")
class TestRessource {
    @Inject
    lateinit var client: PgPool

    @RepositoryGenerator(table = "test", id = Int::class, entity = TestEntity::class)
    val testRepository: Any by lazy {
        TestRepository(client)
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationScoped
    fun all(): Multi<TestEntity>? {
        return (testRepository as TestRepository).getAll()
    }

    @Path("/ids")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationScoped
    fun ids(): Multi<Int>? {
        return (testRepository as TestRepository).getIds()
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: String): Uni<TestEntity> {
        return (testRepository as TestRepository).findById(id.toInt())
    }

    @GET
    @Path("/false")
    @Produces(MediaType.APPLICATION_JSON)
    fun getByDataFalse(): Multi<TestEntity> {
        return (testRepository as TestRepository).findBy(false, "data")
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    suspend fun add(entity: TestEntity): Int {
        print("${entity.id} -> ${entity.data}")
        (testRepository as TestRepository).save(entity).awaitSuspending()
        return entity.id
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    suspend fun update(entity: TestEntity): Int {
        print("${entity.id} -> ${entity.data}")
        (testRepository as TestRepository).update(entity).awaitSuspending()
        return entity.id
    }
}