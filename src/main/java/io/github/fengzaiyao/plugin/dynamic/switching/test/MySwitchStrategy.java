package io.github.fengzaiyao.plugin.dynamic.switching.test;

import io.github.fengzaiyao.plugin.dynamic.switching.core.SwitchStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class MySwitchStrategy implements SwitchStrategy {

    @Override
    public <T> T switchInstance(List<T> candidates, Object args) {
        if (!CollectionUtils.isEmpty(candidates) && candidates.size() == 1) {
            return candidates.get(0);
        }
        System.out.println("获取到的参数: " + args);
        return candidates.get(0);
    }
}
