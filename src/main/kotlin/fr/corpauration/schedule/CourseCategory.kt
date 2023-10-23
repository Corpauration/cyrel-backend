package fr.corpauration.schedule

enum class CourseCategory {
    DEFAULT, CM, TD, ACCUEIL, EXAMENS, INDISPONIBILITE, REUNIONS, MANIFESTATION, PROJET_ENCADRE_TUTORE
}

fun CourseCategory.toColor(): String = when (this) {
    CourseCategory.DEFAULT -> "blue"
    CourseCategory.CM -> "#c42626"
    CourseCategory.TD -> "#2626c4"
    CourseCategory.ACCUEIL -> "#26c426"
    CourseCategory.EXAMENS -> "#26c4c4"
    CourseCategory.INDISPONIBILITE -> "#383838"
    CourseCategory.REUNIONS -> "#6438c4"
    CourseCategory.MANIFESTATION -> "#c46438"
    CourseCategory.PROJET_ENCADRE_TUTORE -> "#c438c4"
}
