package io.github.fengzaiyao.plugin.dynamic.switching.util;

import io.github.fengzaiyao.plugin.dynamic.switching.constant.Constant;

import java.lang.reflect.Method;

public class InvokeUtil {

    @SuppressWarnings("unchecked")
    public static <T> T switchInstanceWithEx(Object object, Object data) throws Exception {
        Method method = object.getClass().getMethod(Constant.METHOD_NAME_SWITCH_INSTANCE, Object.class);
        return (T) method.invoke(object, data);
    }

    public static <T> T switchInstance(T object, Object data) {
        T instance = null;
        try {
            instance = switchInstanceWithEx(object, data);
        } catch (Exception ignored) {
        }
        return instance;
    }
}
