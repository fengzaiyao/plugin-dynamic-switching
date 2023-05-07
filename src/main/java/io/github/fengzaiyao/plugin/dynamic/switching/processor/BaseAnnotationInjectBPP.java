package io.github.fengzaiyao.plugin.dynamic.switching.processor;

import io.github.fengzaiyao.plugin.dynamic.switching.util.AnnotationUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class BaseAnnotationInjectBPP implements MergedBeanDefinitionPostProcessor, InstantiationAwareBeanPostProcessor, EnvironmentAware, ApplicationContextAware, BeanClassLoaderAware, BeanFactoryAware {

    protected transient ClassLoader classLoader;

    protected transient Environment environment;

    protected transient BeanFactory beanFactory;

    protected transient ApplicationContext context;

    protected volatile Class<? extends Annotation> annotationType;

    private final ConcurrentMap<String, AnnotationInjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>();

    protected BaseAnnotationInjectBPP(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * 1、准备好要处理的元数据信息
     *
     * @param beanDefinition Bean基本定义
     * @param beanType       Bean类型
     * @param beanName       Bean名字
     */
    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        AnnotationInjectionMetadata injectionMetadata = findInjectionMetadata(null, beanType, beanName, null);
        injectionMetadata.checkConfigMembers(beanDefinition);
    }

    /**
     * 2.真正处理元数据信息,回调相关注入方法
     * <p>
     * {@link AnnotationInjectionFieldElement#inject(Object, String, PropertyValues)}
     * {@link AnnotationInjectionMethodElement#inject(Object, String, PropertyValues)}
     *
     * @param pvs      属性元数据
     * @param bean     Bean对象
     * @param beanName Bean名字
     */
    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        try {
            AnnotationInjectionMetadata injectionMetadata = findInjectionMetadata(bean, bean.getClass(), beanName, pvs);
            injectionMetadata.inject(bean, beanName, pvs);
        } catch (Throwable throwable) {
            throw new IllegalStateException(BaseAnnotationInjectBPP.class.getName() + "inject class=" + bean.getClass() + " is fail, message = " + throwable.getMessage(), throwable);
        }
        return pvs;
    }

    /**
     * 3、属性注入、方法注入的包裹类
     */
    private class AnnotationInjectionMetadata extends InjectionMetadata {

        private final Collection<AnnotationInjectionFieldElement> fieldElements;

        private final Collection<AnnotationInjectionMethodElement> methodElements;

        public AnnotationInjectionMetadata(Class<?> targetClass, Collection<AnnotationInjectionFieldElement> fieldElements, Collection<AnnotationInjectionMethodElement> methodElements) {
            super(targetClass, combine(fieldElements, methodElements));
            this.fieldElements = fieldElements;
            this.methodElements = methodElements;
        }

        public Collection<AnnotationInjectionFieldElement> getFieldElements() {
            return fieldElements;
        }

        public Collection<AnnotationInjectionMethodElement> getMethodElements() {
            return methodElements;
        }
    }

    /**
     * 属性注入包裹类
     */
    private class AnnotationInjectionFieldElement extends InjectionMetadata.InjectedElement {

        private final Field field;

        private final AnnotationAttributes attributes;

        protected AnnotationInjectionFieldElement(Field field, AnnotationAttributes attributes) {
            super(field, null);
            this.field = field;
            this.attributes = attributes;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            // 生成自动注入的对象 and 反射设置值
            Object injectedObject = getInjectedObject(attributes, bean, beanName, field.getType(), this);
            ReflectionUtils.makeAccessible(field);
            field.set(bean, injectedObject);
        }
    }

    /**
     * 方法注入包裹类
     */
    private class AnnotationInjectionMethodElement extends InjectionMetadata.InjectedElement {

        private final Method method;

        private final AnnotationAttributes attributes;

        protected AnnotationInjectionMethodElement(Method method, AnnotationAttributes attributes, PropertyDescriptor pd) {
            super(method, pd);
            this.method = method;
            this.attributes = attributes;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            // 生成自动注入的对象 and 反射设置值
            Object injectedObject = getInjectedObject(attributes, bean, beanName, pd.getPropertyType(), this);
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean, injectedObject);
        }
    }

    /**
     * 4、留给子类实现,获取要注入的 Bean
     *
     * @param attributes      注解信息
     * @param bean            Bean对象
     * @param beanName        Bean名字
     * @param injectedType    要注入的类型
     * @param injectedElement 所在类的元数据
     */
    protected abstract Object getInjectedObject(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) throws Throwable;

    /**
     * 查询注入的元数据信息
     */
    private AnnotationInjectionMetadata findInjectionMetadata(Object bean, Class<?> beanType, String beanName, PropertyValues pvs) {
        if (beanType == null) {
            throw new IllegalArgumentException("beanType is null");
        }
        String clazzName = beanType.getCanonicalName();
        AnnotationInjectionMetadata injectionMetadata = injectionMetadataCache.get(clazzName);
        if (InjectionMetadata.needsRefresh(injectionMetadata, beanType)) {
            synchronized (injectionMetadataCache) {
                injectionMetadata = injectionMetadataCache.get(clazzName);
                if (InjectionMetadata.needsRefresh(injectionMetadata, beanType)) {
                    injectionMetadata = buildInjectionMetadata(beanType);
                    // 没有搜索到有任何标记有该注解的地方,则忽略
                    if (!(CollectionUtils.isEmpty(injectionMetadata.getFieldElements()) && CollectionUtils.isEmpty(injectionMetadata.getMethodElements()))) {
                        injectionMetadataCache.put(clazzName, injectionMetadata);
                    }
                }
            }
        }
        return injectionMetadata;
    }

    private AnnotationInjectionMetadata buildInjectionMetadata(Class<?> beanType) {
        Collection<AnnotationInjectionFieldElement> fieldElements = buildInjectionFieldElements(beanType);
        Collection<AnnotationInjectionMethodElement> methodElements = buildInjectionMethodElements(beanType);
        return new AnnotationInjectionMetadata(beanType, fieldElements, methodElements);
    }

    /**
     * 构建要注入的属性信息
     */
    private Collection<AnnotationInjectionFieldElement> buildInjectionFieldElements(Class<?> beanType) {
        LinkedList<AnnotationInjectionFieldElement> fieldElements = new LinkedList<>();
        ReflectionUtils.doWithFields(beanType, field -> {
            if (Modifier.isStatic(field.getModifiers())) {
                return;
            }
            AnnotationAttributes attributes = AnnotationUtil.getMergedAttributes(field, annotationType, environment, false);
            if (attributes != null) {
                AnnotationInjectionFieldElement element = new AnnotationInjectionFieldElement(field, attributes);
                fieldElements.add(element);
            }
        });
        return fieldElements;
    }

    /**
     * 构建要注入的方法信息 TODO 暂不支持
     */
    private Collection<AnnotationInjectionMethodElement> buildInjectionMethodElements(Class<?> beanType) {
        return Collections.emptyList();
    }

    private <T> Collection<T> combine(Collection<? extends T>... elements) {
        List<T> allElements = new ArrayList<>();
        for (Collection<? extends T> e : elements) {
            allElements.addAll(e);
        }
        return allElements;
    }

}