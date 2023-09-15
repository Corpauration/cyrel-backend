package fr.corpauration.conf

import fr.corpauration.utils.Entity

@Entity
class ConfEntity(
    var id: String = "",
    var value: String = ""
) {

    companion object
}
