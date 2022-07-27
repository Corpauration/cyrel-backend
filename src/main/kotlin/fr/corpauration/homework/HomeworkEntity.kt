package fr.corpauration.homework

import com.fasterxml.jackson.annotation.JsonInclude
import fr.corpauration.group.GroupEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.time.LocalDate
import java.util.*

@Entity
class HomeworkEntity {
    var id: UUID
    var title: String
    var level: Int
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @fr.corpauration.utils.Lazy
    var content: String?
    var date: LocalDate
    @OneToOne(id = Int::class)
    var group: GroupEntity

    constructor(
        id: UUID = UUID.randomUUID(),
        title: String = "",
        level: Int = -1,
        content: String? = null,
        date: LocalDate = LocalDate.MIN,
        group: GroupEntity = GroupEntity()
    ) {
        this.id = id
        this.title = title
        this.level = level
        this.content = content
        this.date = date
        this.group = group
    }

    companion object { }
}