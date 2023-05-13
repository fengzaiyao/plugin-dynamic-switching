# 前置说明
做需求时，有时候会遇到多数据源问题。例如我们可以使用 mybatis-plus 创建出多个数据源，然后使用 @DS 注解去标记 Service 层的服务，
但是当遇到需要根据外界某个参数去选择对应的 Service 时就会写很多 if else 判断。为了解决这个问题，这个 jar 包就出现了。
# 使用步骤
1、引入 maven 依赖
```xml
<dependency>
    <groupId>io.github.fengzaiyao</groupId>
    <artifactId>plugin-dynamic-switching</artifactId>
    <version>1.0.4</version>
</dependency>
```

2、为了更好演示效果，举个例子，下面是业务的 Service，一个接口和两个实现类
```java
public interface ITestService {

    // 注意被 @DynamicParam 注解标记的入参，传入的参数将会被作为 "额外传递的参数"，去选择 service，
    // 只是会对，下面 第 4 点，第三种使用方式 有影响，其他两种使用方式不受影响
    String sayHello(@DynamicParam String name111, String name222);

    String eating(String food);
}
```
```java
@Service
public class TestServiceOne implements ITestService {
    @Override
    public String sayHello(String name111, String name222) {
        return "sayHello-111";
    }
    @Override
    public String eating(String food) {
        return "eating-111";
    }
}
```
```java
@Service
public class TestServiceTwo implements ITestService {
    @Override
    public String sayHello(String name111, String name222) {
        return "sayHello-222";
    }
    @Override
    public String eating(String food) {
        return "eating-222";
    }
}
```
3、实现 SwitchStrategy 接口，自定义选择策略，这将会影响你选择哪个 service 进行调用！！！
```java
// 记得使用 @Component 注入到 Spring 中
@Component
public class MySwitchStrategy implements SwitchStrategy {

    // list 是候选 service 列表、arg  是你额外传递的参数
    @Override
    public <T> T switchInstance(List<T> list, Object arg) {
        return list.get(0);
    }
}
```

4、示范如何使用
```java
@RestController
@RequestMapping(value = "/test")
public class TestController {
    
    // 使用 @DynamicSwitch 进行依赖注入, MySwitchStrategy.class 指定你要使用那种选择策略(上一步注入的自定义选择策略)
    @DynamicSwitch(MySwitchStrategy.class)
    private ITestService testService;

    @GetMapping("/t1")
    public void selectService() throws Exception {
        // 第一种使用方式：调用 switchInstance 选择出 service 实例，然后你就正常调用就行了
        String result1 = InvokeUtil.switchInstance(testService, "额外传递的参数").sayHello("水水", "果果");
        // 第二种使用方式：调用 invokeMethod 方法，直接调用 service 方法，sayHello 是方法名字
        String result2 = InvokeUtil.invokeMethod(testService, "sayHello", "额外传递的参数", "水水", "果果");
        // 第三种使用方式：直接调用，调用的方法的入参，有一个必须被 @DynamicParam 注解标记
        String result3 = testService.sayHello("额外传递的参数", "水水");
    }
}
```

