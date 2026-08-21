package com.nhnacademy.ruleengine.engine.repository.support;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Flux 질의 실행과 실패 처리를 한곳에 모은다.
 * <p>
 * 질의를 던지고 결과를 평탄화하고 실패를 도메인 예외로 바꾸는 절차는 어느 버킷을 읽든 똑같다.
 * 리포지토리마다 같은 try-catch를 두면 실패 로그와 예외 종류가 조금씩 갈라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FluxQueryExecutor {

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;

    public <T> List<T> query(
            String fluxQuery,
            Function<FluxRecord, T> mapper
    ) {
        return query(fluxQuery).stream()
                .map(mapper)
                .toList();
    }

    public List<FluxRecord> query(String fluxQuery) {
        try {
            return influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDbProperties.org())
                    .stream()
                    .flatMap(table -> table.getRecords().stream())
                    .toList();

        } catch (Exception exception) {
            log.error("InfluxDB 조회에 실패했습니다.", exception);

            throw new SensorDataException(ErrorCode.SENSOR_DATA_QUERY_FAILED);
        }
    }
}
