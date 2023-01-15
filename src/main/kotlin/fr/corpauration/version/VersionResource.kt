package fr.corpauration.version

import fr.corpauration.utils.RepositoryGenerator
import io.smallrye.mutiny.Uni
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class VersionResource {
    @ConfigProperty(name = "quarkus.application.version")
    lateinit var version: String

    @Inject
    @RepositoryGenerator(
        table = "version",
        id = Boolean::class,
        entity = VersionEntity::class
    )
    lateinit var versionRepository: VersionRepository

    @ServerExceptionMapper
    fun mapException(x: UnknownCanal): RestResponse<String>? {
        return RestResponse.status(Response.Status.BAD_REQUEST, "Unknown version canal")
    }

    @GET
    fun getVersion(): Uni<String> {
        return Uni.createFrom().item(version)
    }

    @POST
    fun isClientInRightVersion(clientVersion: String): Uni<Boolean> {
        val v = CyrelVersion.fromString(version)
        val b = CyrelVersion.fromString(clientVersion)
        return Uni.createFrom().item(b.isAtLeast(v.major, v.minor))
    }

    @GET
    @Path("/client/{platform}")
    fun getClientLastVersion(platform: String): Uni<VersionEntity> {
        return versionRepository.findBy(platform, "platform").collect().first()
    }
}