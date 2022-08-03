package fr.corpauration.homework

import fr.corpauration.group.GroupRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Uni
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/homework")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class HomeworkResource {
    @Inject
    @RepositoryGenerator(table = "homeworks", id = UUID::class, entity = HomeworkEntity::class)
    lateinit var homeworkRepository: HomeworkRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @GET
    @Path("/{id}")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: UUID): Uni<HomeworkEntity> {
        return homeworkRepository.findById(id).flatMap { it.loadLazy() }
    }
}