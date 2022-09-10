package fr.corpauration.user

import fr.corpauration.group.GroupEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.ManyToMany
import java.time.LocalDate
import java.util.*

@Entity
class UserEntity(
    var id: UUID = UUID.randomUUID(),
    var email: String = "",
    var firstname: String = "",
    var lastname: String = "",
    var type: Int = -1,
    var birthday: LocalDate? = null,
    @ManyToMany(junction_table = "users_groups") var groups: List<GroupEntity> = emptyList()
) {


    companion object
}