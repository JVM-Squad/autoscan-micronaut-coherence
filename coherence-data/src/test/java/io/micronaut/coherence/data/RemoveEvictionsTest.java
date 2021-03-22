package io.micronaut.coherence.data;

import com.tangosol.util.UUID;
import io.micronaut.coherence.data.model.Book;
import io.micronaut.coherence.data.repositories.AsyncBookRepository;
import io.micronaut.coherence.data.repositories.BookRepository;
import io.micronaut.coherence.data.repositories.CoherenceAsyncBookRepository;
import io.micronaut.coherence.data.repositories.CoherenceBookRepository;
import io.micronaut.coherence.data.util.EventRecord;
import io.micronaut.coherence.data.util.EventType;
import io.micronaut.context.BeanContext;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.async.AsyncCrudRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.inject.Inject;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Validate remove event eviction logic.
 */
@MicronautTest(propertySources = {"classpath:sessions.yaml"}, environments = "evict-remove")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class RemoveEvictionsTest extends AbstractDataTest {
    /**
     * A sync repo that extends {@link AbstractCoherenceRepository}.
     */
    @Inject
    protected CoherenceBookRepository repo;

    /**
     * A {@code repository} implementing {@link CrudRepository}.
     */
    @Inject
    protected BookRepository crudRepo;

    /**
     * A sync repo that extends {@link AbstractCoherenceAsyncRepository}.
     */
    @Inject
    protected CoherenceAsyncBookRepository repoAsync;

    /**
     * A {@code repository} implementing {@link AsyncCrudRepository}.
     */
    @Inject
    protected AsyncBookRepository crudRepoAsync;

    /**
     * Micronaut {@link BeanContext}.
     */
    @Inject
    protected BeanContext beanContext;

    // ----- test methods ---------------------------------------------------

    /**
     * Validate event listener returning false results in the entity not being removed using {@link #crudRepo}.
     */
    @Test
    public void shouldValidatePreRemoveEvictionSyncRepo() {
        runRemoveEventTestEviction(crudRepo);
    }

    /**
     * Validate event listener returning false results in the entity not being removed using {@link #repo}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldValidatePreRemoveEvictionSyncRepoCoherence() {
        runRemoveEventTestEviction(asCrudRepo(repo));
    }

    /**
     * Validate event listener returning false results in the entity not being removed using {@link #crudRepoAsync}.
     */
    @Test
    public void shouldValidatePreRemoveEvictionAsyncRepo() {
        runRemoveEventTestEviction(crudRepoAsync);
    }

    /**
     * Validate event listener returning false results in the entity not being removed using {@link #repoAsync}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldValidatePreRemoveEvictionAsyncRepoCoherence() {
        runRemoveEventTestEviction(asAsyncCrudRepo(repoAsync));
    }

    // ----- helper methods -------------------------------------------------

    /**
     * Validate eviction behavior.
     *
     * @param repository the {@link CrudRepository} under test
     */
    private void runRemoveEventTestEviction(final CrudRepository<Book, UUID> repository) {
        repository.delete(DUNE);
        assertThat(repository.existsById(DUNE.getUuid()), is(true));
        assertThat(eventRecorder.getRecordedEvents(), contains(
                new EventRecord<>(EventType.PRE_REMOVE, DUNE)));
    }

    /**
     * Validate eviction behavior.
     *
     * @param repository the {@link AsyncCrudRepository} under test
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runRemoveEventTestEviction(final AsyncCrudRepository<Book, UUID> repository) {
        CompletableFuture deleteFuture = repository.delete(DUNE);
        deleteFuture.thenCompose(unused -> repository.existsById(DUNE.getUuid()))
                .thenAccept(exists -> assertThat(exists, is(true)))
                .thenAccept(unused -> assertThat(eventRecorder.getRecordedEvents(), contains(
                        new EventRecord<>(EventType.PRE_REMOVE, DUNE)))).join();
    }
}
