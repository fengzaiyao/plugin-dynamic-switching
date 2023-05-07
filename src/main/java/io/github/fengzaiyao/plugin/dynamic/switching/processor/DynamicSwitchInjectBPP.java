package io.github.fengzaiyao.plugin.dynamic.switching.processor;

import io.github.fengzaiyao.plugin.dynamic.switching.core.DynamicSwitch;
import io.github.fengzaiyao.plugin.dynamic.switching.core.SwitchStrategy;
import javassist.*;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DynamicSwitchInjectBPP extends BaseAnnotationInjectBPP {

    public DynamicSwitchInjectBPP() {
        super(DynamicSwitch.class);
    }

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
        String instanceClazzName = injectedType.getCanonicalName() + "$DynamicSwitch";
        ClassPool pool = ClassPool.getDefault();
        // 1、设置接口
        CtClass clazz = pool.makeClass(instanceClazzName);
        clazz.setInterfaces(new CtClass[]{pool.get(injectedType.getCanonicalName())});
        // 2、设置属性
        CtField ctField_1 = new CtField(pool.get(SwitchStrategy.class.getCanonicalName()), "strategy", clazz);
        CtField ctField_2 = new CtField(pool.get(List.class.getCanonicalName()), "candidates", clazz);
        ctField_1.setModifiers(Modifier.PUBLIC);
        ctField_2.setModifiers(Modifier.PUBLIC);
        clazz.addField(ctField_1);
        clazz.addField(ctField_2);
        // 3、设置方法
        ReflectionUtils.doWithMethods(injectedType, method -> {
            try {
                // 3.1、设置额外方法
                Class<?>[] params = method.getParameterTypes();
                CtClass[] paramTypes = new CtClass[params.length + 1];
                paramTypes[0] = pool.get(Object.class.getCanonicalName());
                StringBuilder paramBuilder = new StringBuilder();
                for (int i = 0; i < params.length; i++) {
                    String canonicalName = params[i].getCanonicalName();
                    paramTypes[i + 1] = pool.get(canonicalName);
                    paramBuilder.append("$").append(i + 2).append(",");
                }
                if (paramBuilder.length() > 0) {
                    paramBuilder.deleteCharAt(paramBuilder.length() - 1);
                }
                StringBuilder total = new StringBuilder();
                total.append("{").append("\n");
                total.append("java.lang.Object instance = $0.strategy.switchInstance($0.candidates, $1);").append("\n");
                if (method.getReturnType() == Void.class || method.getReturnType() == void.class) {
                    total.append("((").append(injectedType.getCanonicalName()).append(") instance).").append(method.getName()).append("(").append(paramBuilder.toString()).append(");").append("\n");
                } else {
                    total.append("return ((").append(injectedType.getCanonicalName()).append(") instance).").append(method.getName()).append("(").append(paramBuilder.toString()).append(");").append("\n");
                }
                total.append("}");
                CtClass retType = pool.get(method.getReturnType().getCanonicalName());
                CtMethod ctMethod = new CtMethod(retType, method.getName(), paramTypes, clazz);
                ctMethod.setModifiers(Modifier.PUBLIC);
                ctMethod.setBody(total.toString());
                clazz.addMethod(ctMethod);
                // 3.2、设置接口方法
                // 方法入参
                Class<?>[] params0 = method.getParameterTypes();
                CtClass[] paramTypes0 = new CtClass[params0.length];
                for (int i = 0; i < params0.length; i++) {
                    paramTypes0[i] = pool.get(params0[i].getCanonicalName());
                }
                // 返回类型
                CtClass retType0 = pool.get(method.getReturnType().getCanonicalName());
                CtMethod ctMethod0 = new CtMethod(retType0, method.getName(), paramTypes0, clazz);
                // 访问权限
                ctMethod0.setModifiers(Modifier.PUBLIC);
                // 方法体
                ctMethod0.setBody("throw new RuntimeException(\"不能直接调用此方法\");");
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
        ctConstructor.setBody("{$0.strategy = $1;$0.candidates = $2;}");
        clazz.addConstructor(ctConstructor);
        // 5、生成代理文件(开发时可能需要用到)
        if (attributes.getBoolean("generateFile")) {
            clazz.writeFile(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).toURI().getPath());
        }
        // 6、创建对象
        Constructor<?> constructor = clazz.toClass().getConstructor(SwitchStrategy.class, List.class);
        return constructor.newInstance(strategyInstance, candidates);
    }
}