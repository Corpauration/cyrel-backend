package fr.corpauration.rooms

import fr.corpauration.schedule.CourseEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.ManyToMany

@Entity
class RoomEntity(
    var id: String = "",
    var name: String = "",
    var capacity: Int = 0,
    var computers: Boolean = false,
    @ManyToMany(junction_table = "rooms_courses") var courses: List<CourseEntity> = emptyList(),
) {
    companion object
}