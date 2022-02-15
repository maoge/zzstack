package com.zzstack.paas.underlying.metasvr.bean;

import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Objects;

public abstract class BeanMapper {

    public static Object getFixDataAsObject(Map<String, Object> mapper, String fixHeader) {
        if (mapper == null)
            return null;

        if (fixHeader == null || fixHeader.length() == 0)
            return null;

        Object o = mapper.get(fixHeader);
        return o;
    }

    public static String getFixDataAsString(Map<String, Object> mapper, String fixHeader) {
        if (mapper == null)
            return "";

        if (fixHeader == null || fixHeader.isEmpty())
            return "";

        Object obj = mapper.get(fixHeader);
        String value = obj instanceof String ? (String) obj : String.valueOf(obj);
        return value != null ? value : "";
    }

    public static int getFixDataAsInt(Map<String, Object> mapper, String fixHeader) {
        if (mapper == null)
            return 0;

        if (fixHeader == null || fixHeader.isEmpty())
            return 0;

        Object obj = mapper.get(fixHeader);
        if (Objects.isNull(obj)) {
            return 0;
        }
        return obj instanceof Integer ? (Integer) obj : Integer.valueOf(String.valueOf(obj));
    }

    public static long getFixDataAsLong(Map<String, Object> mapper, String fixHeader) {
        if (mapper == null)
            return 0L;

        if (fixHeader == null || fixHeader.isEmpty())
            return 0L;

        Object obj = mapper.get(fixHeader);
        if (Objects.isNull(obj)) {
            return 0L;
        }
        return obj instanceof Long ? (Long) obj : Long.valueOf(String.valueOf(obj));
    }

    public static float getFixDataAsFloat(Map<String, Object> mapper, String fixHeader) {
        if (mapper == null)
            return 0;

        if (fixHeader == null || fixHeader.isEmpty())
            return 0;

        Object obj = mapper.get(fixHeader);
        if (Objects.isNull(obj)) {
            return 0f;
        }
        return obj instanceof Float ? (Float) obj : Float.valueOf(String.valueOf(obj));
    }

    public static double getFixDataAsDouble(Map<String, Object> mapper, String fixHeader) {
        if (mapper == null)
            return 0;

        if (fixHeader == null || fixHeader.isEmpty())
            return 0;

        Object obj = mapper.get(fixHeader);
        if (Objects.isNull(obj)) {
            return 0d;
        }
        return obj instanceof Double ? (Double) obj : Double.valueOf(String.valueOf(obj));
    }


    public static String toJsonString(Object obj) {
        return JsonObject.mapFrom(obj).toString();
    }

}
