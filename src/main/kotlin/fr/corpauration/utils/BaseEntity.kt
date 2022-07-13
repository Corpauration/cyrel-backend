package fr.corpauration.utils

import io.vertx.mutiny.sqlclient.Row

class BaseEntity {
    val id: Int
    val data: Boolean
    constructor(row: Row) {
        id = row.getInteger("id")
        data = row.getBoolean("data")
    }


    /*companion object {
        @JvmStatic fun from(row: Row): BaseEntity? {
            return BaseEntity(row)
        }
    }*/

    fun from(row: Row): BaseEntity {
        return BaseEntity(row)
    }

    object StaticFunctions {
        fun execute() {
            println("Executing from inside a static object")
        }
        fun from(row: Row): BaseEntity {
        return BaseEntity(row)
    }

    }

}