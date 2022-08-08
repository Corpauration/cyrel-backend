package fr.corpauration.group

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.RowSet
import org.reactivestreams.Publisher
import java.util.function.Function

class CustomGroupRepository: GroupRepository() {
    fun findChildrenOf(id: Int): Multi<GroupEntity> {
        val rowSet: Uni<RowSet<Row>> = client.query("SELECT * FROM groups").execute()
        return rowSet.onItem().transformToMulti(Function<RowSet<Row>, Publisher<*>> { set: RowSet<Row> ->
            Multi.createFrom().iterable(set)
        }).flatMap { GroupEntity.Companion.from(it as Row, client)!!.toMulti() }
    }
}