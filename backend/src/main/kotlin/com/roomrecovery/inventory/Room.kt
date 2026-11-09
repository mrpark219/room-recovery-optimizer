package com.roomrecovery.inventory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "room")
class Room(
    @Id var id: Long = 0,
    @Column(name = "accommodation_id") var accommodationId: Long = 0,
    @Column(name = "room_type_id") var roomTypeId: Long = 0,
    var name: String = ""
)
