package io.github.fengzaiyao.plugin.dynamic.switching.test.service;

import org.springframework.stereotype.Service;

@Service
public class TestService222 implements ITestService {

    @Override
    public String sayHello(Object arg, String name) {
        return name + arg;
    }
}
