package fr.corpauration.security

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
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

    @POST
    suspend fun getToken(credentials: Credentials): TokenResponse {
        val response: HttpResponse = client.submitForm(
            url = "http://localhost:6969/realms/test-realm/protocol/openid-connect/token",
            formParameters = Parameters.build {
                append("client_id", "cyrel")
                append("grant_type", "password")
                append("client_secret", System.getenv("CLIENT_SECRET"))
                append("scope", "openid")
                append("username", credentials.username)
                append("password", credentials.password)
            }
        )
        return response.body()
    }
}