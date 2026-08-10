package com.nhnacademy.ruleengine.engine.core;

import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTest {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    // 출력 포트를 가진 중간 노드
    private static class PassThroughNode extends AbstractNode {
        PassThroughNode(String id) {
            super(id);
            addInputPort(INPUT_PORT);
            addOutputPort(OUTPUT_PORT);
        }

        @Override
        protected void onProcess(Message message) {
            send(OUTPUT_PORT, message);
        }
    }

    // 출력 포트가 없는 종단 노드
    private static class TerminalNode extends AbstractNode {
        TerminalNode(String id) {
            super(id);
            addInputPort(INPUT_PORT);
        }

        @Override
        protected void onProcess(Message message) {
            // 처리 후 종료한다.
        }
    }

    @Test
    @DisplayName("출력 포트에 연결된 Connection이 없으면 검증에 실패한다")
    void validateDetectsUnconnectedOutputPort() {
        Flow flow = new Flow("test-flow")
                .addNode(new PassThroughNode("source"))
                .addNode(new PassThroughNode("dangling"))
                .connect("source", OUTPUT_PORT, "dangling", INPUT_PORT);

        List<String> errors = flow.validate();

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().contains("dangling"));
        assertTrue(errors.getFirst().contains(OUTPUT_PORT));
    }

    @Test
    @DisplayName("출력 포트가 모두 연결되고 종단 노드로 끝나면 검증을 통과한다")
    void validatePassesWhenEveryOutputPortIsConnected() {
        Flow flow = new Flow("test-flow")
                .addNode(new PassThroughNode("source"))
                .addNode(new TerminalNode("terminal"))
                .connect("source", OUTPUT_PORT, "terminal", INPUT_PORT);

        assertTrue(flow.validate().isEmpty());
    }
}
