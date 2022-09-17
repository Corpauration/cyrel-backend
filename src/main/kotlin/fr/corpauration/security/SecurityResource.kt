package fr.corpauration.security

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Path("/security")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class SecurityResource {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    private val LOG: Logger = Logger.getLogger(SecurityResource::class.java)

    @ServerExceptionMapper
    fun mapException(x: WrongEmailDomain): RestResponse<String>? {
        return RestResponse.status(Response.Status.UNAUTHORIZED, "Wrong email domain")
    }

    @POST
    suspend fun getToken(credentials: Credentials): TokenResponse {
        if (!credentials.username.endsWith("@cy-tech.fr")) throw WrongEmailDomain()
        val response: HttpResponse = client.submitForm(
            url = "${System.getenv("KEYCLOAK_URL")}/protocol/openid-connect/token",
            formParameters = Parameters.build {
                append("client_id", "cyrel")
                append("grant_type", "password")
                append("client_secret", System.getenv("KEYCLOAK_SECRET"))
                append("scope", "openid")
                append("username", credentials.username)
                append("password", credentials.password)
            }
        )
        if (!response.status.isSuccess()) LOG.error(response.request.url)
        return response.body()
    }

    @PUT
    suspend fun refreshToken(refreshToken: String): TokenResponse {
        val response: HttpResponse = client.submitForm(
            url = "${System.getenv("KEYCLOAK_URL")}/protocol/openid-connect/token",
            formParameters = Parameters.build {
                append("client_id", "cyrel")
                append("grant_type", "refresh_token")
                append("client_secret", System.getenv("KEYCLOAK_SECRET"))
                append("refresh_token", refreshToken)
            }
        )
        if (!response.status.isSuccess()) LOG.error(response.request.url)
        return response.body()
    }
}