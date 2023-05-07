package io.github.fengzaiyao.plugin.dynamic.switching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;

@Resource
@SpringBootApplication(scanBasePackages = "io.github.fengzaiyao.plugin")
public class PluginDynamicSwitchingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PluginDynamicSwitchingApplication.class, args);
    }

}
