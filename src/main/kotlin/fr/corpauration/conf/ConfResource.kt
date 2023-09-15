package fr.corpauration.conf

import biscuit.format.schema.Schema
import com.clevercloud.biscuit.crypto.PublicKey
import fr.corpauration.utils.RepositoryGenerator
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class ConfResource {

    @Inject
    lateinit var client: PgPool

    @Inject
    @RepositoryGenerator(
        table = "conf",
        id = String::class,
        entity = ConfEntity::class
    )
    lateinit var confRepository: ConfRepository

    val webhookUrl: Uni<String> by lazy {
        confRepository.findById("webhook_url").map { it.value }
    }

    val referentRegisteringPublicKey: Uni<PublicKey> by lazy {
        confRepository.findById("referent_registering_public").map {
            PublicKey(
                Schema.PublicKey.Algorithm.Ed25519,
                it.value
            )
        }
    }
}
