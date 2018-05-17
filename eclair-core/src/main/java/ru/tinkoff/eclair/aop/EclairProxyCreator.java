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

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;
import ru.tinkoff.eclair.core.*;
import ru.tinkoff.eclair.definition.*;
import ru.tinkoff.eclair.definition.method.MethodLog;
import ru.tinkoff.eclair.definition.method.MethodMdc;
import ru.tinkoff.eclair.definition.method.factory.MethodLogFactory;
import ru.tinkoff.eclair.definition.method.factory.MethodMdcFactory;
import ru.tinkoff.eclair.logger.EclairLogger;
import ru.tinkoff.eclair.printer.resolver.PrinterResolver;
import ru.tinkoff.eclair.validate.MethodValidator;
import ru.tinkoff.eclair.validate.log.group.*;
import ru.tinkoff.eclair.validate.log.single.LogErrorValidator;
import ru.tinkoff.eclair.validate.log.single.LogOutValidator;
import ru.tinkoff.eclair.validate.log.single.LogValidator;
import ru.tinkoff.eclair.validate.mdc.group.MdcsValidator;
import ru.tinkoff.eclair.validate.mdc.group.MergedMdcsValidator;
import ru.tinkoff.eclair.validate.mdc.group.MethodMdcsValidator;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Vyacheslav Klapatnyuk
 */
public class EclairProxyCreator extends AbstractAutoProxyCreator {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private final Map<String, Class<?>> beanClassCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object[]> advisorsCache = new ConcurrentHashMap<>();

    private final GenericApplicationContext applicationContext;
    private final AnnotationDefinitionFactory annotationDefinitionFactory;
    private final Map<String, EclairLogger> loggers;
    private final ExpressionEvaluator expressionEvaluator;
    private final MethodValidator methodValidator;

    private final LoggerBeanNamesResolver loggerBeanNamesResolver = LoggerBeanNamesResolver.getInstance();
    private final AnnotationExtractor annotationExtractor = AnnotationExtractor.getInstance();
    private final ParameterNameResolver parameterNameResolver = new ParameterNameResolver();

    private boolean validate = false;

    /**
     * @param orderedLoggers in order of execution, if necessary
     */
    public EclairProxyCreator(GenericApplicationContext applicationContext,
                              AnnotationDefinitionFactory annotationDefinitionFactory,
                              List<EclairLogger> orderedLoggers,
                              ExpressionEvaluator expressionEvaluator,
                              PrinterResolver printerResolver) {
        this.applicationContext = applicationContext;
        this.annotationDefinitionFactory = annotationDefinitionFactory;
        this.loggers = BeanFactoryHelper.getInstance().collectToOrderedMap(applicationContext, EclairLogger.class, orderedLoggers);
        this.expressionEvaluator = expressionEvaluator;
        this.methodValidator = initMethodValidator(printerResolver);
    }

    private MethodValidator initMethodValidator(PrinterResolver printerResolver) {
        LoggerBeanNamesResolver loggerBeanNamesResolver = LoggerBeanNamesResolver.getInstance();
        Map<String, Set<String>> loggerNames = loggers.keySet().stream()
                .collect(toMap(
                        identity(),
                        loggerName -> loggerBeanNamesResolver.resolve(applicationContext, loggerName))
                );
        return new MethodValidator(AnnotationExtractor.getInstance(),
                new LogsValidator(loggerNames, new LogValidator<>(printerResolver)),
                new LogInsValidator(loggerNames, new LogValidator<>(printerResolver)),
                new LogOutsValidator(loggerNames, new LogOutValidator(printerResolver)),
                new LogErrorsValidator(loggerNames, new LogErrorValidator(printerResolver)),
                new ParameterLogsValidator(loggerNames, new LogValidator<>(printerResolver)),
                new MethodLogsValidator(loggerNames),
                new MethodMdcsValidator(),
                new MdcsValidator(),
                new MergedMdcsValidator()
        );
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> targetClass = (bean instanceof FactoryBean) ? ((FactoryBean) bean).getObjectType() : ClassUtils.getUserClass(bean);
        beanClassCache.putIfAbsent(beanName, targetClass);
        return bean;
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        Class<?> targetClass = beanClassCache.computeIfAbsent(beanName, s -> ClassUtils.getUserClass(beanClass));
        Object[] cachedAdvisors = advisorsCache.get(targetClass);
        if (nonNull(cachedAdvisors)) {
            return cachedAdvisors.length == 0 ? AbstractAutoProxyCreator.DO_NOT_PROXY : cachedAdvisors;
        }
        if (!supports(targetClass)) {
            advisorsCache.put(targetClass, EMPTY_ARRAY);
            return AbstractAutoProxyCreator.DO_NOT_PROXY;
        }
        if (validate) {
            annotationExtractor.getCandidateMethods(targetClass).forEach(methodValidator::validate);
        }
        MdcAdvisor mdcAdvisor = getMdcAdvisor(targetClass);
        List<LogAdvisor> logAdvisors = getLoggingAdvisors(targetClass);
        Object[] composedAdvisors = composeAdvisors(mdcAdvisor, logAdvisors);
        advisorsCache.put(targetClass, composedAdvisors);
        return composedAdvisors.length == 0 ? AbstractAutoProxyCreator.DO_NOT_PROXY : composedAdvisors;
    }

    private boolean supports(Class<?> targetClass) {
        return annotationExtractor.getCandidateMethods(targetClass).stream().anyMatch(method ->
                annotationExtractor.hasAnyAnnotation(method) || Stream.of(method.getParameters()).anyMatch(annotationExtractor::hasAnyAnnotation)
        );
    }

    @Override
    protected boolean advisorsPreFiltered() {
        return true;
    }

    private MdcAdvisor getMdcAdvisor(Class<?> beanClass) {
        List<MethodMdc> methodMdcs = annotationExtractor.getCandidateMethods(beanClass).stream()
                .map(this::getMethodMdc)
                .filter(Objects::nonNull)
                .collect(toList());
        return MdcAdvisor.newInstance(methodMdcs, expressionEvaluator);
    }

    private MethodMdc getMethodMdc(Method method) {
        List<String> parameterNames = parameterNameResolver.tryToResolve(method);
        Set<ParameterMdc> methodParameterMdcs = annotationDefinitionFactory.buildMethodParameterMdcs(method);
        List<Set<ParameterMdc>> parameterMdcs = annotationDefinitionFactory.buildParameterMdcs(method);
        return MethodMdcFactory.newInstance(method, parameterNames, methodParameterMdcs, parameterMdcs);
    }

    private List<LogAdvisor> getLoggingAdvisors(Class<?> beanClass) {
        return loggers.entrySet().stream()
                .map(entry -> getLogAdvisor(beanClass, entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private LogAdvisor getLogAdvisor(Class<?> beanClass, String loggerName, EclairLogger eclairLogger) {
        List<MethodLog> methodLogs = getMethodLogs(beanClass, loggerName);
        return LogAdvisor.newInstance(eclairLogger, methodLogs);
    }

    private List<MethodLog> getMethodLogs(Class<?> beanClass, String loggerName) {
        Set<String> loggerNames = loggerBeanNamesResolver.resolve(applicationContext, loggerName);
        return annotationExtractor.getCandidateMethods(beanClass).stream()
                .map(method -> getMethodLog(loggerNames, method))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private MethodLog getMethodLog(Set<String> loggerNames, Method method) {
        List<String> parameterNames = parameterNameResolver.tryToResolve(method);
        InLog inLog = annotationDefinitionFactory.buildInLog(loggerNames, method);
        List<ParameterLog> parameterLogs = annotationDefinitionFactory.buildParameterLogs(loggerNames, method);
        OutLog outLog = annotationDefinitionFactory.buildOutLog(loggerNames, method);
        Set<ErrorLog> errorLogs = annotationDefinitionFactory.buildErrorLogs(loggerNames, method);
        return MethodLogFactory.newInstance(method, parameterNames, inLog, parameterLogs, outLog, errorLogs);
    }

    private Object[] composeAdvisors(MdcAdvisor mdcAdvisor, List<LogAdvisor> logAdvisors) {
        if (isNull(mdcAdvisor)) {
            return logAdvisors.toArray();
        }
        List<Advisor> advisors = new ArrayList<>(singletonList(mdcAdvisor));
        advisors.addAll(logAdvisors);
        return advisors.toArray();
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    Map<String, Class<?>> getBeanClassCache() {
        return beanClassCache;
    }

    @Override
    protected void customizeProxyFactory(ProxyFactory proxyFactory) {
        proxyFactory.setProxyTargetClass(true);
    }
}
