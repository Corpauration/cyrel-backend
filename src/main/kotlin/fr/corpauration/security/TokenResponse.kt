package fr.corpauration.security

import com.fasterxml.jackson.annotation.JsonAlias

data class TokenResponse(@JsonAlias("access_token") val accessToken: String, @JsonAlias("refresh_token") val refreshToken: String, @JsonAlias("id_token") val idToken: String, @JsonAlias("expires_in") val expiresIn: Int, @JsonAlias("refresh_expires_in") val refreshExpiresIn: Int, @JsonAlias("token_type") val tokenType: String, @JsonAlias("not-before-policy") val notBeforePolicy: String, @JsonAlias("session_state") val sessionState: String, val scope: String)
