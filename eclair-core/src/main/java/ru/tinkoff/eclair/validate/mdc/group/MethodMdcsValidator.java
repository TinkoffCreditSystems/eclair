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

package ru.tinkoff.eclair.validate.mdc.group;

import ru.tinkoff.eclair.annotation.Mdc;
import ru.tinkoff.eclair.validate.AnnotationUsageException;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author Vyacheslav Klapatnyuk
 */
public class MethodMdcsValidator extends MdcsValidator {

    @Override
    public void validate(Method method, Collection<Mdc> target) throws AnnotationUsageException {
        super.validate(method, target);

        target.stream()
                .filter(mdc -> mdc.value().isEmpty())
                .findFirst()
                .ifPresent(mdc -> {
                    if (method.getParameterCount() == 0) {
                        throw new AnnotationUsageException(method,
                                "Method not contains any parameters",
                                "Do not use empty @Mdc annotation on method without parameters",
                                mdc);
                    }
                });
    }
}
