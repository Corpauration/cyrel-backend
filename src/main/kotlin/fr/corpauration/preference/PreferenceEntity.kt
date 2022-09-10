package fr.corpauration.preference

import fr.corpauration.theme.ThemeEntity
import fr.corpauration.utils.Entity
import fr.corpauration.utils.OneToOne
import java.util.*

@Entity
class PreferenceEntity(
    var id: UUID = UUID.randomUUID(),
    @OneToOne(id = Int::class) var theme: ThemeEntity? = ThemeEntity()
) {
    companion object
}