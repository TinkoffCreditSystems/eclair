package ru.tinkoff.eclair.definition;

import org.junit.Test;
import ru.tinkoff.eclair.annotation.Log;
import ru.tinkoff.eclair.format.printer.Printer;
import ru.tinkoff.eclair.format.printer.ToStringPrinter;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.logging.LogLevel.TRACE;
import static org.springframework.boot.logging.LogLevel.WARN;
import static org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation;

/**
 * @author Viacheslav Klapatniuk
 */
public class OutLogDefinitionTest {

    @Test
    public void newInstance() {
        // given
        Log.out logOut = givenLogOut();
        Printer printer = givenPrinter();
        // when
        OutLogDefinition definition = new OutLogDefinition(logOut, printer);
        // then
        assertThat(definition.getLevel(), is(WARN));
        assertThat(definition.getIfEnabledLevel(), is(WARN));
        assertThat(definition.getVerboseLevel(), is(TRACE));
        assertThat(definition.getPrinter(), is(printer));
    }

    private Log.out givenLogOut() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("level", WARN);
        attributes.put("ifEnabled", WARN);
        attributes.put("verbose", TRACE);
        return synthesizeAnnotation(attributes, Log.out.class, null);
    }

    private Printer givenPrinter() {
        return new ToStringPrinter();
    }

    @Test
    public void newInstanceByValue() {
        // given
        Log.out logOut = givenLogOutByValue();
        Printer printer = givenPrinter();
        // when
        OutLogDefinition definition = new OutLogDefinition(logOut, printer);
        // then
        assertThat(definition.getLevel(), is(WARN));
    }

    private Log.out givenLogOutByValue() {
        return synthesizeAnnotation(singletonMap("value", WARN), Log.out.class, null);
    }
}
