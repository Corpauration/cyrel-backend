package fr.corpauration.schedule

import fr.corpauration.utils.Entity
import java.time.LocalDateTime

@Entity
class CourseEntity {
    var id: String
    var start: LocalDateTime
    var end: LocalDateTime?
    var category: Int
    var subject: String?
    var teachers: String
    var rooms: String
    /*@ManyToMany(junction_table = "courses_groups")
    var groups: List<GroupEntity>*/

    constructor(
        id: String = "",
        start: LocalDateTime = LocalDateTime.MIN,
        end: LocalDateTime? = null,
        category: Int = CourseCategory.DEFAULT.ordinal,
        subject: String? = null,
        teachers: String = "",
        rooms: String = "",
//        groups: List<GroupEntity> = emptyList()
    ) {
        this.id = id
        this.start = start
        this.end = end
        this.category = category
        this.subject = subject
        this.teachers = teachers
        this.rooms = rooms
//        this.groups = groups
    }

    companion object
}