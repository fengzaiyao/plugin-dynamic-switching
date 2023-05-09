package io.github.fengzaiyao.plugin.dynamic.switching.core;

import java.util.List;

public interface SwitchStrategy {

    <T> T switchInstance(List<T> candidates, Object arg);
}
