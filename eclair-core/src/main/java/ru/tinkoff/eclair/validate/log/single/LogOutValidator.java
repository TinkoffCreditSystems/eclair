package ru.tinkoff.eclair.validate.log.single;

import ru.tinkoff.eclair.annotation.Log;
import ru.tinkoff.eclair.printer.resolver.PrinterResolver;
import ru.tinkoff.eclair.validate.AnnotationUsageException;

import java.lang.reflect.Method;

public class LogOutValidator extends LogValidator<Log.out> {

    public LogOutValidator(PrinterResolver printerResolver) {
        super(printerResolver);
    }

    @Override
    public void validate(Method method, Log.out target) throws AnnotationUsageException {
        super.validate(method, target);
        if (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class) {
            if (!target.printer().isEmpty()) {
                throw new AnnotationUsageException(method,
                        "Printer was defined for void method",
                        "Remove unnecessary printer parameter");
            }
        }
    }
}
