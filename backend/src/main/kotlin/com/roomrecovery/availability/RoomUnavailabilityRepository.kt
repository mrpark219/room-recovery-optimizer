package com.roomrecovery.availability

import org.springframework.data.jpa.repository.JpaRepository

interface RoomUnavailabilityRepository : JpaRepository<RoomUnavailability, Long>, RoomUnavailabilityRepositoryCustom
