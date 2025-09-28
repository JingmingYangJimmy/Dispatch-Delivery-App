package com.delivery.routeplanning.repository;

import com.delivery.routeplanning.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, String> {
}
