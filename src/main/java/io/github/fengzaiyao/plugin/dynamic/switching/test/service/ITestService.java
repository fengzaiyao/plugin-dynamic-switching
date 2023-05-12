package io.github.fengzaiyao.plugin.dynamic.switching.test.service;

import io.github.fengzaiyao.plugin.dynamic.switching.core.DynamicParam;

public interface ITestService {

    String sayHello(@DynamicParam Object arg, String name);
}
