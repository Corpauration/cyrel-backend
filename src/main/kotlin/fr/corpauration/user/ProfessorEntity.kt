package fr.corpauration.user

import fr.corpauration.utils.Entity
import java.util.*

@Entity
class ProfessorEntity(var id: UUID = UUID.randomUUID(), var authorized: Boolean = false) {


    companion object
}