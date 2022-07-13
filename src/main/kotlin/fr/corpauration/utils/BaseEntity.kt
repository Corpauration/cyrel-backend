package fr.corpauration.utils

import io.vertx.mutiny.sqlclient.Row

class BaseEntity {
    val id: Int
    val data: Boolean

    constructor(row: Row) {
        id = row.getInteger("id")
        data = row.getBoolean("data")
    }

    fun from(row: Row): BaseEntity {
        return BaseEntity(row)
    }

    object StaticFunctions {
        fun from(row: Row): BaseEntity {
            return BaseEntity(row)
        }
    }
}