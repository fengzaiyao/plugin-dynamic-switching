package io.github.fengzaiyao.plugin.dynamic.switching.util;

import io.github.fengzaiyao.plugin.dynamic.switching.constant.Constant;

import java.lang.reflect.Method;

public class InvokeUtil {

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object object, String methodName, Object data, Object... args) throws Exception {
        Object[] paramValue = new Object[args.length + 1];
        Class<?>[] paramTypes = new Class[args.length + 1];
        paramTypes[0] = Object.class;
        paramValue[0] = data;
        for (int i = 0; i < args.length; i++) {
            paramTypes[i + 1] = args[i].getClass();
            paramValue[i + 1] = args[i];
        }
        Method method = object.getClass().getMethod(Constant.METHOD_NAME_PREFIX + methodName, paramTypes);
        return (T) method.invoke(object, paramValue);
    }

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
