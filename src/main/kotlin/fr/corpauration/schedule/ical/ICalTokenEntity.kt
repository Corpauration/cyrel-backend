package fr.corpauration.schedule.ical

import fr.corpauration.user.UserEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.util.*

@Entity
class ICalTokenEntity(
    var id: String = "",
    @OneToOne(id = UUID::class) var user: UserEntity? = UserEntity(),
    var private: String = "",
    var public: String = ""
) {
    companion object
}