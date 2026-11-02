package com.findit.util;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON utility for serialization and deserialization.
 */
public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Iterable<?>) {
            Iterable<?> list = (Iterable<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : array) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }

        // Generic object serialization via reflection
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (!first) sb.append(",");
                sb.append("\"").append(f.getName()).append("\":").append(toJson(val));
                first = false;
            } catch (Exception ignored) {
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Simple JSON parser into Map<String, Object> or List<Object>.
     */
    public static Object parse(String json) {
        if (json == null) return null;
        String trimmed = json.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.startsWith("{")) {
            return parseObject(trimmed);
        } else if (trimmed.startsWith("[")) {
            return parseArray(trimmed);
        } else if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        } else if ("true".equalsIgnoreCase(trimmed)) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(trimmed)) {
            return Boolean.FALSE;
        } else if ("null".equalsIgnoreCase(trimmed)) {
            return null;
        } else {
            try {
                if (trimmed.contains(".")) {
                    return Double.parseDouble(trimmed);
                } else {
                    return Long.parseLong(trimmed);
                }
            } catch (NumberFormatException e) {
                return trimmed;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);
        content = content.trim();
        if (content.isEmpty()) return map;

        int len = content.length();
        int i = 0;
        while (i < len) {
            // Find key
            while (i < len && (Character.isWhitespace(content.charAt(i)) || content.charAt(i) == ',')) i++;
            if (i >= len) break;
            if (content.charAt(i) != '"') break;
            i++; // skip open quote
            int keyStart = i;
            while (i < len && content.charAt(i) != '"') {
                if (content.charAt(i) == '\\') i++;
                i++;
            }
            String key = content.substring(keyStart, i);
            i++; // skip close quote

            // Find colon
            while (i < len && content.charAt(i) != ':') i++;
            i++; // skip colon

            // Find value
            while (i < len && Character.isWhitespace(content.charAt(i))) i++;
            if (i >= len) break;

            int valStart = i;
            if (content.charAt(i) == '"') {
                i++;
                while (i < len && content.charAt(i) != '"') {
                    if (content.charAt(i) == '\\') i++;
                    i++;
                }
                i++; // skip quote
                String val = content.substring(valStart + 1, i - 1);
                map.put(key, unescapeJson(val));
            } else if (content.charAt(i) == '{' || content.charAt(i) == '[') {
                char open = content.charAt(i);
                char close = open == '{' ? '}' : ']';
                int depth = 1;
                i++;
                boolean inString = false;
                while (i < len && depth > 0) {
                    char c = content.charAt(i);
                    if (c == '"' && content.charAt(i - 1) != '\\') inString = !inString;
                    else if (!inString) {
                        if (c == open) depth++;
                        else if (c == close) depth--;
                    }
                    i++;
                }
                String nested = content.substring(valStart, i);
                map.put(key, parse(nested));
            } else {
                while (i < len && content.charAt(i) != ',' && content.charAt(i) != '}') i++;
                String rawVal = content.substring(valStart, i).trim();
                map.put(key, parse(rawVal));
            }
        }
        return map;
    }

    private static List<Object> parseArray(String json) {
        List<Object> list = new ArrayList<>();
        String content = json.trim();
        if (content.startsWith("[")) content = content.substring(1);
        if (content.endsWith("]")) content = content.substring(0, content.length() - 1);
        content = content.trim();
        if (content.isEmpty()) return list;

        int len = content.length();
        int i = 0;
        while (i < len) {
            while (i < len && (Character.isWhitespace(content.charAt(i)) || content.charAt(i) == ',')) i++;
            if (i >= len) break;

            int valStart = i;
            if (content.charAt(i) == '"') {
                i++;
                while (i < len && content.charAt(i) != '"') {
                    if (content.charAt(i) == '\\') i++;
                    i++;
                }
                i++;
                String val = content.substring(valStart + 1, i - 1);
                list.add(unescapeJson(val));
            } else if (content.charAt(i) == '{' || content.charAt(i) == '[') {
                char open = content.charAt(i);
                char close = open == '{' ? '}' : ']';
                int depth = 1;
                i++;
                boolean inString = false;
                while (i < len && depth > 0) {
                    char c = content.charAt(i);
                    if (c == '"' && content.charAt(i - 1) != '\\') inString = !inString;
                    else if (!inString) {
                        if (c == open) depth++;
                        else if (c == close) depth--;
                    }
                    i++;
                }
                String nested = content.substring(valStart, i);
                list.add(parse(nested));
            } else {
                while (i < len && content.charAt(i) != ',' && content.charAt(i) != ']') i++;
                String rawVal = content.substring(valStart, i).trim();
                list.add(parse(rawVal));
            }
        }
        return list;
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}

