package org.opendaylight.controller.cluster.datastore.persistence;

import akka.actor.Props;
import akka.persistence.PersistentRepr;
import akka.testkit.TestActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NonPersistentJournalTest extends AbstractActorTest {

    @Test
    public void testDoAsyncWriteMessages() throws TimeoutException, InterruptedException {

        TestActorRef<NonPersistentJournal> ref = TestActorRef.create(getSystem(), Props.create(NonPersistentJournal.class));

        Future<Void> future = addJournalEntries(ref);

        Await.ready(future, Duration.apply(1, TimeUnit.SECONDS));

        assertEquals(1L, (long) ref.underlyingActor().journals().get("foo").get());
        assertEquals(2L, (long) ref.underlyingActor().journals().get("bar").get());
        assertNull(ref.underlyingActor().journals().get("non"));
    }

    @Test
    public void testDoAsyncReadHighestSequenceNr() throws TimeoutException, InterruptedException {

        TestActorRef<NonPersistentJournal> ref = TestActorRef.create(getSystem(), Props.create(NonPersistentJournal.class));

        Future<Void> future = addJournalEntries(ref);

        Await.ready(future, Duration.apply(1, TimeUnit.SECONDS));

        Future<Long> foo = ref.underlyingActor().doAsyncReadHighestSequenceNr("foo", -1L);

        Await.ready(foo, Duration.apply(1, TimeUnit.SECONDS));

        assertEquals(1L, (long) foo.value().get().get());

        Future<Long> bar = ref.underlyingActor().doAsyncReadHighestSequenceNr("bar", -1L);

        Await.ready(bar, Duration.apply(1, TimeUnit.SECONDS));

        assertEquals(2L, (long) bar.value().get().get());

        Future<Long> non = ref.underlyingActor().doAsyncReadHighestSequenceNr("non", -1L);

        Await.ready(non, Duration.apply(1, TimeUnit.SECONDS));

        assertEquals((long) NonPersistentJournal.EMPTY, (long) non.value().get().get());

    }


    private PersistentRepr entry(String persistenceId) {
        PersistentRepr mock = mock(PersistentRepr.class);
        when(mock.persistenceId()).thenReturn(persistenceId);
        return mock;
    }

    private Future<Void> addJournalEntries(TestActorRef<NonPersistentJournal> ref) {
        List<PersistentRepr> l = new ArrayList<>();
        l.add(entry("foo"));
        l.add(entry("foo"));
        l.add(entry("bar"));
        l.add(entry("bar"));
        l.add(entry("bar"));
        return ref.underlyingActor().doAsyncWriteMessages(l);
    }




}