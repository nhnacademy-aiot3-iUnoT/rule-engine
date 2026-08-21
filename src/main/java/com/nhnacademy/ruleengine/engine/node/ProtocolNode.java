package com.nhnacademy.ruleengine.engine.node;

import com.nhnacademy.ruleengine.engine.exception.ConnectionException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
// 외부 프로토콜 연결과 재연결 생명주기를 관리하는 노드다.
public abstract class ProtocolNode extends AbstractNode {

    private final Map<String, Object> config;

    public enum ConnectionState {DISCONNECTED, CONNECTING, CONNECTED, ERROR}

    private ConnectionState connectionState = ConnectionState.DISCONNECTED;

    private final Long reconnectIntervalMs;

    private ScheduledExecutorService reconnectScheduler;
    private int maxCounts = 10;
    private int currentCounts;


    protected ProtocolNode(String id, Map<String, Object> config) {
        super(id);

        this.config = config == null ? Map.of() : config;

        this.currentCounts = 0;

        this.reconnectIntervalMs = 5000L;

        if (this.config.containsKey("maxCounts")) {
            this.maxCounts = (int) getConfig("maxCounts");
        }
    }


    @Override
    public void initialize() {
        // 최초 연결 실패 시 예약된 재연결 절차로 전환한다.
        connectionState = ConnectionState.CONNECTING;
        // 재연결을 위한 별도의 스레드 생성
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            connect();
            connectionState = ConnectionState.CONNECTED;
            currentCounts = 0;
        } catch (Exception e) {
            connectionState = ConnectionState.ERROR;
            log.error("[{}] 연결 실패: {}", getId(), e.getMessage());
            reconnect();
        }
    }

    @Override
    public void shutdown() {
        // 재연결 작업을 먼저 멈춘 뒤 실제 연결을 해제한다.
        // 재연결 스레드 종료
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdownNow();
            reconnectScheduler = null;
        }
        try {
            disconnect();
        } catch (Exception e) {
            log.error("[{}] 연결 해제중 오류 발생 : {}", getId(), e.getMessage());
        } finally {
            connectionState = ConnectionState.DISCONNECTED;
        }
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    public void reconnect() {
        // 최대 시도 횟수 안에서 일정 간격으로 연결을 재시도한다.
        // 정해진 횟수 만큼 재연결 시도,이후 실패시 종료
        if (currentCounts >= maxCounts) {
            connectionState = ConnectionState.ERROR;
            log.info("시도횟수 초과 {}", currentCounts);
            return;
        }

        currentCounts++;

        reconnectScheduler.schedule(() -> {
            try {
                connect();
                connectionState = ConnectionState.CONNECTED;
                currentCounts = 0;
                log.info("[{}] 재연결 시도 성공", getId());
            } catch (Exception e) {
                log.warn("[{}] 재연결 시도 {} 번째 실패: {}", getId(), currentCounts, e.getMessage());
                reconnect();
            }
        }, reconnectIntervalMs, TimeUnit.MILLISECONDS);
    }

    public Object getConfig(String key) {
        return config.get(key);
    }


    protected abstract void connect() throws ConnectionException;

    protected abstract void disconnect();
}
