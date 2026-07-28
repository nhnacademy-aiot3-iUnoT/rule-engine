package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.FlowProcessingCompletion;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;

// Flow의 마지막 단계에서 전체 처리가 끝났음을 알린다.
public class FlowCompletionNode extends AbstractNode {

    private static final String INPUT_PORT = "in";

    public FlowCompletionNode(String id) {
        super(id);
        addInputPort(INPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        FlowProcessingCompletion completion = message.get(
                MessageFields.FLOW_PROCESSING_COMPLETION
        );

        if (completion != null) {
            completion.complete();
        }
    }
}
