package fr.corpauration.schedule

import fr.corpauration.group.GroupEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.ManyToMany
import java.time.LocalDate

@Entity
class CourseEntity {
    var id: String
    var start: LocalDate
    var end: LocalDate?
    var category: CourseCategory
    var subject: String?
    var teachers: String
    var rooms: String
    @ManyToMany(junction_table = "courses_groups")
    var groups: List<GroupEntity>

    constructor(
        id: String = "",
        start: LocalDate = LocalDate.MIN,
        end: LocalDate? = null,
        category: CourseCategory = CourseCategory.DEFAULT,
        subject: String? = null,
        teachers: String = "",
        rooms: String = "",
        groups: List<GroupEntity> = emptyList()
    ) {
        this.id = id
        this.start = start
        this.end = end
        this.category = category
        this.subject = subject
        this.teachers = teachers
        this.rooms = rooms
        this.groups = groups
    }

    companion object
}