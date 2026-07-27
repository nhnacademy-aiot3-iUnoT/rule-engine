package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.FlowEngine;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatus;
import com.nhnacademy.ruleengine.engine.exception.VirtualSensorFlowException;
import com.nhnacademy.ruleengine.engine.flow.VirtualSensorFlow;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import com.nhnacademy.ruleengine.engine.validation.LocationHierarchyValidator;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class VirtualSensorService {
    private final FlowEngine flowEngine;
    private final LocationHierarchyValidator locationHierarchyValidator;
    private final NormalizedSensorPublisher normalizedSensorPublisher;

    private static final String FLOW_ID_PREFIX = "virtual-sensor-flow-";

    public void createAndStartFlow(
            Long organizationId,
            Long storageId,
            Long sectionId,
            VirtualSensorCreateRequest request
    ) {
        Map<String, Object> sensorConfig = new HashMap<>();


        sensorConfig = new HashMap<>();
        sensorConfig.put("organizationId", organizationId);
        sensorConfig.put("storageId", storageId);
        sensorConfig.put("sectionId", sectionId);
        sensorConfig.put("tempMin", request.temperature().min());
        sensorConfig.put("tempMax", request.temperature().max());
        sensorConfig.put("humidityMin", request.humidity().min());
        sensorConfig.put("humidityMax", request.humidity().max());
        sensorConfig.put("doorOpenProbability", request.doorOpenProbability());
        sensorConfig.put("measurementInterval", request.measurementIntervalSeconds());
        sensorConfig.put("deviceEui", request.deviceEui());


        String flowId = virtualSensorFlowId(sectionId);

        // 이미 해당 sectionId로 가상데이터 생성하는 Flow가 있는지확인
        if (flowEngine.getFlows().containsKey(flowId)) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_FLOW_ALREADY_EXISTS);
        }


        VirtualSensorFlow flow = new VirtualSensorFlow(sectionId.toString(), sensorConfig, normalizedSensorPublisher);
        flowEngine.registerAndStart(flow.create());

    }

    public void changeFlowStatus(
            Long organizationId,
            Long storageId,
            Long sectionId,
            VirtualSensorStatus status
    ) {

        locationHierarchyValidator.validateSection(organizationId, storageId, sectionId);

        if (status == VirtualSensorStatus.INACTIVE) {
            flowEngine.stopFlow(virtualSensorFlowId(sectionId));
            return;
        }

        // ACTIVE라면 저장된 설정을 조회해서 다시 Flow 시작
        if (status == VirtualSensorStatus.ACTIVE) {
            flowEngine.startFlow(virtualSensorFlowId(sectionId));
        }
    }

    private String virtualSensorFlowId(Long sectionId) {
        return FLOW_ID_PREFIX + sectionId;
    }
}
