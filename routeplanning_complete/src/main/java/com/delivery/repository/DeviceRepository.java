package com.delivery.repository;

import com.delivery.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    @Query(value = "SELECT * FROM devices d WHERE " +
            "d.status = 'AVAILABLE' AND " +
            "ST_Distance_Sphere(point(d.longitude, d.latitude), point(:lng, :lat)) <= :radius * 1000",
            nativeQuery = true)
    List<Device> findAvailableDevicesNearLocation(@Param("lat") double lat,
                                                  @Param("lng") double lng,
                                                  @Param("radius") double radius);
}