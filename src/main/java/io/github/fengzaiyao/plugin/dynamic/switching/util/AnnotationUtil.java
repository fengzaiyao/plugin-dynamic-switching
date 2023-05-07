package io.github.fengzaiyao.plugin.dynamic.switching.util;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.ObjectUtils.containsElement;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.StringUtils.trimWhitespace;

public class AnnotationUtil {

    /**
     * 将注解上的信息转化为Map对象
     *
     * @param target         目标对象
     * @param annotationType 注解Class
     */
    public static AnnotationAttributes getMergedAttributes(Object target, Class<? extends Annotation> annotationType) {
        // 可能是cglib生成的子类,如果是则返回原始类
        Class<?> orgClazz = target.getClass();
        Class<?> useClazz = ClassUtils.getUserClass(orgClazz);
        // 1.直接在当前类中寻找注解
        AnnotationAttributes annotation = getMergedAttributes(useClazz, annotationType);
        if (annotation != null) {
            return annotation;
        }
        // 2.从该类实现的接口中寻找,如果实现多个接口,接口中找到多个注解,只取第一个找到的
        for (Class<?> interfaceClazz : ClassUtils.getAllInterfacesForClassAsSet(useClazz)) {
            annotation = getMergedAttributes(interfaceClazz, annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        // 3.如果没有被代理,从父类一级级往上找
        if (!Proxy.isProxyClass(orgClazz)) {
            Class<?> currentClazz = orgClazz;
            while (currentClazz != Object.class) {
                AnnotationAttributes attributes = getMergedAttributes(currentClazz, annotationType);
                if (attributes != null) {
                    return attributes;
                }
                currentClazz = currentClazz.getSuperclass();
            }
        }
        return null;
    }

    public static AnnotationAttributes getMergedAttributes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return AnnotatedElementUtils.getMergedAnnotationAttributes(element, annotationType);
    }

    /**
     * 将注解上的信息转化为Map对象
     *
     * @param annotatedElement     字段
     * @param annotationType       注解类型
     * @param propertyResolver     解析占位符工具
     * @param ignoreDefaultValue   需要忽略的默认值
     * @param ignoreAttributeNames 需要忽略的默认属性名
     */
    public static AnnotationAttributes getMergedAttributes(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationType, PropertyResolver propertyResolver, boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        // 1.获取该属性上的注解信息
        Annotation annotation = AnnotatedElementUtils.getMergedAnnotation(annotatedElement, annotationType);
        // 2.将属性上的值包装成Map对象,最后再将Map转化为AnnotationAttributes
        return annotation == null ? null : AnnotationAttributes.fromMap(AnnotationUtil.getAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames));
    }

    /**
     * 获取注解上的属性值并封装为Map
     *
     * @param annotation           注解本身
     * @param propertyResolver     解析占位符工具
     * @param ignoreDefaultValue   需要忽略的默认值
     * @param ignoreAttributeNames 需要忽略的默认属性名
     */
    private static Map<String, Object> getAttributes(Annotation annotation, PropertyResolver propertyResolver, boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        if (annotation == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> attributes = getAnnotationAttributes(annotation);
        Map<String, Object> actualAttributes = new LinkedHashMap<>();
        // 遍历注解所有属性转为Map
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();
            // 忽略设置的默认值的属性
            if (ignoreDefaultValue && nullSafeEquals(attributeValue, AnnotationUtils.getDefaultValue(annotation, attributeName))) {
                continue;
            }
            // 忽略值为注解的属性
            if (attributeValue.getClass().isAnnotation()) {
                continue;
            }
            // 忽略注解数组
            if (attributeValue.getClass().isArray() && attributeValue.getClass().getComponentType().isAnnotation()) {
                continue;
            }
            actualAttributes.put(attributeName, attributeValue);
        }
        // 解析占位符问题
        return resolvePlaceholders(actualAttributes, propertyResolver, ignoreAttributeNames);
    }

    /**
     * 解析占位符
     */
    private static Map<String, Object> resolvePlaceholders(Map<String, Object> sourceAnnotationAttributes, PropertyResolver propertyResolver, String... ignoreAttributeNames) {
        // 1.map为null直接返回空集合
        if (isEmpty(sourceAnnotationAttributes)) {
            return Collections.emptyMap();
        }
        Map<String, Object> resolvedAnnotationAttributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : sourceAnnotationAttributes.entrySet()) {
            String attributeName = entry.getKey();
            // 2.忽略掉指定的属性名字
            if (containsElement(ignoreAttributeNames, attributeName)) {
                continue;
            }
            Object attributeValue = entry.getValue();
            // 3.字符串类型
            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(String.valueOf(attributeValue), propertyResolver);
                // 字符串数组类型
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
                attributeValue = values;
            }
            // 否则直接存放
            resolvedAnnotationAttributes.put(attributeName, attributeValue);
        }
        return Collections.unmodifiableMap(resolvedAnnotationAttributes);
    }

    /**
     * 解析占位符(使用 PropertyResolver 解决 "username={xxx}" 占位符问题)
     */
    private static String resolvePlaceholders(String attributeValue, PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }
}