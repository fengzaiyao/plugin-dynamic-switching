# 前置说明
做需求时，有时候会遇到多数据源问题。例如我们可以使用 mybatis-plus 创建出多个数据源，然后使用 @DS 注解去标记 Service 层的服务，
但是当遇到需要根据外界某个参数去选择对应的 Service 时就会写很多 if else 判断。为了解决这个问题，这个 jar 包就出现了。
# 使用步骤
1、引入 maven 依赖
```xml
<dependency>
    <groupId>io.github.fengzaiyao</groupId>
    <artifactId>plugin-dynamic-switching</artifactId>
    <version>1.0.5</version>
</dependency>
```

2、为了演示动态切换服务，这里定义了一个Person类、一个接口、两个实现类

```java
public class Person {
    private String username;
    private String password;
    public String getUsername() {
        return username;
    }
}
```

```java
public interface ITestService {

    // @DynamicParam 标记了在调用方法时,哪个参数将会被作为 "传入的额外参数", 只会从左到头找到第一个标记为 @DynamicParam 的参数
    String sayHello(@DynamicParam String name, String word);
    
    // @DynamicParam 同时支持spEL表达式,会调用 Person 类的 getUsername() 返回值作为 "传入的额外参数"，ps：#person 必须和参数名字同名
    String sayHello(@DynamicParam("#person.getUsername()") Person person, String word);
}
```
```java
@Service
public class TestService111 implements ITestService {
    @Override
    public String sayHello(String name, String word) {
        return "service111：" + name + ":" + word;
    }
}
```
```java
@Service
public class TestService222 implements ITestService {
    @Override
    public String sayHello(String name, String word) {
        return "service222：" + name + ":" + word;
    }
}
```
3、实现 SwitchStrategy 接口，自定义选择策略，这将会影响你选择哪个 service 进行调用

```java
@Component
public class TestStrategy implements SwitchStrategy {
    
    // candidates  => 候选 service 列表、arg => 传入的额外参数, return 的值,为真正调用方法的实例对象
    @Override
    public <T> T switchInstance(List<T> candidates, Object arg) {
        // 为方便演示,这里写死,取候选列表第一个
        return candidates.get(0);
    }
}
```

4、示范如何使用
```java
@RestController
@RequestMapping(value = "/test")
public class TestController {

    // 使用 @DynamicSwitch 进行依赖注入，TestStrategy 为上面自定义的选择策略类
    @DynamicSwitch(TestStrategy.class)
    private ITestService testService;

    @GetMapping("/t1")
    public void t1() {
        
        // 第一种方式：调用 switchInstance 选择出 service 实例，然后你就正常调用就行了
        String result1 = InvokeUtil.switchInstance(testService, "额外传递的参数").sayHello("张三", "西瓜");
        // 第二种方式：直接调用，参数被 @DynamicParam 标记才能直接调用
        String result2 = testService.sayHello("额外传递的参数", "西瓜");
        
        // 打印结果 => "service111：张三:西瓜"
        System.out.println(result1); 
        // 打印结果 => "service111：额外传递的参数:西瓜"
        System.out.println(result2); 
    }
}
```

