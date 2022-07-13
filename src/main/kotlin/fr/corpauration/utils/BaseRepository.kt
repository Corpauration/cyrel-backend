package fr.corpauration.utils

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.RowSet
import org.reactivestreams.Publisher
import javax.transaction.Transactional

class BaseRepository<K, V : BaseEntity>(var client: PgPool, var table: String) {
    @Transactional
    fun getAll(): Multi<V>? {
        val rowSet: Uni<RowSet<Row>> = client.query("SELECT * FROM test").execute()
        return rowSet.onItem().transformToMulti(java.util.function.Function<RowSet<Row>, Publisher<*>> { set: RowSet<Row> ->
            Multi.createFrom().iterable(set)
        }).onItem().transform(java.util.function.Function<Any, V> { row: Any ->
            BaseEntity.StaticFunctions.from(row as Row) as V
        })
    }

    fun findById(id: K): BaseEntity {
        return TODO()
    }

    fun findBy(id: Any, field: String): BaseEntity {
        return TODO()
    }

    fun save(obj: BaseEntity): Nothing = TODO()
}