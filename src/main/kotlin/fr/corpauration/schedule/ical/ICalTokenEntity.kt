package fr.corpauration.schedule.ical

import fr.corpauration.user.UserEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.util.*

@Entity
class ICalTokenEntity(
    var id: UUID = UUID.randomUUID(),
    var email: String = ""
) {
    companion object
}