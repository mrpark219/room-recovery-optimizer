package com.roomrecovery.inventory

import org.springframework.data.jpa.repository.JpaRepository

interface AccommodationRepository : JpaRepository<Accommodation, Long>
