package ru.tinkoff.eclair.validate.log.group;

import org.springframework.boot.logging.LogLevel;
import org.springframework.core.annotation.AnnotationUtils;
import ru.tinkoff.eclair.annotation.Log;
import ru.tinkoff.eclair.core.AnnotationAttribute;
import ru.tinkoff.eclair.validate.AnnotationUsageException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodLogsValidator extends GroupLogValidator<Annotation> {


    /**
     * @param loggerNames logger aliases grouped by its bean name
     */
    public MethodLogsValidator(Map<String, Set<String>> loggerNames) {
        super(loggerNames);
    }

    @Override
    public void validate(Method method, Set<Annotation> target) throws AnnotationUsageException {
        groupAnnotationsByLogger(method, target)
                .forEach((key, value) -> {
                    validateLogInAndOutUsage(method, value);
                    validateLevelValue(method, value);
                });
    }

    private void validateLogInAndOutUsage(Method target, List<Annotation> annotations) {
        boolean logsFound = annotations.stream().anyMatch(annotation -> annotation.annotationType() == Log.class);
        boolean logOutsFound = annotations.stream().anyMatch(annotation -> annotation.annotationType() == Log.in.class);
        boolean logInsFound = annotations.stream().anyMatch(annotation -> annotation.annotationType() == Log.out.class);
        if (logsFound && (logInsFound || logOutsFound)) {
            if (areTheAttributesTheSame(annotations)) {
                throw new AnnotationUsageException(target,
                        "Don't use @Log, @Log.in and @Log.out on methods together with the same config",
                        "Use only @Log to define the same behavior for beginning and ending logging");
            }
            throw new AnnotationUsageException(target,
                    "Don't use @Log with @Log.in or @Log.out on methods",
                    "Use both @Log.in and @Log.out annotations to define different behavior" +
                            "for beginning and ending logging");
        }
        if (logInsFound && logOutsFound && areTheAttributesTheSame(annotations)) {
            throw new AnnotationUsageException(target,
                    "Don't use @Log.in and @Log.out on methods together with the same config",
                    "Use only @Log to define the same behavior for beginning and ending logging");
        }
    }

    private boolean areTheAttributesTheSame(List<Annotation> logs) {
        return logs.stream().map(AnnotationUtils::getAnnotationAttributes)
                .distinct().count() == 1;
    }

    private void validateLevelValue(Method target, List<Annotation> annotations) {
        if (annotations.stream().anyMatch(annotation -> AnnotationAttribute.LEVEL.extract(annotation).equals(LogLevel.OFF))) {
            throw new AnnotationUsageException(target,
                    "Don't use level = LogLevel.OFF for methods. It's equivalent to no annotation",
                    "Remove unnecessary annotation");
        }
    }
}
