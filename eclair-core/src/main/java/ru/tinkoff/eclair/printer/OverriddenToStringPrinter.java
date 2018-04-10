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

package ru.tinkoff.eclair.printer;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static java.util.Objects.isNull;

/**
 * @author Viacheslav Klapatniuk
 */
public class OverriddenToStringPrinter extends ToStringPrinter {

    @Override
    public boolean supports(Class<?> clazz) {
        Method toString = ReflectionUtils.findMethod(clazz, "toString");
        return isNull(toString) || !toString.getDeclaringClass().equals(Object.class);
    }
}
