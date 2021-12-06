package com.wish.im.server.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.wish.im.common.context.ImContext;
import com.wish.im.common.context.ImContextHolder;
import lombok.AllArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 公有字段自动填充
 *
 * @author shy
 * @date 2020/11/11
 */
@AllArgsConstructor
public class AutoFillHandler implements MetaObjectHandler {

    public AutoFillHandler() {
        this(false);
    }

    /**
     * 是否严格填充
     * 严格模式填充策略,默认有值不覆盖,如果提供的值为null也不填充
     * 非严格模式，自动强制填充
     */
    private final boolean isStrictFill;


    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        String userId = getUserId();
        this.strictInsertFill(metaObject, "creator", String.class, userId);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updatedBy", String.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        String userId = getUserId();
        this.strictUpdateFill(metaObject, "updatedBy", String.class, userId);
    }

    @Override
    public MetaObjectHandler strictFillStrategy(MetaObject metaObject, String fieldName, Supplier<?> fieldVal) {
        boolean needFill = !isStrictFill || metaObject.getValue(fieldName) == null;
        if (needFill) {
            Object obj = fieldVal.get();
            if (Objects.nonNull(obj)) {
                metaObject.setValue(fieldName, obj);
            }
        }
        return this;
    }

    public String getUserId() {
        ImContext currentContext = ImContextHolder.currentContext();
        return currentContext.getUid() == null ? currentContext.getServiceId() : currentContext.getUid();
    }
}
