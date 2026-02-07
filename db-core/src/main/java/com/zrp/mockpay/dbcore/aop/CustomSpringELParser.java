package com.zrp.mockpay.dbcore.aop;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpEL(Spring Expression Language)을 파싱하여 파라미터 값을 추출하는 유틸리티
 */
public class CustomSpringELParser {

    private CustomSpringELParser() {
        // Utility Class는 인스턴스화 방지
    }

    public static Object getDynamicValue(String[] parameterNames, Object[] args, String key) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 파라미터 이름과 값을 매핑 (예: request -> PaymentRequest 객체)
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // SpEL 파싱 (예: "#request.memberId" -> 1L)
        return parser.parseExpression(key).getValue(context, Object.class);
    }
}