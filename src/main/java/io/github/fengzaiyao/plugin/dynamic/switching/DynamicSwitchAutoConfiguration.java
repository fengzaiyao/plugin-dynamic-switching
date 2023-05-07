package io.github.fengzaiyao.plugin.dynamic.switching;

import io.github.fengzaiyao.plugin.dynamic.switching.register.AnnotationBPPRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AnnotationBPPRegistrar.class)
public class DynamicSwitchAutoConfiguration {
}
