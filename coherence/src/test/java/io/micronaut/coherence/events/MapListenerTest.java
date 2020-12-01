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
package io.micronaut.coherence.events;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.oracle.coherence.event.CacheName;
import com.oracle.coherence.event.Deleted;
import com.oracle.coherence.event.Inserted;
import com.oracle.coherence.event.MapName;
import com.oracle.coherence.event.ScopeName;
import com.oracle.coherence.event.ServiceName;
import com.oracle.coherence.event.Synchronous;
import com.oracle.coherence.event.Updated;
import com.oracle.coherence.inject.Name;
import com.oracle.coherence.inject.PropertyExtractor;
import com.oracle.coherence.inject.WhereFilter;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import data.Person;
import data.PhoneNumber;
import io.micronaut.coherence.annotation.CoherenceEventListener;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

@MicronautTest(propertySources = "classpath:sessions.yaml", environments = "MapListenerTest")
class MapListenerTest {
    @Inject
    @Name("test")
    Session session;

    @Inject
    TestListener listener;

    @Inject
    ApplicationContext ctx;

    @Test
    void testMapEvents() {
        NamedCache<String, Person> people = session.getCache("people");

        // Wait for the listener registration as it is async
        Eventually.assertDeferred(() -> EventsHelper.getListenerCount(people), is(greaterThanOrEqualTo(2)));

        people.put("homer", new Person("Homer", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("marge", new Person("Marge", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("bart", new Person("Bart", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("lisa", new Person("Lisa", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));
        people.put("maggie", new Person("Maggie", "Simpson", LocalDate.now(), new PhoneNumber(1, "555-123-9999")));

        people.invoke("homer", new Uppercase());
        people.invoke("bart", new Uppercase());

        people.remove("bart");
        people.remove("marge");
        people.remove("lisa");
        people.remove("maggie");

        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_INSERTED), is(5));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_UPDATED), is(2));
        Eventually.assertDeferred(() -> listener.getEvents(MapEvent.ENTRY_DELETED), is(4));

        // There should be an insert and an update for Bart.
        // The delete for Bart does not match the filter because the lastName
        // had been changed to uppercase.
        List<MapEvent<String, Person>> filteredEvents = listener.getFilteredEvents();
        Eventually.assertDeferred(filteredEvents::size, is(2));
        MapEvent<String, Person> eventOne = filteredEvents.get(0);
        MapEvent<String, Person> eventTwo = filteredEvents.get(1);
        assertThat(eventOne.getId(), is(MapEvent.ENTRY_INSERTED));
        assertThat(eventOne.getKey(), is("bart"));
        assertThat(eventTwo.getId(), is(MapEvent.ENTRY_UPDATED));
        assertThat(eventTwo.getKey(), is("bart"));
        assertThat(eventTwo.getNewValue().getLastName(), is("SIMPSON"));

        // Transformed events should just be inserts with the person's firstName as the new value
        List<MapEvent<String, String>> transformedEvents = listener.getTransformedEvents();
        Eventually.assertDeferred(transformedEvents::size, is(5));
        //        assertThat(transformedEvents.get(0).getNewValue(), is("Homer"));
        //        assertThat(transformedEvents.get(1).getNewValue(), is("Marge"));
        //        assertThat(transformedEvents.get(2).getNewValue(), is("Bart"));
        //        assertThat(transformedEvents.get(3).getNewValue(), is("Lisa"));
        //        assertThat(transformedEvents.get(4).getNewValue(), is("Maggie"));
    }

    // ---- helper classes --------------------------------------------------

    public static class Uppercase
            implements InvocableMap.EntryProcessor<String, Person, Object> {
        @Override
        public Object process(InvocableMap.Entry<String, Person> entry) {
            Person p = entry.getValue();
            p.setLastName(p.getLastName().toUpperCase());
            entry.setValue(p);
            return null;
        }
    }

    @Singleton
    @Context
    @Requires(env = "MapListenerTest")
    public static class TestListener {
        private final Map<Integer, Integer> events = new ConcurrentHashMap<>();

        private final List<MapEvent<String, Person>> filteredEvents = Collections.synchronizedList(new ArrayList<>());

        private final List<MapEvent<String, String>> transformedEvents = Collections.synchronizedList(new ArrayList<>());

        Integer getEvents(int id) {
            return events.get(id);
        }

        public List<MapEvent<String, Person>> getFilteredEvents() {
            return filteredEvents;
        }

        public List<MapEvent<String, String>> getTransformedEvents() {
            return transformedEvents;
        }

        @Synchronous
        @WhereFilter("firstName = 'Bart' and lastName = 'Simpson'")
        @CoherenceEventListener
        void onHomer(@CacheName("people") MapEvent<String, Person> event) {
            filteredEvents.add(event);
        }

        @Synchronous
        @CoherenceEventListener
        void onPersonDeleted(@Deleted @CacheName("people") MapEvent<String, Person> event) {
            record(event);
        }

        @Synchronous
        @CoherenceEventListener
        void onPersonInserted(@Inserted @ScopeName("Test") @MapName("people") MapEvent<String, Person> event) {
            record(event);
            assertThat(event.getNewValue().getLastName(), is("Simpson"));
        }

        @Synchronous
        @PropertyExtractor("firstName")
        @CoherenceEventListener
        void onPersonInsertedTransformed(@Inserted @MapName("people") MapEvent<String, String> event) {
            transformedEvents.add(event);
        }

        @Synchronous
        @CoherenceEventListener
        void onPersonUpdated(@Updated @ServiceName("StorageService") @MapName("people") MapEvent<String, Person> event) {
            record(event);
            assertThat(event.getOldValue().getLastName(), is("Simpson"));
            assertThat(event.getNewValue().getLastName(), is("SIMPSON"));
        }

        private void record(MapEvent<String, Person> event) {
            System.out.println("Received event: " + event);
            events.compute(event.getId(), (k, v) -> v == null ? 1 : v + 1);
        }
    }
}