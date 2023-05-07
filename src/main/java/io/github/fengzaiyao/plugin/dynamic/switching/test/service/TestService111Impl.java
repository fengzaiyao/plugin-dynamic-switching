package io.github.fengzaiyao.plugin.dynamic.switching.test.service;

import org.springframework.stereotype.Service;

@Service
public class TestService111Impl implements ITestService {

    @Override
    public void sayHello(Integer num) {
        System.out.println("sayHello:" + num);
        String eating = eating("666");
        System.out.println("eat:"+eating);
    }

    @Override
    public String eating(String food) {
        return "香蕉";
    }

    @Override
    public void run() {
        System.out.println("run-111");
    }

    @Override
    public String code() {
        return "11111";
    }
}
