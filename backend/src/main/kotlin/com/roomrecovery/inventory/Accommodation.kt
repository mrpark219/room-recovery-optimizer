package com.roomrecovery.inventory

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "accommodation")
class Accommodation(
    @Id var id: Long,
    var name: String
)
