package io.github.fengzaiyao.plugin.dynamic.switching.util;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ReflectionsUtil {

    public interface ConvertFunction<T, RS> extends Function<T, RS>, Serializable {
    }

    private static final Pattern GET_PATTERN = Pattern.compile("get[A-Z].*");
    private static final Pattern IS_PATTERN = Pattern.compile("is[A-Z].*");

    /**
     * 根据传入的getter方法或者is方法，获取对应的[字段名], 例如 getDelete => delete、isSuccess => success
     *
     * @param convertFunction 类的get方法或者is方法
     */
    public static <T, RS> String convert(ConvertFunction<T, RS> convertFunction) {
        try {
            Class<? extends Function> aClass = convertFunction.getClass();
            Method writeReplace = aClass.getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(convertFunction);
            String implMethodName = serializedLambda.getImplMethodName();
            System.out.println(implMethodName);
//            if (GET_PATTERN.matcher(implMethodName).matches()) {
//                implMethodName = implMethodName.substring(3);
//            } else if (IS_PATTERN.matcher(implMethodName).matches()) {
//                implMethodName = implMethodName.substring(2);
//            }
            return Introspector.decapitalize(implMethodName);
        } catch (Exception e) {
            return "";
        }
    }
}
