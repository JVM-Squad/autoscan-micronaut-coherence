/*
 * Copyright 2017-2023 original authors
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier annotation used to annotate the parameter {@link CoherenceEventListener} annotated methods
 * that will receive {@link com.tangosol.net.events.partition.TransferEvent TransferEvents} of type
 * {@link com.tangosol.net.events.partition.TransferEvent.Type#LOST LOST}.
 *
 * @author Jonathan Knight
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Lost {
    /**
     * An annotation literal for the {@link Lost}
     * annotation.
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    final class Literal extends AnnotationLiteral<Lost> implements Lost {

        /**
         * Annotation literal instance for {@link Lost}.
         */
        public static final Literal INSTANCE = new Literal();
    }
}
