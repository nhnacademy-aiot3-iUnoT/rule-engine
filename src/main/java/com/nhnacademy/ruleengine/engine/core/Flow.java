package com.nhnacademy.ruleengine.engine.core;


import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

@Slf4j
// 노드와 Connection을 구성하고 Flow의 생명주기를 관리한다.
public class Flow {
    private enum NodeState {UNVISITED, VISITING, VISITED}

    public enum FlowState {STOPPED, RUNNING}

    @Getter
    private final String id;
    @Getter
    private final Map<String, AbstractNode> nodes;
    @Getter
    private final List<Connection> connections;
    @Getter
    private FlowState flowState;

    private final Map<String, Object> transportConfig;


    public Flow(String id) {
        this(id, null);
    }

    public Flow(String id, Map<String, Object> transportConfig) {
        this.id = Objects.requireNonNull(
                id,
                "Flow ID는 필수입니다."
        );
        this.nodes = new HashMap<>();
        this.connections = new ArrayList<>();
        this.transportConfig = transportConfig;
        flowState = FlowState.STOPPED;
    }

    public Flow addNode(AbstractNode node) {
        // 노드 ID를 기준으로 Flow에 노드를 등록한다.
        nodes.put(node.getId(), node);
        return this;
    }

    public Flow connect(String sourceNodeId, String sourcePort, String targetNodeId, String targetPort) {
        return connect(sourceNodeId, sourcePort, targetNodeId, targetPort, this.transportConfig);
    }

    public Flow connect(String sourceNodeId, String sourcePort, String targetNodeId, String targetPort, Map<String, Object> transportConfig) {
        // 연결 전에 출발/도착 노드와 포트가 존재하는지 확인한다.
        if (!nodes.containsKey(sourceNodeId) || !nodes.containsKey(targetNodeId)) {
            throw new IllegalArgumentException("존재하지 않는 노드 ID입니다.");
        }

        AbstractNode sourceNode = nodes.get(sourceNodeId);
        AbstractNode targetNode = nodes.get(targetNodeId);

        if (sourceNode.getOutputPort(sourcePort) == null || targetNode.getInputPort(targetPort) == null) {
            throw new IllegalArgumentException("존재하지 않는 포트입니다.");
        }

        Connection connection = createConnection(
                this.id, sourceNodeId, sourcePort, targetNodeId, targetPort, transportConfig);

        sourceNode.getOutputPort(sourcePort).connect(connection);
        connection.setTarget(targetNode.getInputPort(targetPort));
        log.debug("[{}] Connection Success {}", id, connection.getId());
        connections.add(connection);

        return this;
    }

    private Connection createConnection(String flowId, String sourceNodeId, String sourcePort,
                                        String targetNodeId, String targetPort,
                                        Map<String, Object> transportConfig) {
        // capacity 설정 유무에 따라 무제한 또는 제한 버퍼를 생성한다.
        String connectionId = String.format("%s:%s -> %s:%s", sourceNodeId, sourcePort, targetNodeId, targetPort);
        Integer capacity = getCapacity(transportConfig);

        if (capacity == null) {
            return new LocalConnection(connectionId);
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("connection capacity must be greater than 0: " + capacity);
        }

        log.debug("[{}] LocalConnection created with capacity {}", flowId, capacity);
        return new LocalConnection(connectionId, capacity);
    }

    private Integer getCapacity(Map<String, Object> transportConfig) {
        if (transportConfig == null || !transportConfig.containsKey("capacity")) {
            return null;
        }

        Object capacity = transportConfig.get("capacity");
        if (capacity instanceof Number number) {
            return number.intValue();
        }

        if (capacity instanceof String text) {
            return Integer.parseInt(text);
        }

        throw new IllegalArgumentException("connection capacity must be a number: " + capacity);
    }

    public Connection getConnection(String connectionId) {
        String normalizedConnectionId = normalizeConnectionId(connectionId);

        return connections.stream()
                .filter(connection -> normalizeConnectionId(connection.getId()).equals(normalizedConnectionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("connection Id Not Found " + connectionId));
    }

    public Connection removeConnection(String connectionId) {
        // 출력 포트와 연결 목록에서 모두 제거한 뒤 Connection을 정지한다.
        Connection connection = getConnection(connectionId);
        ConnectionEndpoint source = parseConnectionId(connection).source();

        AbstractNode sourceNode = nodes.get(source.nodeId());

        if (sourceNode == null || sourceNode.getOutputPort(source.port()) == null) {
            throw new IllegalStateException("Source output port not found: " + source.nodeId() + ":" + source.port());
        }

        sourceNode.getOutputPort(source.port()).disconnect(connection);
        connections.remove(connection);
        connection.stop();

        log.debug("[{}] Connection Removed {}", id, connection.getId());
        return connection;
    }

    public List<Connection> getConnectionsOfNode(String nodeId) {
        return connections.stream()
                .filter(connection -> {
                    ConnectionEndpoints endpoints = parseConnectionId(connection);
                    return endpoints.source().nodeId().equals(nodeId) || endpoints.target().nodeId().equals(nodeId);
                })
                .toList();
    }

    public AbstractNode removeNode(String nodeId) {
        // 남은 연결이 있는 노드는 안전을 위해 제거하지 않는다.
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("node Id Not Found " + nodeId);
        }

        if (!getConnectionsOfNode(nodeId).isEmpty()) {
            throw new IllegalStateException("Node has remaining connections: " + nodeId);
        }

        AbstractNode node = nodes.remove(nodeId);
        node.shutdown();
        log.debug("[{}] Node Removed {}", id, nodeId);
        return node;
    }

    public static String normalizeConnectionId(String connectionId) {
        return connectionId.replaceAll("\\s+", "");
    }

    public void shutdown() {
        // Connection을 먼저 정지한 뒤 각 노드를 종료한다.
        for (Connection connection : connections) {
            connection.stop();
        }
        for (AbstractNode node : nodes.values()) {
            node.shutdown();
        }
        flowState = FlowState.STOPPED;

    }

    public void initialize() {
        // 모든 노드 초기화가 끝나면 Flow를 실행 상태로 변경한다.
        for (AbstractNode node : nodes.values()) {
            node.initialize();
        }
        flowState = FlowState.RUNNING;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (nodes.isEmpty()) {
            errors.add(
                    "에러: Flow에 등록된 노드가 하나도 없습니다."
            );
            return errors;
        }

        Map<String, List<String>> nodeGraph =
                createNodeGraph(errors);

        if (containsCycle(nodeGraph)) {
            errors.add(
                    "에러: Flow 내에서 순환참조가 발생하였습니다"
            );
        }

        return errors;
    }

    private Map<String, List<String>> createNodeGraph(
            List<String> errors
    ) {
        Map<String, List<String>> nodeGraph =
                new HashMap<>();

        for (String nodeId : nodes.keySet()) {
            nodeGraph.put(nodeId, new ArrayList<>());
        }

        for (Connection connection : connections) {
            addConnectionToGraph(
                    connection,
                    nodeGraph,
                    errors
            );
        }

        return nodeGraph;
    }

    private void addConnectionToGraph(
            Connection connection,
            Map<String, List<String>> nodeGraph,
            List<String> errors
    ) {
        ConnectionEndpoints endpoints =
                parseConnectionId(connection);

        String sourceId =
                endpoints.source().nodeId();

        String targetId =
                endpoints.target().nodeId();

        boolean sourceExists =
                nodes.containsKey(sourceId);

        boolean targetExists =
                nodes.containsKey(targetId);

        if (!sourceExists) {
            errors.add(
                    String.format(
                            "에러: 연결[%s]의 소스 노드(%s)가 존재하지 않습니다.",
                            connection.getId(),
                            sourceId
                    )
            );
        }

        if (!targetExists) {
            errors.add(
                    String.format(
                            "에러: 연결[%s]의 대상 노드(%s)가 존재하지 않습니다.",
                            connection.getId(),
                            targetId
                    )
            );
        }

        if (sourceExists && targetExists) {
            nodeGraph.get(sourceId)
                    .add(targetId);
        }
    }

    private boolean containsCycle(
            Map<String, List<String>> nodeGraph
    ) {
        Map<String, NodeState> nodeStates =
                new HashMap<>();

        for (String nodeId : nodes.keySet()) {
            nodeStates.put(
                    nodeId,
                    NodeState.UNVISITED
            );
        }

        for (Entry<String, NodeState> entry
                : nodeStates.entrySet()) {

            if (entry.getValue() == NodeState.UNVISITED
                    && checkCycle(
                    entry.getKey(),
                    nodeStates,
                    nodeGraph
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean checkCycle(String nodeId, Map<String, NodeState> nodeStates, Map<String, List<String>> nodeGraph) {
        // DFS 방문 상태를 이용해 순환 참조를 찾는다.
        nodeStates.put(nodeId, NodeState.VISITING);


        List<String> neighbors = nodeGraph.get(nodeId);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                NodeState neighborState = nodeStates.get(neighbor);

                if (neighborState == NodeState.VISITING) {
                    return true;
                }

                if (neighborState == NodeState.UNVISITED && checkCycle(neighbor, nodeStates, nodeGraph)) {
                    return true;
                }
            }
        }

        nodeStates.put(nodeId, NodeState.VISITED);
        return false;
    }

    private ConnectionEndpoints parseConnectionId(Connection connection) {
        String[] parts = connection.getId().split("->");
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid connection id: " + connection.getId());
        }

        return new ConnectionEndpoints(
                parseEndpoint(connection.getId(), parts[0]),
                parseEndpoint(connection.getId(), parts[1])
        );
    }

    private ConnectionEndpoint parseEndpoint(String connectionId, String endpoint) {
        String[] parts = endpoint.strip().split(":");
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid connection id: " + connectionId);
        }

        return new ConnectionEndpoint(parts[0].strip(), parts[1].strip());
    }

    private record ConnectionEndpoints(ConnectionEndpoint source, ConnectionEndpoint target) {
    }

    private record ConnectionEndpoint(String nodeId, String port) {
    }

}
