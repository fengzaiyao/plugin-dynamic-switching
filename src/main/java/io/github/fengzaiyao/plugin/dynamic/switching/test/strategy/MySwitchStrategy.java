package io.github.fengzaiyao.plugin.dynamic.switching.test.strategy;

import io.github.fengzaiyao.plugin.dynamic.switching.core.SwitchStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MySwitchStrategy implements SwitchStrategy {

    @Override
    public <T> T switchInstance(List<T> list, Object arg) {
        return list.get(0);
    }
}
