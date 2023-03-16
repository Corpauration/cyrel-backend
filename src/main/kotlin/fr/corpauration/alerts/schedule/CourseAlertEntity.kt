package fr.corpauration.alerts.schedule

import fr.corpauration.utils.Entity
import java.time.LocalDateTime

@Entity
class CourseAlertEntity(
    var id: String = "",
    var group: Int = -100,
    var time: LocalDateTime = LocalDateTime.MIN,
    var event: Int = -1,
) {
    companion object
}