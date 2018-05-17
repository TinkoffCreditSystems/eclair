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

package ru.tinkoff.eclair.validate.log.single;

import org.springframework.boot.logging.LogLevel;
import org.springframework.util.StringUtils;
import ru.tinkoff.eclair.core.AnnotationAttribute;
import ru.tinkoff.eclair.printer.Printer;
import ru.tinkoff.eclair.printer.resolver.PrinterResolver;
import ru.tinkoff.eclair.validate.AnnotationUsageException;
import ru.tinkoff.eclair.validate.AnnotationUsageValidator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.springframework.boot.logging.LogLevel.OFF;

/**
 *
 * @author Vyacheslav Klapatnyuk
 */
public class LogValidator<T extends Annotation> implements AnnotationUsageValidator<T> {

    private final PrinterResolver printerResolver;

    public LogValidator(PrinterResolver printerResolver) {
        this.printerResolver = printerResolver;
    }

    @Override
    public void validate(Method method, T target) throws AnnotationUsageException {
        LogLevel expectedLevel = AnnotationAttribute.LEVEL.extract(target);
        LogLevel ifEnabledLevel = AnnotationAttribute.IF_ENABLED.extract(target);
        if (ifEnabledLevel.ordinal() >= expectedLevel.ordinal() && ifEnabledLevel != OFF) {
            throw new AnnotationUsageException(method,
                    format("'ifEnabled = %s' should be more strict than 'level = %s'", ifEnabledLevel, expectedLevel),
                    "Correct specified levels relation or replace 'ifEnabled' by default value 'OFF'",
                    target);
        }

        String printerName = AnnotationAttribute.PRINTER.extract(target);
        if (StringUtils.hasText(printerName)) {
            Printer printer = printerResolver.resolve(printerName);
            if (isNull(printer)) {
                throw new AnnotationUsageException(method,
                        format("Unknown printer '%s'", printerName),
                        "Use correct bean name or alias to specify 'printer'",
                        target);
            }
        }
    }
}
