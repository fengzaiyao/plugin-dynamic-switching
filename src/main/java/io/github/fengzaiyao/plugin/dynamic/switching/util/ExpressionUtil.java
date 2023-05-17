package io.github.fengzaiyao.plugin.dynamic.switching.util;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class ExpressionUtil {

    private static ExpressionParser parser = new SpelExpressionParser();

    public static Object getValue(Object target, String key, String expression) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable(key, target);
        return parser.parseExpression(expression).getValue(context);
    }
}
