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
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


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

    @DELETE
    suspend fun logout(refreshToken: String) {
        val response: HttpResponse = client.submitForm(
            url = "${System.getenv("KEYCLOAK_URL")}/protocol/openid-connect/logout",
            formParameters = Parameters.build {
                append("client_id", "cyrel")
                append("client_secret", System.getenv("KEYCLOAK_SECRET"))
                append("refresh_token", refreshToken)
            }
        )
        if (!response.status.isSuccess()) LOG.error(response.request.url)
    }
}