package robotcontrol.service;

import org.springframework.stereotype.Service;
import robotcontrol.dto.LocationRequest;
import robotcontrol.dto.LocationResponse;
import robotcontrol.dto.RemainingTimeResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 简单的内存实现：
 * - 保存最新位置
 * - 保存历史轨迹
 * - 用“总时长 - 已过时长”估算剩余时间（演示用）
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    static class DeviceState {
        LocationResponse latest;
        List<LocationResponse> history = new ArrayList<>();
        // 简化的路线时长（假设每次任务 30 分钟），真实系统应来自 route_records
        long taskStartMillis = -1;
        long totalDurationSec = 30 * 60;
    }

    private final Map<String, DeviceState> store = new ConcurrentHashMap<>();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void updateLocation(LocationRequest req) {
        DeviceState st = store.computeIfAbsent(req.getDeviceId(), k -> new DeviceState());
        LocationResponse point = new LocationResponse(req.getDeviceId(), req.getLat(), req.getLng(), req.getTimestamp());
        st.latest = point;
        st.history.add(point);
        if (st.taskStartMillis < 0) {
            st.taskStartMillis = req.getTimestamp();
        }
    }

    @Override
    public LocationResponse getCurrentLocation(String deviceId) {
        DeviceState st = store.get(deviceId);
        if (st == null || st.latest == null) throw new NoSuchElementException("device not found: " + deviceId);
        return st.latest;
    }

    @Override
    public RemainingTimeResponse getRemainingTime(String deviceId) {
        DeviceState st = store.get(deviceId);
        if (st == null || st.latest == null) throw new NoSuchElementException("device not found: " + deviceId);
        long now = Instant.now().toEpochMilli();
        long elapsedSec = Math.max(0, (now - st.taskStartMillis) / 1000);
        long remain = Math.max(0, st.totalDurationSec - elapsedSec);
        String eta = OffsetDateTime.ofInstant(Instant.ofEpochMilli(now + remain * 1000), ZoneOffset.UTC).format(ISO);
        return new RemainingTimeResponse(deviceId, remain, eta);
    }

    @Override
    public List<LocationResponse> getLocationHistory(String deviceId, String start, String end) {
        DeviceState st = store.get(deviceId);
        if (st == null) return List.of();
        long startMs = 0L;
        long endMs = Long.MAX_VALUE;
        try {
            if (start != null) startMs = Instant.parse(start).toEpochMilli();
            if (end != null) endMs = Instant.parse(end).toEpochMilli();
        } catch (Exception ignored) {}
        long s = startMs, e = endMs;
        return st.history.stream()
                .filter(p -> p.getTimestamp() >= s && p.getTimestamp() <= e)
                .sorted(Comparator.comparingLong(LocationResponse::getTimestamp))
                .collect(Collectors.toList());
    }
}
