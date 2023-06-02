package io.glutenproject.sql.compare;

import lombok.Data;

import java.util.Arrays;

@Data
public class ResultRow {

    private ResultRowField[] fields;

    @Data
    public static class ResultRowField {
        private String dataType;
        private Object value;

        @Override
        public String toString() {
            return dataType + ":" + value;
        }

        @Override
        public int hashCode() {
            return dataType.hashCode() + value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ResultRowField)) {
                return false;
            }
            ResultRowField other = (ResultRowField) obj;
            boolean dataTypeEqual = false;
            boolean valueEqual = false;
            if (dataType == null && other.dataType == null) {
                dataTypeEqual = true;
            } else if (dataType == null || other.dataType == null) {
                dataTypeEqual = false;
            } else {
                dataTypeEqual = dataType.equals(other.dataType);
            }

            if (value == null && other.value == null) {
                valueEqual = true;
            } else if (value == null || other.value == null) {
                valueEqual = false;
            } else {
                valueEqual = value.equals(other.value);
            }
            return dataTypeEqual && valueEqual;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                ResultRowField f = fields[i];
                if (f == null) {
                    sb.append("null");
                } else {
                    sb.append(f.toString());
                }
                if (i != fields.length - 1) {
                    sb.append(",");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResultRow)) {
            return false;
        }
        ResultRow other = (ResultRow) obj;
        if (fields == null && other.fields == null) {
            return true;
        } else if (fields == null || other.fields == null){
            return false;
        }
        if (fields.length != other.fields.length) {
            return false;
        }
        for (int i = 0; i < fields.length; i ++) {
            ResultRowField f1 = fields[i];
            ResultRowField f2 = other.fields[i];
            if (f1 == null && f2 == null) {
                continue;
            } else if (f1 == null || f2 == null) {
                return false;
            } else {
                if (f1.equals(f2)) {
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fields);
    }
}
