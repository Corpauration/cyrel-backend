package fr.corpauration.homework

import fr.corpauration.group.GroupEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.time.LocalDate
import java.util.*

@Entity
class HomeworkEntity(
    var id: UUID = UUID.randomUUID(),
    var title: String = "",
    var type: Int = -1,
    var content: String = "",
    var date: LocalDate = LocalDate.MIN,
    @OneToOne(id = Int::class) var group: GroupEntity = GroupEntity()
) {

    companion object
}