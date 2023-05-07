package io.github.fengzaiyao.plugin.dynamic.switching.test;

import io.github.fengzaiyao.plugin.dynamic.switching.core.DynamicSwitch;
import io.github.fengzaiyao.plugin.dynamic.switching.test.service.A;
import io.github.fengzaiyao.plugin.dynamic.switching.test.service.ITestService;
import io.github.fengzaiyao.plugin.dynamic.switching.util.ReflectionsUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;


@RestController
@RequestMapping("/test")
public class TestController {

    @DynamicSwitch(value = MySwitchStrategy.class, generateFile = true)
    private ITestService testService;

    @GetMapping("/t01")
    public Object test01() throws Exception {

        ReflectionsUtil.convert(ITestService::code);
        execute(testService, "sayHello", "hotel", 1);
        // execute(testService, "eating", "hotel", "哈哈哈");
        return null;
    }

    private <T> T execute(Object object, String methodName, Object data, Object... args) throws Exception {
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
}
