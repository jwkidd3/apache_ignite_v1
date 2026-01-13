package com.example.ignite.solutions.lab13.cdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents a Debezium CDC event from Kafka
 *
 * Debezium event structure:
 * {
 *   "before": { ... },  // Previous state (null for inserts)
 *   "after": { ... },   // New state (null for deletes)
 *   "source": { ... },  // Metadata about the source
 *   "op": "c|u|d|r",    // Operation: c=create, u=update, d=delete, r=read (snapshot)
 *   "ts_ms": 123456789  // Timestamp
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDCEvent {

    public enum Operation {
        CREATE("c"),
        UPDATE("u"),
        DELETE("d"),
        READ("r");  // Snapshot read

        private final String code;

        Operation(String code) {
            this.code = code;
        }

        public static Operation fromCode(String code) {
            for (Operation op : values()) {
                if (op.code.equals(code)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operation code: " + code);
        }
    }

    @JsonProperty("before")
    private Map<String, Object> before;

    @JsonProperty("after")
    private Map<String, Object> after;

    @JsonProperty("source")
    private Map<String, Object> source;

    @JsonProperty("op")
    private String op;

    @JsonProperty("ts_ms")
    private Long timestamp;

    public CDCEvent() {}

    public Map<String, Object> getBefore() {
        return before;
    }

    public void setBefore(Map<String, Object> before) {
        this.before = before;
    }

    public Map<String, Object> getAfter() {
        return after;
    }

    public void setAfter(Map<String, Object> after) {
        this.after = after;
    }

    public Map<String, Object> getSource() {
        return source;
    }

    public void setSource(Map<String, Object> source) {
        this.source = source;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Operation getOperation() {
        return Operation.fromCode(op);
    }

    public String getTable() {
        if (source != null) {
            return (String) source.get("table");
        }
        return null;
    }

    public String getSchema() {
        if (source != null) {
            return (String) source.get("schema");
        }
        return null;
    }

    public boolean isInsert() {
        return "c".equals(op) || "r".equals(op);
    }

    public boolean isUpdate() {
        return "u".equals(op);
    }

    public boolean isDelete() {
        return "d".equals(op);
    }

    @Override
    public String toString() {
        return "CDCEvent{" +
                "table='" + getTable() + '\'' +
                ", op='" + op + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
