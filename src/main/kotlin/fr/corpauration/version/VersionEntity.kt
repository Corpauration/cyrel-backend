package fr.corpauration.version

import fr.corpauration.utils.Entity

@Entity
class VersionEntity(
    var id: Boolean, // Fake id field
    var version: String,
    var platform: String,
    var url: String
) {
    companion object
}