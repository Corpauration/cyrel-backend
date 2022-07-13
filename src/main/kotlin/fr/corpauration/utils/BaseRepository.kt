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

class BaseRepository<K, V: BaseEntity>(var client: PgPool, var table: String) {

    fun battle(r: Any): V {
        print(r)
        return object {} as V
    }
    fun battle2(r: Any): V {
        print(r)
        print("efzefzef")
        return object {} as V
    }

    @Transactional
    fun findAll(): List<V> {
        print("print")
        val rowSet: Uni<RowSet<Row>> = client.preparedQuery("SELECT * FROM $1").execute(Tuple.of(table))
        /*print(rowSet.onItem().transform(RowSet<Row>::iterator)
            .onItem().tra)*/
        val whatsTheType = rowSet.onItem().transformToMulti(java.util.function.Function<RowSet<Row>, Publisher<*>> {
            set: RowSet<Row> -> Multi.createFrom().iterable(set)
        }).onItem().transform(java.util.function.Function<Any, V> {
                row: Any -> battle(row)
        })


        return emptyList()
    }

    /*fun oskour(): Multi<Any> {
        val rowSet: Uni<RowSet<Row>> = client.preparedQuery("SELECT * FROM $1").execute(Tuple.of(table))
        return rowSet.onItem().transformToMulti(set -> Multi.createFrom().iterable(set)).onItem().transform(Fruit::from);
    }*/

    fun findById(id: K): V {
        return TODO()
    }

    fun findBy(id: Any, field: String): V {
        return TODO()
    }

    fun save(obj: V): Nothing = TODO()
}