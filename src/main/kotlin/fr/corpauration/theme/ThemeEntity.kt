package fr.corpauration.theme

import fr.corpauration.utils.Entity

@Entity
class ThemeEntity(
    var id: Int = -1,
    var background: String = "",
    var foreground: String = "",
    var card: String = "",
    var navIcon: String = "",
) {
    companion object
}