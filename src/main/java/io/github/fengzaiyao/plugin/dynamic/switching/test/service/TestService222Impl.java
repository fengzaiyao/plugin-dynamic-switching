package io.github.fengzaiyao.plugin.dynamic.switching.test.service;

import org.springframework.stereotype.Service;

@Service
public class TestService222Impl implements ITestService {

    @Override
    public void sayHello(Integer num) {
        System.out.println("sayHello:" + num);
    }

    @Override
    public String eating(String food) {
        return "苹果";
    }

    @Override
    public void run() {
        System.out.println("run-222");
    }

    @Override
    public String code() {
        return "22222";
    }
}
