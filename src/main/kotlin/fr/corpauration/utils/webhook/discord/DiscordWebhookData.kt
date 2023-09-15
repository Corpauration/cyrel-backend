package fr.corpauration.utils.webhook.discord

import fr.corpauration.utils.webhook.WebhookData

data class DiscordWebhookData(val url: String, val message: String): WebhookData
