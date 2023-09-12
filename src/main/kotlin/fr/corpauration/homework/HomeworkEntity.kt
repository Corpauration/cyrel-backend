package fr.corpauration.homework

import com.fasterxml.jackson.annotation.JsonIgnore
import fr.corpauration.group.GroupEntity
import fr.corpauration.user.UserEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
class HomeworkEntity(
    var id: UUID = UUID.randomUUID(),
    var title: String = "",
    var type: Int = -1,
    var content: String = "",
    var date: LocalDate = LocalDate.MIN,
    @OneToOne(id = Int::class) var group: GroupEntity = GroupEntity(),
    @JsonIgnore @OneToOne(id = UUID::class) var last_modified_by: UserEntity = UserEntity(),
    var last_modified_at: LocalDateTime = LocalDateTime.now(),
    var created_at: LocalDateTime = LocalDateTime.now()
) {

    companion object
}