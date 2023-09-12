package fr.corpauration.utils

import io.smallrye.mutiny.Uni

object Mutiny {
    fun <T, U> sequentialCreateUniFromItems(items: List<T>, run: (r: T) -> Uni<U>): Uni<List<U>> {
        if (items.isEmpty()) return Uni.createFrom().item(listOf())
        val uni = Uni.createFrom().item(items.first()).flatMap(run)
        return if (items.size == 1) uni.map { listOf(it) }
        else uni.flatMap { item -> sequentialCreateUniFromItems(items.drop(1), run).map { listOf(item).plus(it) } }
    }
}
