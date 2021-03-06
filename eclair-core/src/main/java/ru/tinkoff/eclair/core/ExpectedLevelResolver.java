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

package ru.tinkoff.eclair.core;

import org.springframework.boot.logging.LogLevel;
import ru.tinkoff.eclair.definition.LogDefinition;

import java.util.function.Function;

/**
 * @author Vyacheslav Klapatnyuk
 */
public final class ExpectedLevelResolver implements Function<LogDefinition, LogLevel> {

    private static final ExpectedLevelResolver instance = new ExpectedLevelResolver();

    public static ExpectedLevelResolver getInstance() {
        return instance;
    }

    @Override
    public LogLevel apply(LogDefinition definition) {
        return min(definition.getLevel(), definition.getIfEnabledLevel());
    }

    private LogLevel min(LogLevel level1, LogLevel level2) {
        return level1.ordinal() <= level2.ordinal() ? level1 : level2;
    }
}
