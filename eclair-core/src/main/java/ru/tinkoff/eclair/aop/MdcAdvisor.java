/*
 * Copyright 2018 Tinkoff Bank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.tinkoff.eclair.aop;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.BridgeMethodResolver;
import ru.tinkoff.eclair.core.ExpressionEvaluator;
import ru.tinkoff.eclair.definition.MethodMdc;
import ru.tinkoff.eclair.definition.ParameterMdc;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * @author Viacheslav Klapatniuk
 */
final class MdcAdvisor extends StaticMethodMatcherPointcutAdvisor implements MethodInterceptor {

    private final ExpressionEvaluator expressionEvaluator;
    private final Map<Method, MethodMdc> methodMdcs;

    private MdcAdvisor(ExpressionEvaluator expressionEvaluator,
                       List<MethodMdc> methodMdcs) {
        this.expressionEvaluator = expressionEvaluator;
        this.methodMdcs = methodMdcs.stream().collect(toMap(MethodMdc::getMethod, identity()));
    }

    static MdcAdvisor newInstance(ExpressionEvaluator expressionEvaluator,
                                  List<MethodMdc> methodMdcs) {
        return methodMdcs.isEmpty() ? null : new MdcAdvisor(expressionEvaluator, methodMdcs);
    }

    @Override
    public Advice getAdvice() {
        return this;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return methodMdcs.containsKey(method) || methodMdcs.containsKey(BridgeMethodResolver.findBridgedMethod(method));
    }

    /**
     * Despite the fact that this method is not being used.
     */
    @Override
    public boolean isPerInstance() {
        return false;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Set<String> localKeys = new HashSet<>();
        try (LocalKeysHolder ignored = new LocalKeysHolder(localKeys)) {
            MethodMdc methodMdc = methodMdcs.get(invocation.getMethod());
            processMethodDefinitions(invocation, methodMdc, localKeys);
            processParameterDefinitions(invocation, methodMdc, localKeys);
            return invocation.proceed();
        }
    }

    private void processMethodDefinitions(MethodInvocation invocation, MethodMdc methodMdc, Set<String> localKeys) {
        for (ParameterMdc definition : methodMdc.getMethodDefinitions()) {
            String key = definition.getKey();
            if (key.isEmpty()) {
                key = invocation.getMethod().getName();
            }
            String expressionString = definition.getExpressionString();
            if (expressionString.isEmpty()) {
                Object[] arguments = invocation.getArguments();
                List<String> parameterNames = methodMdc.getParameterNames();
                for (int a = 0; a < arguments.length; a++) {
                    String parameterName = parameterNames.get(a);
                    String subKey = isNull(parameterName) ? synthesizeKey(key, a) : parameterName;
                    putMdc(subKey, arguments[a], definition, localKeys);
                }
            } else {
                Object value = expressionEvaluator.evaluate(expressionString);
                putMdc(key, value, definition, localKeys);
            }
        }
    }

    private void processParameterDefinitions(MethodInvocation invocation, MethodMdc methodMdc, Set<String> localKeys) {
        Object[] arguments = invocation.getArguments();
        for (int a = 0; a < arguments.length; a++) {
            for (ParameterMdc definition : methodMdc.getParameterDefinitions().get(a)) {
                String key = definition.getKey();
                if (key.isEmpty()) {
                    key = methodMdc.getParameterNames().get(a);
                    if (isNull(key)) {
                        key = synthesizeKey(invocation.getMethod().getName(), a);
                    }
                }
                String expressionString = definition.getExpressionString();
                Object value = expressionString.isEmpty() ? arguments[a] : expressionEvaluator.evaluate(expressionString, arguments[a]);
                putMdc(key, value, definition, localKeys);
            }
        }
    }

    private String synthesizeKey(String prefix, int index) {
        return prefix + "[" + index + "]";
    }

    private void putMdc(String key, Object value, ParameterMdc definition, Set<String> localKeys) {
        if (!definition.isGlobal()) {
            localKeys.add(key);
        }
        MDC.put(key, isNull(value) ? null : value.toString());
    }

    private static final class LocalKeysHolder implements AutoCloseable {

        private final Set<String> localKeys;

        private LocalKeysHolder(Set<String> localKeys) {
            this.localKeys = localKeys;
        }

        @Override
        public void close() throws Exception {
            localKeys.forEach(MDC::remove);
        }
    }
}
