package fr.corpauration.utils

import io.smallrye.mutiny.Multi
import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/test")
class TestRessource {
    @Inject
    lateinit var client: PgPool

    @RepositoryGenerator(table = "test", id = String::class, entity = BaseEntity::class)
    val testRepository: Any by lazy {
        TestRepository(client)
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationScoped
    fun all(): Multi<BaseEntity>? {
        return (testRepository as TestRepository).getAll()
    }
}