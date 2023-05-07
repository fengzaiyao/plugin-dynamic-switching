package io.github.fengzaiyao.plugin.dynamic.switching.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicSwitch {

    Class<? extends SwitchStrategy> value();

    boolean generateFile() default false;
}
