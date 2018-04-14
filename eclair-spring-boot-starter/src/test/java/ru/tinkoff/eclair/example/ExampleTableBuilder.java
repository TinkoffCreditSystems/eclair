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

package ru.tinkoff.eclair.example;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.logging.LogLevel;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * @author Vyacheslav Klapatnyuk
 */
class ExampleTableBuilder {

    static final String TABLE_HEADER = " Enabled level      | Log sample\n--------------------|------------\n";

    /**
     * ' `LEVEL` .. `LEVEL` '
     */
    private static final int LEVELS_CELL_WIDTH = 1 + 7 + 1 + 2 + 1 + 7 + 1;

    private boolean hide = true;
    private PatternLayout patternLayout;

    void setPatternLayout(PatternLayout patternLayout) {
        this.patternLayout = patternLayout;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    String buildSampleCell(List<ILoggingEvent> events) {
        if (events.isEmpty()) {
            return "-";
        }
        return events.stream().map(patternLayout::doLayout).map(this::asCode).collect(joining("<br>"));
    }

    String buildTable(Map<String, List<LogLevel>> levels) {
        String body = buildTableBody(levels);
        return TABLE_HEADER + body;
    }

    private String buildTableBody(Map<String, List<LogLevel>> levels) {
        return levels.entrySet().stream()
                .map(entry -> buildLevelsCell(entry.getValue()) + "| " + entry.getKey())
                .collect(joining("\n"));
    }

    private String buildLevelsCell(List<LogLevel> logLevels) {
        StringBuilder payload = new StringBuilder(" ");
        payload.append((hide && logLevels.size() > 2) ? buildCroppedLevelsCell(logLevels) : buildFullLevelsCell(logLevels));
        while (payload.length() < LEVELS_CELL_WIDTH) {
            payload.append(" ");
        }
        return payload.toString();
    }

    private String buildCroppedLevelsCell(List<LogLevel> logLevels) {
        return asCode(logLevels.get(0).name()) + " .. " + asCode(logLevels.get(logLevels.size() - 1).name());
    }

    private String buildFullLevelsCell(List<LogLevel> logLevels) {
        return logLevels.stream().map(Enum::name).map(this::asCode).collect(joining(" "));
    }

    private String asCode(String input) {
        return '`' + input + '`';
    }
}