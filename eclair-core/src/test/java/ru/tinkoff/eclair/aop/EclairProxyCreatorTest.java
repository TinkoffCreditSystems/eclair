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

package ru.tinkoff.eclair.aop;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.tinkoff.eclair.annotation.Log;
import ru.tinkoff.eclair.core.AnnotationDefinitionFactory;
import ru.tinkoff.eclair.core.BeanFactoryHelper;
import ru.tinkoff.eclair.core.ExpressionEvaluator;
import ru.tinkoff.eclair.logger.SimpleLogger;
import ru.tinkoff.eclair.printer.Printer;
import ru.tinkoff.eclair.printer.ToStringPrinter;
import ru.tinkoff.eclair.printer.resolver.AliasedPrinterResolver;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Vyacheslav Klapatnyuk
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        EclairProxyCreator.class,
        ExpressionEvaluator.class,
        SpelExpressionParser.class,
        StandardEvaluationContext.class,
        AnnotationDefinitionFactory.class,
        BeanFactoryHelper.class,

        EclairProxyCreatorTest.InfrastructureTestConfiguration.class,
        EclairProxyCreatorTest.TestConfiguration.class
})
public class EclairProxyCreatorTest {

    @Autowired
    private EclairProxyCreator eclairProxyCreator;

    @Configuration
    public static class InfrastructureTestConfiguration {

        @Bean
        public Printer printer() {
            return new ToStringPrinter();
        }

        @Bean
        public SimpleLogger simpleLogger() {
            return new SimpleLogger();
        }

        @Bean
        public AliasedPrinterResolver aliasedPrinterResolver(ApplicationContext applicationContext,
                                                             List<Printer> printers) {
            return new AliasedPrinterResolver(
                    BeanFactoryHelper.getInstance().collectToOrderedMap(applicationContext, Printer.class, printers),
                    BeanFactoryHelper.getInstance().getAliases(applicationContext, Printer.class));
        }
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public Simple simple() {
            return new Simple();
        }

        @Bean
        public Parent parent() {
            return new Child();
        }

        @Bean
        public FactoryBean<Simple> simpleFactoryBean() {
            return new FactoryBean<Simple>() {
                @Override
                public Simple getObject() throws Exception {
                    return new Simple();
                }

                @Override
                public Class<?> getObjectType() {
                    return Simple.class;
                }

                @Override
                public boolean isSingleton() {
                    return true;
                }
            };
        }
    }

    @SuppressWarnings("unused")
    public static class Simple {

        @Log
        public void method() {
        }
    }

    @SuppressWarnings("unused")
    interface Parent {

        @Log
        void method();
    }

    public static class Child implements Parent {

        @Override
        public void method() {
        }
    }

    @Test
    public void getBeanClassCache() {
        // given
        // when
        Map<String, Class<?>> beanClassCache = eclairProxyCreator.getBeanClassCache();
        // then
        assertTrue(beanClassCache.get("eclairProxyCreatorTest.TestConfiguration") == TestConfiguration.class);
        assertTrue(beanClassCache.get("simple") == Simple.class);
        assertTrue(beanClassCache.get("parent") == Child.class);
        assertTrue(beanClassCache.get("simpleFactoryBean") == Simple.class);
    }
}
