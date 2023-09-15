package fr.corpauration.utils.webhook.discord

import fr.corpauration.utils.webhook.Webhook
import fr.corpauration.utils.webhook.WebhookData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*

class DiscordWebhook: Webhook {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    override suspend fun send(data: WebhookData) {
        when (data) {
            is DiscordWebhookData -> {
                httpClient.post(data.url) {
                    contentType(ContentType.Application.Json)
                    setBody(DiscordWebhookJson(data.message))
                }
            }
        }
    }
}