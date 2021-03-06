
package com.summer.log.annotation;

import com.summer.log.constant.Level;
import com.summer.log.serializer.LogSerializer;
import com.summer.log.serializer.ToStringSerializer;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 日志记录
 *
 * @author Tom
 * @version 1.0.0
 * @date 4/13/22
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Logging {

    /**
     * 日志名称
     *
     * @return
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 日志名称
     *
     * @return
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 序列化参数
     *
     * @return
     */
    Class<? extends LogSerializer> serializeArgsUsing() default ToStringSerializer.class;

    /**
     * 序列化返回值
     *
     * @return
     */
    Class<? extends LogSerializer> serializeReturnUsing() default ToStringSerializer.class;

    /**
     * 日志级别
     *
     * @return
     */
    Level level() default Level.INFO;

    /**
     * 单行日志最大长度
     * -1:不限制长度
     *
     * @return
     */
    int maxLength() default -1;

    /**
     * 异常日志
     *
     * @return
     */
    ThrowableLog[] throwableLog() default {};

}
