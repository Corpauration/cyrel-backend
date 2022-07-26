package fr.corpauration.user

import fr.corpauration.utils.Entity
import java.util.*

@Entity
class ProfessorEntity {
    var id: UUID

    constructor(
        id: UUID = UUID.randomUUID()
    ) {
        this.id = id
    }


    companion object { }
}