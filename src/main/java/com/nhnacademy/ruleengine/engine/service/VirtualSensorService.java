package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.FlowLifecycleManager;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatus;
import com.nhnacademy.ruleengine.engine.exception.VirtualSensorFlowException;
import com.nhnacademy.ruleengine.engine.flow.VirtualSensorFlow;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import com.nhnacademy.ruleengine.engine.validation.LocationHierarchyValidator;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VirtualSensorService {
    private final FlowLifecycleManager flowLifecycleManager;
    private final LocationHierarchyValidator locationHierarchyValidator;
    private final NormalizedSensorPublisher normalizedSensorPublisher;

    public void createAndStartFlow(
            Long organizationId,
            Long storageId,
            Long sectionId,
            VirtualSensorCreateRequest request
    ) {
        VirtualSensorConfig sensorConfig = VirtualSensorConfig.from(
                organizationId,
                storageId,
                sectionId,
                request
        );
        String flowId = VirtualSensorFlow.flowId(sectionId);

        // 같은 Section의 가상 센서 Flow가 중복 생성되는 것을 막는다.
        if (flowLifecycleManager.isRegistered(flowId)) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_FLOW_ALREADY_EXISTS);
        }

        VirtualSensorFlow flow = new VirtualSensorFlow(sensorConfig, normalizedSensorPublisher);
        flowLifecycleManager.start(flowId, flow::create);
    }

    public void changeFlowStatus(
            Long organizationId,
            Long storageId,
            Long sectionId,
            VirtualSensorStatus status
    ) {

        locationHierarchyValidator.validateSection(organizationId, storageId, sectionId);

        if (status == VirtualSensorStatus.INACTIVE) {
            flowLifecycleManager.stop(VirtualSensorFlow.flowId(sectionId));
            return;
        }

        // 중지된 기존 Flow를 다시 시작한다.
        if (status == VirtualSensorStatus.ACTIVE) {
            flowLifecycleManager.resume(VirtualSensorFlow.flowId(sectionId));
        }
    }
}
