package fr.corpauration.group

import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.util.*

@Entity
class GroupEntity(
    var id: Int = -100,
    var name: String = "",
    var referent: UUID? = null,
    @OneToOne(id = Int::class) var parent: GroupEntity? = null,
    var private: Boolean = false
) {

    companion object
}

// FIXME Change that with annotation at build time
const val ADMIN = -1
const val HOMEWORK_RESP = -2
const val DELEGATE = -3