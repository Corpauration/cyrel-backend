package fr.corpauration.utils

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.RowIterator
import io.vertx.mutiny.sqlclient.RowSet
import io.vertx.mutiny.sqlclient.Tuple
import org.reactivestreams.Publisher
import java.util.function.Function
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

    @Transactional
    fun getIds(): Multi<Int> {
        val rowSet: Uni<RowSet<Row>> = client.query("SELECT id FROM test").execute()
        return rowSet.onItem().transformToMulti(java.util.function.Function<RowSet<Row>, Publisher<*>> { set: RowSet<Row> ->
            Multi.createFrom().iterable(set)
        }).onItem().transform(java.util.function.Function<Any, Int> { row: Any ->
            (row as Row).getInteger("id") as Int
        })
    }

    fun findById(id: Int): Uni<V> {
        return client.preparedQuery("SELECT * FROM test WHERE id = $1").execute(Tuple.of(id)).onItem().transform(RowSet<Row>::iterator).onItem().transform<Any?>(Function<RowIterator<Row?>, Any?> { iterator: RowIterator<Row?> -> if (iterator.hasNext()) BaseEntity.StaticFunctions.from(iterator.next() as Row) else null }) as Uni<V>
    }

    fun findBy(id: Any, field: String): BaseEntity {
        return TODO()
    }

    fun save(obj: BaseEntity): Nothing = TODO()
}