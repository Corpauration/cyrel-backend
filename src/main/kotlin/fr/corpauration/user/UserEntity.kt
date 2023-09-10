package fr.corpauration.user

import fr.corpauration.group.SpecialGroup.*
import fr.corpauration.utils.ElementsCollection
import fr.corpauration.utils.Entity
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
    @ElementsCollection(
        junction_table = "users_tags",
        key_type = String::class,
        value_type = String::class
    ) var tags: Map<String, String> = emptyMap()
) {

    fun isInGroup(group: Int): Boolean =
        when (group) {
            ADMIN.id -> tags[ADMIN.name.lowercase()] == "true"
            HOMEWORK_RESP.id -> tags[HOMEWORK_RESP.name.lowercase()] == "true"
            DELEGATE.id -> tags[DELEGATE.name.lowercase()] == "true"
            else -> listOf("promo", "group", "english").any { tags[it] == group.toString() }
        }

    fun isInOneOfGroups(vararg groups: Int): Boolean = groups.any { isInGroup(it) }

    fun isInGroups(vararg groups: Int): Boolean = groups.all { isInGroup(it) }

    companion object
}