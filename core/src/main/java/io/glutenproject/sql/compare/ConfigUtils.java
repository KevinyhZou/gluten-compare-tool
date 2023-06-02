package io.glutenproject.sql.compare;

import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ConfigUtils {

    private static Map<String, Object> convertConfigFields(Object config, Field[] fields, String configKeyPrefix) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
        for (Field f : fields) {
            f.setAccessible(true);
            Value annotation = f.getAnnotation(Value.class);
            if (annotation == null) {
                continue;
            }
            String annotationValue = annotation.value();
            annotationValue = annotationValue.replace("$", "")
                    .replace("{", "")
                    .replace("}", "")
                    .trim();
            Object fieldValue = f.get(config);
            map.put(configKeyPrefix + annotationValue, fieldValue);
        }
        return map;
    }

    public static Map<String, Object> convertConfigToMap(SessionConfig.GlutenConfig config, String glutenConfigKeyPrefix) throws IllegalAccessException {
        Field[] fields = SessionConfig.GlutenConfig.class.getDeclaredFields();
        return convertConfigFields(config, fields, glutenConfigKeyPrefix);
    }

    public static Map<String, Object> convertConfigToMap(SessionConfig.YarnConfig config, String yarnConfigKeyPrefix) throws IllegalAccessException {
        Field[] fields = SessionConfig.YarnConfig.class.getDeclaredFields();
        return convertConfigFields(config, fields, yarnConfigKeyPrefix);
    }

    public static Map<String, Object> convertConfigToMap(SessionConfig config, String hiveConfigKeyPrefix, String hiveVarKeyPrefix, boolean withGlutenConfig) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
        if (config.getGlutenConfig() != null && withGlutenConfig) {
            Map<String, Object> glutenConfigMap = convertConfigToMap(config.getGlutenConfig(), hiveVarKeyPrefix);
            map.putAll(glutenConfigMap);
        }
        if (config.getYarnConfig() != null) {
            Map<String, Object> glutenConfigMap = convertConfigToMap(config.getYarnConfig(), hiveVarKeyPrefix);
            map.putAll(glutenConfigMap);
        }
        Field[] fields = SessionConfig.class.getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            Value annotation = f.getAnnotation(Value.class);
            if (annotation != null) {
                String annotationValue = annotation.value();
                annotationValue = annotationValue.replace("$", "")
                        .replace("{", "")
                        .replace("}", "")
                        .trim();
                Object fieldValue = f.get(config);
                if (annotationValue.startsWith("hive.server2")) {
                    map.put(hiveConfigKeyPrefix + annotationValue, fieldValue);
                } else {
                    map.put(annotationValue, fieldValue);
                }
            }
        }
        return map;
    }

}
