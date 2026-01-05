package com.lyra.agent.tool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
/**
 * 标注可被注册为工具的组件。
 * 通过名称与描述用于生成工具目录与匹配调用。
 */
public @interface Tool {
    String name();

    String description();
}