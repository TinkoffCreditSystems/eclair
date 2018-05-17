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

package ru.tinkoff.eclair.validate.log.group;

import org.junit.Before;
import org.junit.Test;
import ru.tinkoff.eclair.annotation.Log;
import ru.tinkoff.eclair.printer.resolver.AliasedPrinterResolver;
import ru.tinkoff.eclair.validate.AnnotationUsageException;
import ru.tinkoff.eclair.validate.log.single.LogOutValidator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation;

/**
 * @author Vyacheslav Klapatnyuk
 */
public class LogOutsValidatorTest {

    private Method method;

    @Before
    public void init() throws NoSuchMethodException {
        method = LogOutsValidatorTest.class.getMethod("init");
    }

    @Test
    public void isTransitive() {
        // given
        Map<String, Set<String>> loggerNames = singletonMap("logger", new HashSet<>(asList("logger", "")));
        LogOutValidator logOutValidator =   mock(LogOutValidator.class);
        LogOutsValidator logOutsValidator = new LogOutsValidator(loggerNames, logOutValidator);
        Log.out logOut = synthesizeAnnotation(Log.out.class);
        Set<Log.out> annotations = singleton(logOut);
        // when
        logOutsValidator.validate(method, annotations);
        // then
        verify(logOutValidator).validate(method, logOut);
    }

    @Test(expected = AnnotationUsageException.class)
    public void validateVoidMethodLogging() {
        // given
        Map<String, Set<String>> loggerNames = singletonMap("logger", new HashSet<>(asList("logger", "")));
        LogOutValidator logOutValidator =   new LogOutValidator(new AliasedPrinterResolver(Collections.emptyMap(), Collections.emptyMap()));
        LogOutsValidator logOutsValidator = new LogOutsValidator(loggerNames, logOutValidator);
        Map<String, Object> printer = singletonMap("printer", "jacksonPrinter");
        Log.out logOut = synthesizeAnnotation(printer, Log.out.class, null);
        Set<Log.out> annotation = singleton(logOut);
        // when
        logOutsValidator.validate(method, annotation);
        // then expected exception
    }
}
