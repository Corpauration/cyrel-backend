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

class BaseRepository<K, BaseEntity>(var client: PgPool, var table: String) {

    fun battle(r: Any): BaseEntity {
        print(r)
        return object {} as BaseEntity
    }
    fun battle2(r: Any): BaseEntity {
        print(r)
        print("efzefzef")
        return object {} as BaseEntity
    }

    @Transactional
    fun findAll(): List<BaseEntity> {
        print("print")
        val rowSet: Uni<RowSet<Row>> = client.query("SELECT * FROM test").execute()
        /*print(rowSet.onItem().transform(RowSet<Row>::iterator)
            .onItem().tra)*/
        val whatsTheType = rowSet.onItem().transformToMulti(java.util.function.Function<RowSet<Row>, Publisher<*>> {
            set: RowSet<Row> -> Multi.createFrom().iterable(set)
        }).onItem().transform(java.util.function.Function<Any, BaseEntity> {
                row: Any -> print("michel"); object {} as BaseEntity
        })


        return emptyList()
    }





    fun oskour(): Multi<BaseEntity>? {
        val rowSet: Uni<RowSet<Row>> = client.query("SELECT * FROM test").execute()
        return rowSet.onItem().transformToMulti(java.util.function.Function<RowSet<Row>, Publisher<*>> {
                set: RowSet<Row> -> Multi.createFrom().iterable(set)
        }).onItem().transform(java.util.function.Function<Any, BaseEntity> {
                row: Any -> print("michel"); fr.corpauration.utils.BaseEntity.StaticFunctions.from(row as Row) as BaseEntity
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