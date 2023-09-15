package fr.corpauration.utils.webhook

interface Webhook {
    suspend fun send(data: WebhookData)
}
