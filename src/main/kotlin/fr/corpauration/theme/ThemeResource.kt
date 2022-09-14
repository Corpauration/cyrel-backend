package fr.corpauration.theme

import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/theme")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class ThemeResource {
    @Inject
    @RepositoryGenerator(table = "themes", id = Int::class, entity = ThemeEntity::class, additionalDataSource = "")
    lateinit var themeRepository: ThemeRepository

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: Int): Uni<ThemeEntity> {
        return themeRepository.findById(id)
    }
}

@Path("/themes")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class ThemesResource {
    @Inject
    lateinit var themeRepository: ThemeRepository

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): Multi<ThemeEntity> {
        return themeRepository.getAll()
    }
}