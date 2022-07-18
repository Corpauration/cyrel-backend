package fr.corpauration.user

import fr.corpauration.group.GroupEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.ManyToMany
import java.time.LocalDate
import java.util.Date
import java.util.UUID

@Entity
class UserEntity {
    var id: UUID
    var email: String
    var firstname: String
    var lastname: String
    var type: Int
    var birthday: LocalDate?
    @ManyToMany(junction_table = "users_groups")
    var groups: List<GroupEntity>

    constructor(
        id: UUID = UUID.randomUUID(),
        email: String = "",
        firstname: String = "",
        lastname: String = "",
        type: Int = -1,
        birthday: LocalDate? = null,
        groups: List<GroupEntity> = emptyList()
    ) {
        this.id = id
        this.email = email
        this.firstname = firstname
        this.lastname = lastname
        this.type = type
        this.birthday = birthday
        this.groups = groups
    }


    companion object { }
}