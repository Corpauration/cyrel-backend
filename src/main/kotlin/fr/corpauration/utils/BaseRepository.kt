package fr.corpauration.utils

import io.vertx.mutiny.pgclient.PgPool
import java.util.Objects
import java.util.UUID
import javax.inject.Inject

class BaseRepository<K, V>(var table: String) {
    @Inject
    var client: PgPool? = null

    fun findAll(): List<V> {
        return TODO()
    }

    fun findById(id: K): V {
        return TODO()
    }

    fun findBy(id: Any, field: String): V {
        return TODO()
    }

    fun save(obj: V): Nothing = TODO()
}