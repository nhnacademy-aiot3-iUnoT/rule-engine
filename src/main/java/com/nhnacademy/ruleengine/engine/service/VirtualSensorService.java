package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.FlowEngine;
import com.nhnacademy.ruleengine.engine.dto.LocationCreateRequest;
import com.nhnacademy.ruleengine.engine.exception.VirtualSensorFlowException;
import com.nhnacademy.ruleengine.engine.flow.VirtualSensorFlow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.nhnacademy.ruleengine.engine.flow.VirtualSensorFlow.FLOW_ID;

@Service
@RequiredArgsConstructor
public class VirtualSensorService {
    private final FlowEngine flowEngine;
    private final RuleEngineProperties ruleEngineProperties;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;

    private Map<String, Object> sensorConfig;

    public void createAndStart(Long organizationId, Long storageId, Long sectionId, LocationCreateRequest request) {

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


        String flowId = FLOW_ID + sectionId;

        // 이미 해당 sectionId로 가상데이터 생성하는 Flow가 있는지확인
        if (flowEngine.getFlows().containsKey(flowId)) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_FLOW_ALREADY_EXISTS);
        }


        VirtualSensorFlow flow = new VirtualSensorFlow(sectionId.toString(), ruleEngineProperties, mqttNodeConfigFactory, sensorConfig);
        flowEngine.registerAndStart(flow.create());

    }
}
