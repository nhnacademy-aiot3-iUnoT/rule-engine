package com.nhnacademy.ruleengine.engine.dto.sensor.query;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SensorLatestResponseTest {

    @Test
    void 센서_페이로드를_최신_조회_응답으로_변환한다() {
        SensorPayload payload = new SensorPayload(
                1L,
                "device-1",
                2L,
                3L,
                "temperature",
                21.5,
                "C",
                "2026-07-30T10:00:00Z"
        );

        SensorLatestResponse response = SensorLatestResponse.from(payload);

        assertThat(response).isEqualTo(new SensorLatestResponse(
                1L,
                "device-1",
                2L,
                3L,
                "temperature",
                21.5,
                "C",
                "2026-07-30T10:00:00Z"
        ));
    }

    @Test
    void 센서_페이로드가_null이면_변환할_수_없다() {
        assertThatNullPointerException()
                .isThrownBy(() -> SensorLatestResponse.from(null))
                .withMessage("센서 데이터는 필수입니다.");
    }
}
