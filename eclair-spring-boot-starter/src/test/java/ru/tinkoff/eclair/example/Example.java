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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.tinkoff.eclair.annotation.Log;
import ru.tinkoff.eclair.annotation.Mdc;
import ru.tinkoff.eclair.logger.ManualLogger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import static org.springframework.boot.logging.LogLevel.*;

/**
 * @author Vyacheslav Klapatnyuk
 */
@SuppressWarnings("unused")
class Example {

    @Log
    void simple() {
        // logic
    }

    void simplePlain() {
        Logger logger = LoggerFactory.getLogger("ru.tinkoff.eclair.example.Example.simple");
        logger.debug(">");
        // logic
        logger.debug("<");
    }

    @Log
    void simpleError() {
        throw new RuntimeException();
    }

    void simpleErrorPlain() {
        Logger logger = LoggerFactory.getLogger("ru.tinkoff.eclair.example.Example.simpleError");
        logger.debug(">");
        try {
            throw new RuntimeException();
        } catch (Exception e) {
            logger.debug("!");
            throw e;
        }
    }

    @Log(INFO)
    void level() {
        // logic
    }

    void levelPlain() {
        Logger logger = LoggerFactory.getLogger("ru.tinkoff.eclair.example.Example.level");
        logger.info(">");
        // logic
        logger.info("<");
    }

    @Log(level = INFO, ifEnabled = DEBUG)
    void ifEnabled() {
        // logic
    }

    void ifEnabledPlain() {
        Logger logger = LoggerFactory.getLogger("ru.tinkoff.eclair.example.Example.ifEnabled");
        if (logger.isDebugEnabled()) {
            logger.info(">");
        }
        // logic
        if (logger.isDebugEnabled()) {
            logger.info("<");
        }
    }

    @Log(INFO)
    boolean verbose(String s, Integer i, Double d) {
        return false;
    }

    boolean verbosePlain(String s, Integer i, Double d) {
        Logger logger = LoggerFactory.getLogger("ru.tinkoff.eclair.example.Example.verbose");
        if (logger.isDebugEnabled()) {
            logger.info("> s=\"{}\", i={}, d={}", s, i, d);
        } else {
            logger.info(">");
        }
        boolean result = false;
        if (logger.isDebugEnabled()) {
            logger.info("< {}", result);
        } else {
            logger.info("<");
        }
        return result;
    }

    @Log(verbose = OFF)
    boolean verboseDisabled(String s, Integer i, Double d) {
        return false;
    }

    boolean verboseDisabledPlain(String s, Integer i, Double d) {
        Logger logger = LoggerFactory.getLogger("ru.tinkoff.eclair.example.Example.verboseDisabled");
        logger.debug(">");
        boolean result = false;
        logger.debug("<");
        return result;
    }

    @Log(printer = "jacksonPrinter")
    void json(Dto dto, Integer i) {
    }

    @Log(printer = "jaxb2Printer")
    void xml(Dto dto, Integer i) {
    }

    @Log.in(INFO)
    @Log.out(TRACE)
    void inOut(Dto dto, String s, Integer i) {
    }

    @Log.error
    void error() {
        throw new RuntimeException("Something strange happened");
    }

    @Log.error(level = WARN, ifEnabled = DEBUG)
    void warningOnDebug() {
        throw new RuntimeException("Something strange happened, but it doesn't matter");
    }

    @Log.error(level = WARN, ofType = {NullPointerException.class, IndexOutOfBoundsException.class})
    @Log.error(exclude = Error.class)
    void filterErrors(Throwable throwable) throws Throwable {
        throw throwable;
    }

    @Log.error(level = ERROR, ofType = Exception.class)
    @Log.error(level = WARN, ofType = RuntimeException.class)
    void mostSpecific() {
        throw new IllegalArgumentException();
    }

    void parameter(@Log(INFO) Dto dto, String s, Integer i) {
    }

    @Log.out(printer = "maskJaxb2Printer")
    Dto printers(@Log(printer = "maskJaxb2Printer") Dto xml,
                 @Log(printer = "jacksonPrinter") Dto json,
                 Integer i) {
        return xml;
    }

    @Log.in(INFO)
    void parameterLevels(@Log(INFO) Double d,
                         @Log(DEBUG) String s,
                         @Log(TRACE) Integer i) {
    }

    @Log.in(INFO)
    @Log.out(level = TRACE, verbose = TRACE)
    @Log.error(level = WARN, ofType = RuntimeException.class, exclude = NullPointerException.class)
    @Log.error(level = ERROR, ofType = {Error.class, Exception.class})
    Dto mix(@Log(printer = "jaxb2Printer") Dto xml,
            @Log(ifEnabled = TRACE, printer = "jacksonPrinter") Dto json,
            Integer i) {
        throw new IllegalArgumentException("Something strange happened");
    }

    private Example self;

    @Autowired
    public void setSelf(Example self) {
        this.self = self;
    }

    @Log
    void outer() {
        self.mdc();
    }

    @Mdc(key = "static", value = "string")
    @Mdc(key = "sum", value = "1 + 1", global = true)
    @Mdc(key = "beanReference", value = "@jacksonPrinter.print(new ru.tinkoff.eclair.example.Dto())")
    @Mdc(key = "staticMethod", value = "T(java.util.UUID).randomUUID()")
    @Log
    void mdc() {
        self.inner();
    }

    @Log.in
    void inner() {
    }

    @Log.in
    void mdcByArgument(@Mdc(key = "dto", value = "#this")
                       @Mdc(key = "length", value = "s.length()") Dto dto) {
    }

    @Mdc
    @Log.in
    void mdcByDefault(String first, Double second) {
    }

    /**
     * DEBUG [s=s, i=0] ru.tinkoff.eclair.example.Example.mdc > s="s", i=0
     * else if parameter names not enabled
     * DEBUG [mdc[0]=s, mdc[1]=0] ru.tinkoff.eclair.example.Example.mdc > "s", 0
     */
    @Log.in
    void mdc(@Mdc String s, @Mdc Integer i) {
    }

    /**
     * DEBUG [methodKey[s]=s, methodKey[i]=0] ru.tinkoff.eclair.example.Example.mdc > s="s", i=0
     */
    @Mdc(key = "methodKey")
    @Log.in
    void mdc1(String s, Integer i) {
    }

    /**
     * DEBUG [mdc2=methodValue] ru.tinkoff.eclair.example.Example.mdc > s="s", i=0
     */
    @Mdc("methodValue")
    @Log.in
    void mdc2(String s, Integer i) {
    }

    /**
     * Equals to {@link Example#mdc(java.lang.String, java.lang.Integer)}
     * DEBUG [s=s, i=0] ru.tinkoff.eclair.example.Example.mdc > s="s", i=0
     * else if parameter names not enabled
     * DEBUG [mdc3[0]=s, mdc3[1]=0] ru.tinkoff.eclair.example.Example.mdc > "s", 0
     */
    @Mdc
    @Log.in
    void mdc3(String s, Integer i) {
    }

    @Autowired
    @Qualifier("simpleLogger")
    private ManualLogger logger;

    @Log
    void manual() {
        logger.info("Eager logging: {}", Math.PI);
        logger.debug("Lazy logging: {}", (Supplier) () -> Math.PI);
    }

    @Log
    void manualLevel() {
        // log ERROR
        logger.error("ERROR");
        logger.errorIfWarnEnabled("ERROR if WARN enabled");
        logger.errorIfInfoEnabled("ERROR if INFO enabled");
        logger.errorIfDebugEnabled("ERROR if DEBUG enabled");
        logger.errorIfTraceEnabled("ERROR if TRACE enabled");
        // log WARN
        logger.warn("WARN");
        logger.warnIfInfoEnabled("WARN if INFO enabled");
        logger.warnIfDebugEnabled("WARN if DEBUG enabled");
        logger.warnIfTraceEnabled("WARN if TRACE enabled");
        // log INFO
        logger.info("INFO");
        logger.infoIfDebugEnabled("INFO if DEBUG enabled");
        logger.infoIfTraceEnabled("INFO if TRACE enabled");
        // log DEBUG
        logger.debug("DEBUG");
        logger.debugIfTraceEnabled("DEBUG if TRACE enabled");
        // log TRACE
        logger.trace("TRACE");
    }

    /**
     * Equals to next
     */
    @Log.in(ERROR)
    @Log.out(TRACE)
    void inLog() {
    }

    @Log.in(ERROR)
    @Log.out(TRACE)
    void inOut() {
    }

    // TODO: add example for {@link LogError} ordering (in order of appearance)

    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Log(level = INFO, printer = "jaxb2Printer")
    @Log.error(ofType = RuntimeException.class)
    @interface Audit {
    }

    @Log(level = INFO, printer = "jaxb2Printer")
    @Log.error(ofType = RuntimeException.class)
    void listing() {
    }

    @Audit
    void meta() {
    }

    @Log
    @Log(logger = "auditLogger")
    void multiLogger() {
    }

    /**
     * As if there is no '@Log.in' above method:
     * DEBUG [] ru.tinkoff.eclair.example.Example.offLevelWithArg > a=1
     */
    void offLevelWithArg(@Log int a) {
    }

    /**
     * As if there is no '@Log' above parameter:
     * DEBUG [] ru.tinkoff.eclair.example.Example.offLevelWithArg >
     */
    @Log.in
    void argOffLevel(@Log(OFF) int a) {
    }
}
