package com.nhnacademy.ruleengine.engine.dto.sensor;

import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog.ResolvedSection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class SensorContextTest {

    ExternalSensorMessage externalSensorMessage;


    @Test
    @DisplayName("SectionContext 생성")
    void initSensorContext() {
        externalSensorMessage = new ExternalSensorMessage(
                "testTopic",
                1L,
                "test-time",
                "appName",
                "testDeviceEui",
                "testLocation",
                "testPoint",
                null

        );

        ResolvedSection resolvedSection = new ResolvedSection(1L, 1L, 1L);

        SensorContext sensorContext = SensorContext.from(externalSensorMessage, resolvedSection);
        assertAll(
                () -> {
                    assertEquals("test-time", sensorContext.time());
                    assertEquals(1L, sensorContext.organizationId());
                    assertEquals("testDeviceEui", sensorContext.deviceEui());
                    assertEquals(1L, sensorContext.storageId());
                    assertEquals(1L, sensorContext.sectionId());
                }
        );
    }

    @Test
    @DisplayName("Time Null 일때 SectionContext 생성")
    void initTimeNullSensorContext() {

        externalSensorMessage = new ExternalSensorMessage(
                "testTopic",
                1L,
                null,
                "appName",
                "testDeviceEui",
                "testLocation",
                "testPoint",
                null

        );

        ResolvedSection resolvedSection = new ResolvedSection(1L, 1L, 1L);

        SensorContext sensorContext = SensorContext.from(externalSensorMessage, resolvedSection);
        assertAll(
                () -> {
                    assertEquals("unknown", sensorContext.time());
                    assertEquals(1L, sensorContext.organizationId());
                    assertEquals("testDeviceEui", sensorContext.deviceEui());
                    assertEquals(1L, sensorContext.storageId());
                    assertEquals(1L, sensorContext.sectionId());
                }
        );
    }

    @Test
    @DisplayName("DeviceEui Null 일때 SectionContext 생성")
    void initDeviceEuiNullSensorContext() {

        externalSensorMessage = new ExternalSensorMessage(
                "testTopic",
                1L,
                "test-time",
                "appName",
                null,
                "testLocation",
                "testPoint",
                null

        );

        ResolvedSection resolvedSection = new ResolvedSection(1L, 1L, 1L);

        SensorContext sensorContext = SensorContext.from(externalSensorMessage, resolvedSection);
        assertAll(
                () -> {
                    assertEquals("test-time", sensorContext.time());
                    assertEquals(1L, sensorContext.organizationId());
                    assertEquals("unknown", sensorContext.deviceEui());
                    assertEquals(1L, sensorContext.storageId());
                    assertEquals(1L, sensorContext.sectionId());
                }
        );
    }

    @Test
    @DisplayName("ResolvedSection이 null일 때 IllegalArgumentException을 던진다")
    void initResolvedSectionNull() {

        externalSensorMessage = new ExternalSensorMessage(
                "testTopic",
                1L,
                "test-time",
                "appName",
                "testDeviceEui",
                "testLocation",
                "testPoint",
                null

        );

        assertThrows(IllegalArgumentException.class, () -> SensorContext.from(externalSensorMessage, null));

    }


}