package com.nhnacademy.ruleengine.engine.node;

import com.nhnacademy.ruleengine.engine.connection.InputPort;
import com.nhnacademy.ruleengine.engine.connection.OutputPort;
import com.nhnacademy.ruleengine.engine.connection.impl.DefaultInputPort;
import com.nhnacademy.ruleengine.engine.connection.impl.DefaultOutputPort;
import com.nhnacademy.ruleengine.engine.core.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
// 포트 관리와 메시지 처리 흐름을 제공하는 기본 노드다.
public abstract class AbstractNode implements Node {
    @Getter
    private final String id;

    Map<String, InputPort> inputPorts;
    Map<String, OutputPort> outputPorts;

    protected AbstractNode(String id) {
        this.id = id;
        inputPorts = new HashMap<>();
        outputPorts = new HashMap<>();
    }

    protected void addInputPort(String name) {
        inputPorts.put(name, new DefaultInputPort(name, this));
    }

    protected void addOutputPort(String name) {
        outputPorts.put(name, new DefaultOutputPort(name));
    }

    public InputPort getInputPort(String name) {
        return inputPorts.get(name);
    }

    public OutputPort getOutputPort(String name) {
        return outputPorts.get(name);
    }

    protected void send(String portName, Message message) {
        // 지정한 출력 포트에 연결된 모든 Connection으로 전송한다.
        outputPorts.get(portName).send(message);
    }

    protected abstract void onProcess(Message message);

    public void process(Message message) {
        // 실제 메시지 처리는 하위 노드에 위임한다.
        onProcess(message);

    }

    public List<InputPort> getInputPorts() {
        return this.inputPorts
                .values()
                .stream()
                .toList();
    }


    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }


}
