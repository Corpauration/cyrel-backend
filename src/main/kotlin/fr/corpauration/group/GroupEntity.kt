package fr.corpauration.group

import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.util.UUID

@Entity
class GroupEntity {
    var id: Int
    var name: String
    var referent: UUID?
    @OneToOne(id = Int::class)
    var parent: GroupEntity?
    var private: Boolean

    constructor(id: Int = -1, name: String = "", referent: UUID? = null, parent: GroupEntity? = null, private: Boolean = false) {
        this.id = id
        this.name = name
        this.referent = referent
        this.parent = parent
        this.private = private
    }

    companion object { }
}

// FIXME Change that with annotation at build time
const val ADMIN = -1
const val HOMEWORK_RESP = -2
const val DELEGATE = -3