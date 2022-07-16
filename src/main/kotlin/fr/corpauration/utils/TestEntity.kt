package fr.corpauration.utils

@Entity
class TestEntity {
    var id: Int
    var data: Boolean

    constructor(id: Int = -1, data: Boolean = false) {
        this.id = id
        this.data = data
    }

    companion object { }
}