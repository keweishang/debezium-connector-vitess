/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.vitess;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.vitess.proto.Query;

/** The Vitess table column type */
public class VitessType {

    // name of the column type
    private final String name;
    // enum of column jdbc type
    private final int jdbcId;
    // permitted enum values
    private final List<String> enumValues;

    public VitessType(String name, int jdbcId) {
        this(name, jdbcId, null);
    }

    public VitessType(String name, int jdbcId, List<String> enumValues) {
        this.name = name;
        this.jdbcId = jdbcId;
        this.enumValues = enumValues;
    }

    public String getName() {
        return name;
    }

    public int getJdbcId() {
        return jdbcId;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public boolean isEnum() {
        return enumValues != null && enumValues.size() != 0;
    }

    @Override
    public String toString() {
        return "VitessType{" +
                "name='" + name + '\'' +
                ", jdbcId=" + jdbcId +
                ", enumValues=" + enumValues +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VitessType that = (VitessType) o;
        return jdbcId == that.jdbcId && name.equals(that.name) && Objects.equals(enumValues, that.enumValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, jdbcId, enumValues);
    }

    // Resolve JDBC type from vstream FIELD event
    public static VitessType resolve(Query.Field field) {
        String type = field.getType().name();
        switch (type) {
            case "INT8":
            case "UINT8":
            case "INT16":
                return new VitessType(type, Types.SMALLINT);
            case "UINT16":
            case "INT24":
            case "UINT24":
            case "INT32":
                return new VitessType(type, Types.INTEGER);
            case "ENUM":
                return new VitessType(type, Types.INTEGER, resolveEnumValues(field.getColumnType()));
            case "UINT32":
            case "INT64":
            case "UINT64":
                return new VitessType(type, Types.BIGINT);
            case "VARBINARY":
            case "BINARY":
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "JSON":
            case "DECIMAL":
            case "TIME":
            case "DATE":
            case "DATETIME":
            case "TIMESTAMP":
            case "YEAR":
            case "SET":
                return new VitessType(type, Types.VARCHAR);
            case "FLOAT32":
                return new VitessType(type, Types.FLOAT);
            case "FLOAT64":
                return new VitessType(type, Types.DOUBLE);
            default:
                return new VitessType(type, Types.OTHER);
        }
    }

    /**
     * Resolve the list of permitted Enum values from the Enum Definition
     * @param enumDefinition the Enum column definition from the MySQL table. E.g. "enum('m','l','xl')"
     * @return The list of permitted Enum values
     */
    private static List<String> resolveEnumValues(String enumDefinition) {
        List<String> enumValues = new ArrayList<>();
        if (enumDefinition == null || enumDefinition.length() == 0) {
            return enumValues;
        }

        StringBuilder sb = new StringBuilder();
        boolean startCollecting = false;
        char[] chars = enumDefinition.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\'') {
                if (chars[i + 1] != '\'') {
                    if (startCollecting) {
                        // end of the Enum value, add the Enum value to the result list
                        enumValues.add(sb.toString());
                        sb.setLength(0);
                    }
                    startCollecting = !startCollecting;
                }
                else {
                    sb.append("'");
                    // In MySQL, the single quote in the Enum definition "a'b" is escaped and becomes "a''b".
                    // Skip the second single-quote
                    i++;
                }
            }
            else if (startCollecting) {
                sb.append(chars[i]);
            }
        }
        return enumValues;
    }
}
