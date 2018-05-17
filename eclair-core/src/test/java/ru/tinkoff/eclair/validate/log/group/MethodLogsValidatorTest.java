package ru.tinkoff.eclair.validate.log.group;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.annotation.AnnotationUtils;
import ru.tinkoff.eclair.annotation.Log;
import ru.tinkoff.eclair.validate.AnnotationUsageException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

public class MethodLogsValidatorTest {

    private Method method;

    @Before
    public void init() throws NoSuchMethodException {
        method = MethodLogsValidatorTest.class.getMethod("init");
    }

    @Test(expected = AnnotationUsageException.class)
    public void validateLogAndLogInUsage() {
        // given
        Map<String, Set<String>> loggerNames = singletonMap("logger", new HashSet<>(asList("logger", "")));
        MethodLogsValidator methodLogsValidator = new MethodLogsValidator(loggerNames);
        Set<Log> logs = singleton(AnnotationUtils.synthesizeAnnotation(Log.class));
        Set<Log.in> logIns = singleton(AnnotationUtils.synthesizeAnnotation(Log.in.class));
        // when
        methodLogsValidator.validate(method, Stream.of(logs, logIns)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));
        // then expected exception
    }

    @Test(expected = AnnotationUsageException.class)
    public void validateLogAndLogOutUsage() {
        // given
        Map<String, Set<String>> loggerNames = singletonMap("logger", new HashSet<>(asList("logger", "")));
        MethodLogsValidator methodLogsValidator = new MethodLogsValidator(loggerNames);
        Set<Log> logs = singleton(AnnotationUtils.synthesizeAnnotation(Log.class));
        Set<Log.out> logOuts = singleton(AnnotationUtils.synthesizeAnnotation(Log.out.class));
        // when
        methodLogsValidator.validate(method, Stream.of(logs, logOuts)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));
        // then expected exception
    }

    @Test(expected = AnnotationUsageException.class)
    public void validateLogOutAndLogInUsage() {
        // given
        Map<String, Set<String>> loggerNames = singletonMap("logger", new HashSet<>(asList("logger", "")));
        MethodLogsValidator methodLogsValidator = new MethodLogsValidator(loggerNames);
        Set<Log.in> logIns = singleton(AnnotationUtils.synthesizeAnnotation(Log.in.class));
        Set<Log.out> logOuts = singleton(AnnotationUtils.synthesizeAnnotation(Log.out.class));
        // when
        methodLogsValidator.validate(method, Stream.of(logIns, logOuts)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));
        // then expected exception
    }

    @Test(expected = AnnotationUsageException.class)
    public void validateLogAndLogOutAndLogInUsage() {
        // given
        Map<String, Set<String>> loggerNames = singletonMap("logger", new HashSet<>(asList("logger", "")));
        MethodLogsValidator methodLogsValidator = new MethodLogsValidator(loggerNames);
        Set<Log> logs = singleton(AnnotationUtils.synthesizeAnnotation(Log.class));
        Set<Log.in> logIns = singleton(AnnotationUtils.synthesizeAnnotation(Log.in.class));
        Set<Log.out> logOuts = singleton(AnnotationUtils.synthesizeAnnotation(Log.out.class));
        // when
        methodLogsValidator.validate(method, Stream.of(logs, logIns, logOuts)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));
        // then expected exception
    }

    @Test(expected = AnnotationUsageException.class)
    public void validateLevelOffUsage() {
        // given
        Map<String, Set<String>> loggerNames = singletonMap("logger", new HashSet<>(asList("logger", "")));
        MethodLogsValidator methodLogsValidator = new MethodLogsValidator(loggerNames);
        Set<Annotation> logs = singleton(AnnotationUtils.synthesizeAnnotation(singletonMap("level", LogLevel.OFF), Log.class, null));
        // when
        methodLogsValidator.validate(method, logs);
        // then expected exception
    }
}
