-- =============================================
-- NYC Delivery Path Recommendation System
-- PostgreSQL Database Schema
-- =============================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;

-- =============================================
-- Table: stations (配送站点表)
-- =============================================
CREATE TABLE IF NOT EXISTS stations (
                                        station_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    available_drones INTEGER DEFAULT 0,
    available_robots INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- =============================================
-- Table: devices (配送设备表)
-- =============================================
CREATE TABLE IF NOT EXISTS devices (
                                       device_id VARCHAR(50) PRIMARY KEY,
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('DRONE', 'ROBOT')),
    station_id VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    battery_level INTEGER NOT NULL CHECK (battery_level BETWEEN 0 AND 100),
    max_weight DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'BUSY', 'CHARGING', 'MAINTENANCE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE CASCADE
    );

-- =============================================
-- Table: orders (订单表)
-- =============================================
CREATE TABLE IF NOT EXISTS orders (
                                      order_id VARCHAR(50) PRIMARY KEY,
    origin_lat DOUBLE PRECISION NOT NULL,
    origin_lng DOUBLE PRECISION NOT NULL,
    origin_address VARCHAR(255),
    destination_lat DOUBLE PRECISION NOT NULL,
    destination_lng DOUBLE PRECISION NOT NULL,
    destination_address VARCHAR(255),
    package_weight DOUBLE PRECISION NOT NULL CHECK (package_weight > 0),
    delivery_time_requirement TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
                                                            status IN ('PENDING', 'ASSIGNED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED')
    ),
    assigned_device_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assigned_device_id) REFERENCES devices(device_id) ON DELETE SET NULL
    );

-- =============================================
-- Table: route_records (路径记录表)
-- =============================================
CREATE TABLE IF NOT EXISTS route_records (
                                             record_id SERIAL PRIMARY KEY,
                                             order_id VARCHAR(50) NOT NULL,
    device_id VARCHAR(50) NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    recommendation_type VARCHAR(20) NOT NULL CHECK (
                                                       recommendation_type IN ('FASTEST', 'CHEAPEST', 'OPTIMAL')
    ),
    distance_km DOUBLE PRECISION NOT NULL,
    duration_minutes INTEGER NOT NULL,
    cost_usd DOUBLE PRECISION NOT NULL,
    waypoints JSONB NOT NULL,
    time_score DOUBLE PRECISION CHECK (time_score BETWEEN 0 AND 1),
    cost_score DOUBLE PRECISION CHECK (cost_score BETWEEN 0 AND 1),
    total_score DOUBLE PRECISION CHECK (total_score BETWEEN 0 AND 1),
    selected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
    );

-- =============================================
-- Table: geocoding_cache (地理编码缓存表)
-- =============================================
CREATE TABLE IF NOT EXISTS geocoding_cache (
                                               cache_id SERIAL PRIMARY KEY,
                                               address VARCHAR(255) UNIQUE NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days')
    );

-- =============================================
-- Indexes (索引)
-- =============================================

-- Station location spatial index
CREATE INDEX IF NOT EXISTS idx_stations_location
    ON stations USING GIST (ll_to_earth(latitude, longitude));

-- Device indexes
CREATE INDEX IF NOT EXISTS idx_devices_location
    ON devices USING GIST (ll_to_earth(latitude, longitude));

CREATE INDEX IF NOT EXISTS idx_devices_status
    ON devices(status);

CREATE INDEX IF NOT EXISTS idx_devices_type
    ON devices(device_type);

CREATE INDEX IF NOT EXISTS idx_devices_station
    ON devices(station_id);

-- Order indexes
CREATE INDEX IF NOT EXISTS idx_orders_status
    ON orders(status);

CREATE INDEX IF NOT EXISTS idx_orders_created_at
    ON orders(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_assigned_device
    ON orders(assigned_device_id);

-- Route record indexes
CREATE INDEX IF NOT EXISTS idx_route_records_order_id
    ON route_records(order_id);

CREATE INDEX IF NOT EXISTS idx_route_records_selected
    ON route_records(selected);

CREATE INDEX IF NOT EXISTS idx_route_records_created_at
    ON route_records(created_at DESC);

-- Geocoding cache indexes
CREATE INDEX IF NOT EXISTS idx_geocoding_cache_address
    ON geocoding_cache(address);

CREATE INDEX IF NOT EXISTS idx_geocoding_cache_expires
    ON geocoding_cache(expires_at);

-- =============================================
-- Triggers (触发器)
-- =============================================

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to stations
DROP TRIGGER IF EXISTS trigger_stations_updated_at ON stations;
CREATE TRIGGER trigger_stations_updated_at
    BEFORE UPDATE ON stations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to devices
DROP TRIGGER IF EXISTS trigger_devices_updated_at ON devices;
CREATE TRIGGER trigger_devices_updated_at
    BEFORE UPDATE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to orders
DROP TRIGGER IF EXISTS trigger_orders_updated_at ON orders;
CREATE TRIGGER trigger_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- Stored Functions (存储函数)
-- =============================================

-- Function: Get available devices within radius
CREATE OR REPLACE FUNCTION get_available_devices_within_radius(
    search_lat DOUBLE PRECISION,
    search_lng DOUBLE PRECISION,
    radius_km DOUBLE PRECISION DEFAULT 5.0
)
RETURNS TABLE (
    device_id VARCHAR,
    device_type VARCHAR,
    station_id VARCHAR,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    battery_level INTEGER,
    max_weight DOUBLE PRECISION,
    status VARCHAR,
    distance_km DOUBLE PRECISION
) AS $$
BEGIN
RETURN QUERY
SELECT
    d.device_id,
    d.device_type,
    d.station_id,
    d.latitude,
    d.longitude,
    d.battery_level,
    d.max_weight,
    d.status,
    earth_distance(
            ll_to_earth(search_lat, search_lng),
            ll_to_earth(d.latitude, d.longitude)
    ) / 1000.0 AS distance_km
FROM devices d
WHERE
    d.status = 'AVAILABLE'
  AND d.battery_level >= 20
  AND earth_box(ll_to_earth(search_lat, search_lng), radius_km * 1000) @>
      ll_to_earth(d.latitude, d.longitude)
ORDER BY distance_km;
END;
$$ LANGUAGE plpgsql;

-- Function: Update station device counts
CREATE OR REPLACE FUNCTION update_station_device_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
UPDATE stations SET
                    available_drones = (
                        SELECT COUNT(*) FROM devices
                        WHERE station_id = NEW.station_id
                          AND device_type = 'DRONE'
                          AND status = 'AVAILABLE'
                    ),
                    available_robots = (
                        SELECT COUNT(*) FROM devices
                        WHERE station_id = NEW.station_id
                          AND device_type = 'ROBOT'
                          AND status = 'AVAILABLE'
                    )
WHERE station_id = NEW.station_id;
END IF;

    IF TG_OP = 'DELETE' THEN
UPDATE stations SET
                    available_drones = (
                        SELECT COUNT(*) FROM devices
                        WHERE station_id = OLD.station_id
                          AND device_type = 'DRONE'
                          AND status = 'AVAILABLE'
                    ),
                    available_robots = (
                        SELECT COUNT(*) FROM devices
                        WHERE station_id = OLD.station_id
                          AND device_type = 'ROBOT'
                          AND status = 'AVAILABLE'
                    )
WHERE station_id = OLD.station_id;
END IF;

RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to auto-update station counts
DROP TRIGGER IF EXISTS trigger_update_station_counts ON devices;
CREATE TRIGGER trigger_update_station_counts
    AFTER INSERT OR UPDATE OR DELETE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION update_station_device_counts();

-- =============================================
-- Test Data (测试数据)
-- =============================================

-- Insert stations (NYC locations)
INSERT INTO stations (station_id, name, latitude, longitude, available_drones, available_robots) VALUES
                                                                                                     ('ST001', 'Manhattan Central Station', 40.7580, -73.9855, 0, 0),
                                                                                                     ('ST002', 'Brooklyn Distribution Center', 40.6782, -73.9442, 0, 0),
                                                                                                     ('ST003', 'Queens Hub', 40.7282, -73.7949, 0, 0),
                                                                                                     ('ST004', 'Bronx Station', 40.8448, -73.8648, 0, 0)
    ON CONFLICT (station_id) DO NOTHING;

-- Insert drones
INSERT INTO devices (device_id, device_type, station_id, latitude, longitude, battery_level, max_weight, status) VALUES
                                                                                                                     ('DRONE001', 'DRONE', 'ST001', 40.7580, -73.9855, 95, 5.0, 'AVAILABLE'),
                                                                                                                     ('DRONE002', 'DRONE', 'ST001', 40.7580, -73.9855, 88, 5.0, 'AVAILABLE'),
                                                                                                                     ('DRONE003', 'DRONE', 'ST002', 40.6782, -73.9442, 92, 5.0, 'AVAILABLE'),
                                                                                                                     ('DRONE004', 'DRONE', 'ST003', 40.7282, -73.7949, 85, 5.0, 'AVAILABLE'),
                                                                                                                     ('DRONE005', 'DRONE', 'ST004', 40.8448, -73.8648, 90, 5.0, 'AVAILABLE')
    ON CONFLICT (device_id) DO NOTHING;

-- Insert robots
INSERT INTO devices (device_id, device_type, station_id, latitude, longitude, battery_level, max_weight, status) VALUES
                                                                                                                     ('ROBOT001', 'ROBOT', 'ST001', 40.7580, -73.9855, 100, 10.0, 'AVAILABLE'),
                                                                                                                     ('ROBOT002', 'ROBOT', 'ST001', 40.7580, -73.9855, 85, 10.0, 'AVAILABLE'),
                                                                                                                     ('ROBOT003', 'ROBOT', 'ST002', 40.6782, -73.9442, 90, 10.0, 'AVAILABLE'),
                                                                                                                     ('ROBOT004', 'ROBOT', 'ST003', 40.7282, -73.7949, 78, 10.0, 'AVAILABLE'),
                                                                                                                     ('ROBOT005', 'ROBOT', 'ST004', 40.8448, -73.8648, 95, 10.0, 'AVAILABLE')
    ON CONFLICT (device_id) DO NOTHING;

-- =============================================
-- Utility Views (实用视图)
-- =============================================

-- View: Station summary with device counts
CREATE OR REPLACE VIEW v_station_summary AS
SELECT
    s.station_id,
    s.name,
    s.latitude,
    s.longitude,
    COUNT(CASE WHEN d.device_type = 'DRONE' AND d.status = 'AVAILABLE' THEN 1 END) as available_drones,
    COUNT(CASE WHEN d.device_type = 'ROBOT' AND d.status = 'AVAILABLE' THEN 1 END) as available_robots,
    COUNT(CASE WHEN d.status = 'BUSY' THEN 1 END) as busy_devices,
    COUNT(CASE WHEN d.status = 'CHARGING' THEN 1 END) as charging_devices
FROM stations s
         LEFT JOIN devices d ON s.station_id = d.station_id
GROUP BY s.station_id, s.name, s.latitude, s.longitude;

-- View: Active orders with device info
CREATE OR REPLACE VIEW v_active_orders AS
SELECT
    o.order_id,
    o.origin_address,
    o.destination_address,
    o.package_weight,
    o.status,
    o.assigned_device_id,
    d.device_type,
    d.battery_level,
    s.name as station_name,
    o.created_at
FROM orders o
         LEFT JOIN devices d ON o.assigned_device_id = d.device_id
         LEFT JOIN stations s ON d.station_id = s.station_id
WHERE o.status IN ('PENDING', 'ASSIGNED', 'IN_TRANSIT')
ORDER BY o.created_at DESC;

-- =============================================
-- Completion Message
-- =============================================
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully!';
    RAISE NOTICE 'Tables created: stations, devices, orders, route_records, geocoding_cache';
    RAISE NOTICE 'Test data inserted: 4 stations, 5 drones, 5 robots';
END $$;