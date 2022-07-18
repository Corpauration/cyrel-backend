package fr.corpauration.group

import fr.corpauration.utils.Entity
import java.util.UUID

@Entity
class GroupEntity {
    var id: Int
    var name: String
    var referent: UUID?
    var parent: Int?
    var private: Boolean

    constructor(id: Int = -1, name: String = "", referent: UUID? = null, parent: Int? = null, private: Boolean = false) {
        this.id = id
        this.name = name
        this.referent = referent
        this.parent = parent
        this.private = private
    }

    companion object { }
}