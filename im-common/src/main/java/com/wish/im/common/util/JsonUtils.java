package com.wish.im.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationException;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 类型转换工具类
 * 时间格式 yyyy-MM-dd HH:mm:ss
 *
 * @author shy
 */
public class JsonUtils {
    private static final ObjectMapper MAPPER;

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    static {
        MAPPER = new ObjectMapper();
        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(PATTERN)));
        timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(PATTERN)));
        SimpleDateFormat fmt = new SimpleDateFormat(PATTERN);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setDateFormat(fmt)
                .registerModule(timeModule).registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
    }

    /**
     * 带泛型的对象转换
     *
     * @param fromValue      待转换对象
     * @param toValueTypeRef 转换后的对象类型
     * @param <T>            泛型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        return MAPPER.convertValue(fromValue, toValueTypeRef);
    }

    /**
     * 对象转换
     *
     * @param fromValue   待转换对象
     * @param toValueType 转换后的对象类型
     * @param <T>         返回类型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return MAPPER.convertValue(fromValue, toValueType);
    }

    /**
     * 序列化对象
     *
     * @param object 待序列化
     * @return json
     */
    public static String serialize(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 反序列化
     *
     * @param string json
     * @param target object
     * @param <T>    object对象类型
     * @return 反序列化对象
     */
    public static <T> T deserialize(String string, Class<T> target) {
        try {
            return MAPPER.readValue(string, target);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 反序列化
     *
     * @param string         json
     * @param toValueTypeRef object
     * @param <T>            object对象类型
     * @return 反序列化对象
     */
    public static <T> T deserialize(String string, TypeReference<T> toValueTypeRef) {
        try {
            return MAPPER.readValue(string, toValueTypeRef);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }


    /**
     * @param source can be {@literal null}.
     * @param type   must not be {@literal null}.
     * @return {@literal null} for empty source.
     * @throws SerializationException
     */
    @Nullable
    public static <T> T deserialize(@Nullable byte[] source, Class<T> type) throws SerializationException {
        if (ArrayUtils.isEmpty(source)) {
            return null;
        }
        try {
            return MAPPER.readValue(source, type);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * @param source         can be {@literal null}.
     * @param toValueTypeRef must not be {@literal null}.
     * @return {@literal null} for empty source.
     * @throws SerializationException
     */
    @Nullable
    public static <T> T deserialize(@Nullable byte[] source, TypeReference<T> toValueTypeRef) throws SerializationException {
        if (ArrayUtils.isEmpty(source)) {
            return null;
        }
        try {
            return MAPPER.readValue(source, toValueTypeRef);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    public static byte[] serializeAsBytes(@Nullable Object source) throws SerializationException {
        if (source == null) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        try {
            return MAPPER.writeValueAsBytes(source);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Could not write JSON: " + e.getMessage(), e);
        }
    }

    /**
     * 可以为spring boot提供统一序列化工具
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}