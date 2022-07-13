package fr.corpauration.utils

import io.vertx.mutiny.sqlclient.Row

interface BaseEntity {
    /*companion object {
        @JvmStatic abstract fun from(row: Row): BaseEntity?
    }*/

    fun from(row: Row): BaseEntity
}