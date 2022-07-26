package fr.corpauration.user

import fr.corpauration.utils.Entity
import java.util.*

@Entity
class StudentEntity {
    var id: UUID
    var student_id: Int

    constructor(
        id: UUID = UUID.randomUUID(),
        student_id: Int = 0
    ) {
        this.id = id
        this.student_id = student_id
    }


    companion object { }
}