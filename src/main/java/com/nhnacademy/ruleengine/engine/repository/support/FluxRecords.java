package com.nhnacademy.ruleengine.engine.repository.support;

import com.influxdb.query.FluxRecord;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * FluxRecord에서 값을 꺼내는 규칙을 모은다.
 * <p>
 * 값이 없는 것과 형식이 틀린 것은 다르게 다뤄야 한다. 없는 값은 설정되지 않았다는 뜻이라 null이지만,
 * 숫자 자리에 숫자가 아닌 값이 들어온 것은 질의나 스키마가 잘못됐다는 뜻이라 즉시 드러나야 한다.
 */
public final class FluxRecords {

    private FluxRecords() {
    }

    public static String getString(
            FluxRecord record,
            String key
    ) {
        Object value = record.getValueByKey(key);

        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 지정한 열의 숫자를 읽는다. 열이 없으면 null이다.
     */
    public static Double getDouble(
            FluxRecord record,
            String key
    ) {
        Object value = record.getValueByKey(key);

        if (value == null) {
            return null;
        }

        return toDouble(value, key);
    }

    /**
     * 반드시 있어야 하는 열의 숫자를 읽는다.
     */
    public static double requireDouble(
            FluxRecord record,
            String key
    ) {
        Double value = getDouble(record, key);

        if (value == null) {
            throw new IllegalStateException(key + " 값이 없습니다.");
        }

        return value;
    }

    /**
     * 질의 결과의 기본 값(_value)을 숫자로 읽는다.
     */
    public static double requireValue(FluxRecord record) {
        return toDouble(record.getValue(), "_value");
    }

    /**
     * Flux의 열 목록 표기를 만든다. group과 sort가 같은 목록을 쓰는 곳이 많아 문자열로 한 번만 만든다.
     */
    public static String toColumns(String... columns) {
        return Arrays.stream(columns)
                .map(column -> "\"" + column + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static double toDouble(
            Object value,
            String key
    ) {
        if (!(value instanceof Number number)) {
            throw new IllegalStateException(
                    key + " 값이 숫자 형식이 아닙니다: " + value
            );
        }

        return number.doubleValue();
    }
}
