/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.coherence.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.micronaut.context.annotation.AliasFor;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.messaging.annotation.MessageMapping;

/**
 * Method level annotation used to specify a topic name.
 *
 * @author Jonathan Knight
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Bindable
@Repeatable(Topics.class)
public @interface Topic {

    /**
     * @return The topics to subscribe to
     */
    @AliasFor(annotation = MessageMapping.class, member = "value")
    String[] value() default {};
}
