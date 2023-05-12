package io.github.fengzaiyao.plugin.dynamic.switching.processor;

import io.github.fengzaiyao.plugin.dynamic.switching.constant.Constant;
import io.github.fengzaiyao.plugin.dynamic.switching.core.DynamicParam;
import io.github.fengzaiyao.plugin.dynamic.switching.core.DynamicSwitch;
import io.github.fengzaiyao.plugin.dynamic.switching.core.SwitchStrategy;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtConstructor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicSwitchInjectBPP extends BaseAnnotationInjectBPP {

    public DynamicSwitchInjectBPP() {
        super(DynamicSwitch.class);
    }

    private Map<String, Class<?>> CT_CLASS_CACHE = new ConcurrentHashMap<>();

    @Override
    protected Object getInjectedObject(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) throws Throwable {
        // 1、获取候选实例对象
        List<Object> candidates = new ArrayList<>();
        for (String name : context.getBeanNamesForType(injectedType)) {
            Object candidate = context.getBean(name, injectedType);
            candidates.add(candidate);
        }
        // 2、获取对应策略对象
        Class<? extends SwitchStrategy> strategy = attributes.getClass("value");
        SwitchStrategy strategyInstance = context.getBean(strategy);
        // 3、生成代理对象
        String instanceClazzName = injectedType.getCanonicalName() + "$PluginDynamicSwitch";
        if (CT_CLASS_CACHE.containsKey(instanceClazzName)) {
            Class<?> clazz = CT_CLASS_CACHE.get(instanceClazzName);
            Constructor<?> constructor = clazz.getConstructor(SwitchStrategy.class, List.class);
            return constructor.newInstance(strategyInstance, candidates);
        }
        ClassPool pool = ClassPool.getDefault();
        // 3.1、设置接口
        CtClass clazz = pool.makeClass(instanceClazzName);
        clazz.setInterfaces(new CtClass[]{pool.get(injectedType.getCanonicalName())});
        // 3.2、设置属性
        CtField ctField_1 = new CtField(pool.get(SwitchStrategy.class.getCanonicalName()), Constant.FIELD_NAME_STRATEGY, clazz);
        CtField ctField_2 = new CtField(pool.get(List.class.getCanonicalName()), Constant.FIELD_NAME_CANDIDATES, clazz);
        ctField_1.setModifiers(Modifier.PRIVATE);
        ctField_2.setModifiers(Modifier.PRIVATE);
        clazz.addField(ctField_1);
        clazz.addField(ctField_2);
        // 3.3、设置方法
        // 设置 switchInstance 方法
        CtClass[] ctParam1 = {pool.get(Object.class.getCanonicalName())};
        CtMethod ctMethod1 = new CtMethod(pool.get(injectedType.getCanonicalName()), Constant.METHOD_NAME_SWITCH_INSTANCE, ctParam1, clazz);
        ctMethod1.setModifiers(Modifier.PUBLIC);
        ctMethod1.setBody("{return $0." + Constant.FIELD_NAME_STRATEGY + "." + Constant.METHOD_INTERFACE_SWITCH_INSTANCE + "($0." + Constant.FIELD_NAME_CANDIDATES + ", $1);}");
        clazz.addMethod(ctMethod1);
        // 设置 接口方法 and 拓展方法
        ReflectionUtils.doWithMethods(injectedType, method -> {
            try {
                // ========================================== 接口方法 =================================================
                // 找到被 @DynamicParam 注解标记的入参位置 ps:只找第一个
                StringBuilder params = new StringBuilder();
                int paramIndex = -1;
                Parameter[] parameters = method.getParameters();
                for (int i = 1; i <= parameters.length; i++) {
                    DynamicParam annotation = parameters[i - 1].getAnnotation(DynamicParam.class);
                    if (paramIndex == -1 && !Objects.isNull(annotation)) {
                        paramIndex = i;
                    }
                    params.append("$").append(i).append(",");
                }
                if (params.length() > 0) {
                    params.deleteCharAt(params.length() - 1);
                }
                // 构建方法 body
                StringBuilder body = new StringBuilder();
                body.append("{").append("\n");
                if (paramIndex != -1) {
                    body.append("java.lang.Object instance = $0.").append(Constant.FIELD_NAME_STRATEGY).append(".").append(Constant.METHOD_INTERFACE_SWITCH_INSTANCE).append("($0.").append(Constant.FIELD_NAME_CANDIDATES).append(", $").append(paramIndex).append(");").append("\n");
                    String str = "((" + injectedType.getCanonicalName() + ") instance)." + method.getName() + "(" + params.toString() + ");" + "\n";
                    if (method.getReturnType() != void.class) {
                        str = "return " + str;
                    }
                    body.append(str);
                } else {
                    body.append("throw new RuntimeException(\"接口被 @DynamicSwitch 注解标记并且方法中没有带有 @DynamicParam 注解不能直接调用,请使用 InvokeUtil 调用方法\");");
                }
                body.append("}");
                // 构建入参类型
                Class<?>[] paramTypesClazz = method.getParameterTypes();
                CtClass[] paramTypes = new CtClass[paramTypesClazz.length];
                for (int i = 0; i < paramTypesClazz.length; i++) {
                    paramTypes[i] = pool.get(paramTypesClazz[i].getCanonicalName());
                }
                // 构建返回类型
                CtClass retType = pool.get(method.getReturnType().getCanonicalName());
                // 创建整个方法
                CtMethod ctMethod = new CtMethod(retType, method.getName(), paramTypes, clazz);
                ctMethod.setModifiers(Modifier.PUBLIC);
                ctMethod.setBody(body.toString());
                clazz.addMethod(ctMethod);

                // ========================================== 拓展方法 =================================================

                // 构建入参类型
                Class<?>[] paramTypesClazz0 = method.getParameterTypes();
                CtClass[] paramTypes0 = new CtClass[paramTypesClazz0.length + 1];
                paramTypes0[0] = pool.get(Object.class.getCanonicalName());
                StringBuilder params0 = new StringBuilder();
                for (int i = 0; i < paramTypesClazz0.length; i++) {
                    paramTypes0[i + 1] = pool.get(paramTypesClazz0[i].getCanonicalName());
                    params0.append("$").append(i + 2).append(",");
                }
                if (params0.length() > 0) {
                    params0.deleteCharAt(params0.length() - 1);
                }
                // 构建方法 body
                StringBuilder body0 = new StringBuilder();
                body0.append("{").append("\n");
                body0.append("java.lang.Object instance = $0.").append(Constant.FIELD_NAME_STRATEGY).append(".").append(Constant.METHOD_INTERFACE_SWITCH_INSTANCE).append("($0.").append(Constant.FIELD_NAME_CANDIDATES).append(", $1);").append("\n");
                String str0 = "((" + injectedType.getCanonicalName() + ") instance)." + method.getName() + "(" + params0.toString() + ");" + "\n";
                if (method.getReturnType() != void.class) {
                    str0 = "return " + str0;
                }
                body0.append(str0);
                body0.append("}");
                // 创建整个方法
                CtClass retType0 = pool.get(method.getReturnType().getCanonicalName());
                CtMethod ctMethod0 = new CtMethod(retType0, Constant.METHOD_NAME_PREFIX + method.getName(), paramTypes0, clazz);
                ctMethod0.setModifiers(Modifier.PUBLIC);
                ctMethod0.setBody(body0.toString());
                clazz.addMethod(ctMethod0);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        // 4、构造函数
        CtClass clazz1 = pool.get(SwitchStrategy.class.getCanonicalName());
        CtClass clazz2 = pool.get(List.class.getCanonicalName());
        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{clazz1, clazz2}, clazz);
        ctConstructor.setModifiers(Modifier.PUBLIC);
        ctConstructor.setBody("{$0." + Constant.FIELD_NAME_STRATEGY + " = $1;$0." + Constant.FIELD_NAME_CANDIDATES + " = $2;}");
        clazz.addConstructor(ctConstructor);
        // 5、生成.class文件(开发时可能需要用到)
        if (attributes.getBoolean("generateFile")) {
            URL resource = Thread.currentThread().getContextClassLoader().getResource("");
            if (resource != null) {
                clazz.writeFile(resource.toURI().getPath());
            }
        }
        Class<?> finalClazz = clazz.toClass();
        // 加入缓存,避免下次重复该类
        CT_CLASS_CACHE.put(instanceClazzName, finalClazz);
        // 6、创建对象
        Constructor<?> constructor = finalClazz.getConstructor(SwitchStrategy.class, List.class);
        return constructor.newInstance(strategyInstance, candidates);
    }
}