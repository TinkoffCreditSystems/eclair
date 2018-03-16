package ru.tinkoff.eclair.validate.log.group;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.tinkoff.eclair.core.AnnotationAttribute;
import ru.tinkoff.eclair.core.LoggerBeanNamesResolver;
import ru.tinkoff.eclair.logger.EclairLogger;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * @author Viacheslav Klapatniuk
 */
abstract class LoggerSpecificLogAnnotationsValidator implements Validator {

    private final LoggerBeanNamesResolver loggerBeanNamesResolver = LoggerBeanNamesResolver.getInstance();
    private final Map<String, Set<String>> loggerNames;

    LoggerSpecificLogAnnotationsValidator(GenericApplicationContext genericApplicationContext,
                                          Map<String, EclairLogger> loggers) {
        this.loggerNames = loggers.keySet().stream()
                .collect(toMap(identity(), loggerName -> loggerBeanNamesResolver.resolve(genericApplicationContext, loggerName)));
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Set.class.isAssignableFrom(clazz);
    }

    /**
     * TODO: add validation of several aliases usage within one method
     */
    @Override
    public void validate(Object target, Errors errors) {
        @SuppressWarnings("unchecked")
        Set<? extends Annotation> annotations = (Set<Annotation>) target;

        groupAnnotationsByLogger(annotations, errors).entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> errors.reject("logger.duplicate",
                        format("Annotations duplicated for logger bean '%s': %s", entry.getKey(), entry.getValue())));
    }

    <T extends Annotation> Map<String, List<T>> groupAnnotationsByLogger(Set<T> annotations, Errors errors) {
        return annotations.stream()
                .collect(groupingBy(annotation -> {
                    String loggerName = AnnotationAttribute.LOGGER.extract(annotation);
                    return loggerNames.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(loggerName))
                            .findFirst()
                            .map(Map.Entry::getKey)
                            .orElseGet(() -> {
                                errors.reject("logger.unknown", format("Unknown logger '%s' specified for annotation: %s", loggerName, annotation));
                                return loggerName;
                            });
                }));
    }
}
