package fr.corpauration.schedule

import fr.corpauration.utils.Entity
import java.time.LocalDateTime

@Entity
class CourseEntity//        this.groups = groups//        groups: List<GroupEntity> = emptyList()
    (
    var id: String = "",
    var start: LocalDateTime = LocalDateTime.MIN,
    var end: LocalDateTime? = null,
    var category: Int = CourseCategory.DEFAULT.ordinal,
    var subject: String? = null,
    var teachers: String = "",
    var rooms: String = ""
) {
    /*@ManyToMany(junction_table = "courses_groups")
    var groups: List<GroupEntity>*/

    companion object
}