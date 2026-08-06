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

    // 이번 process() 호출에서 다음 노드로 메시지를 넘겼는지 추적한다.
    // 노드 하나가 여러 Connection 스레드에서 동시에 호출될 수 있어 스레드별로 관리한다.
    private static final ThreadLocal<Boolean> messageForwarded =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

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
        int delivered = outputPorts.get(portName).send(message);

        if (delivered > 0) {
            messageForwarded.set(Boolean.TRUE);
        }
    }

    protected abstract void onProcess(Message message);

    /**
     * 메시지를 처리하고, 이 노드에서 흐름이 끝났으면 요청자에게 완료를 알린다.
     * <p>
     * 노드가 다음 노드로 메시지를 넘기지 않았다면 그 메시지의 여정은 여기서 끝난 것이다.
     * 완료를 알리지 않으면 요청자(RabbitMQ 리스너 등)가 타임아웃까지 대기하므로,
     * <b>각 노드가 아니라 이 자리에서 한 번만</b> 처리한다.
     * <p>
     * onProcess()가 예외를 던진 경우에는 완료로 처리하지 않는다.
     * FlowEngine이 잡아서 실패로 알려야 요청자가 재시도 여부를 판단할 수 있다.
     */
    public final void process(Message message) {
        Boolean previous = messageForwarded.get();
        messageForwarded.set(Boolean.FALSE);

        try {
            onProcess(message);

            if (Boolean.FALSE.equals(messageForwarded.get())) {
                message.completeProcessing();
            }
        } finally {
            messageForwarded.set(previous);
        }
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
