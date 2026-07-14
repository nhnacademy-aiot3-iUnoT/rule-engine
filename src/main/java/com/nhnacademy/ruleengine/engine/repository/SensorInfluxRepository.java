package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.nhnacademy.ruleengine.engine.exception.SensorDataSaveException;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
// 센서 데이터를 InfluxDB Point로 변환해 저장한다.
public class SensorInfluxRepository {

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;

    public void save(
            Long locationId,
            String applicationName,
            String location,
            String sensorType,
            double value,
            String unit,
            String deviceName,
            String deviceEui,
            Instant timestamp
    ) {
        // 조회 조건은 tag로, 실제 측정값은 field로 구성한다.
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag("location_id", String.valueOf(locationId))
                .addTag("application_name", applicationName)
                .addTag("location", location)
                .addTag("sensor_type", sensorType)
                .addTag("device_name", deviceName)
                .addTag("device_eui", deviceEui)
                .addTag("unit", unit)
                .addField("value", value)
                .time(timestamp, WritePrecision.NS);
        try {
            // 즉시 결과를 확인할 수 있도록 blocking write API를 사용한다.
            influxDBClient.getWriteApiBlocking()
                    .writePoint(
                            influxDbProperties.bucket(),
                            influxDbProperties.org(),
                            point
                    );
        } catch (Exception e) {
            throw new SensorDataSaveException("InfluxDB 저장 실패", e);
        }
    }
}
