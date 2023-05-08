package io.github.fengzaiyao.plugin.dynamic.switching.util;

import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class InvokeUtil {

    public static <T> T invokeMethod(Object object, String methodName, Object data, Object... args) throws Exception {
        Object[] paramValue = new Object[args.length + 1];
        Class<?>[] paramTypes = new Class[args.length + 1];
        paramTypes[0] = Object.class;
        paramValue[0] = data;
        for (int i = 0; i < args.length; i++) {
            paramTypes[i + 1] = args[i].getClass();
            paramValue[i + 1] = args[i];
        }
        Method method = object.getClass().getMethod(methodName, paramTypes);
        return (T) method.invoke(object, paramValue);
    }

    public static <T> T switchInstance(Object object, Object data) throws Exception {
        Method method = object.getClass().getMethod("switchInstance", Object.class);
        return (T) method.invoke(object, data);
    }
}
