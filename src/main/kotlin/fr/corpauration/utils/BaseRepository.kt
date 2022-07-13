package fr.corpauration.utils

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.RowSet
import io.vertx.mutiny.sqlclient.Tuple
import org.reactivestreams.Publisher
import javax.inject.Inject

class BaseRepository<K, V: BaseEntity>(var table: String) {
    @Inject
    var client: PgPool? = null

    fun findAll(): List<V> {
        val rowSet: Uni<RowSet<Row>> = client!!.preparedQuery("SELECT * FROM $1").execute(Tuple.of(table))
       /* val whatsTheType = rowSet.onItem().transformToMulti(java.util.function.Function<RowSet<Row>, Publisher<*>> {
            set: RowSet<Row> -> Multi.createFrom().iterable(set)
        }).onItem().transform(java.util.function.Function<Any, V> {
                row: Any -> V = {
            object {} as V
        }
        })*/

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