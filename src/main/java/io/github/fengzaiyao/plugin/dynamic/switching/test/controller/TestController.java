//package io.github.fengzaiyao.plugin.dynamic.switching.test.controller;
//
//import io.github.fengzaiyao.plugin.dynamic.switching.core.DynamicSwitch;
//import io.github.fengzaiyao.plugin.dynamic.switching.test.service.ITestService;
//import io.github.fengzaiyao.plugin.dynamic.switching.test.strategy.MySwitchStrategy;
//import io.github.fengzaiyao.plugin.dynamic.switching.util.InvokeUtil;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping(value = "/test/v1")
//public class TestController {
//
//    @DynamicSwitch(value = MySwitchStrategy.class, generateFile = true)
//    private ITestService testService;
//
//    @GetMapping("/select")
//    public String selectService() throws Exception {
//        return InvokeUtil.switchInstance(testService, 1).sayHello("张三");
//    }
//}
