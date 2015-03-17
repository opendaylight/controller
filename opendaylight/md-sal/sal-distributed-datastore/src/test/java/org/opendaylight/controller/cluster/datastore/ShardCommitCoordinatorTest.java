package org.opendaylight.controller.cluster.datastore;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import akka.actor.Props;
import akka.actor.Status;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.slf4j.Logger;

public class ShardCommitCoordinatorTest extends AbstractActorTest {

    @Test
    public void testCohortEntryExpiry() throws Exception {

        Logger logger = mock(Logger.class);

        ShardCommitCoordinator coordinator = new ShardCommitCoordinator(2, 100, logger , "coordinator");
        coordinator.transactionReady("transaction-67", mock(DOMStoreThreePhaseCommitCohort.class),
                mock(Modification.class));

        Uninterruptibles.sleepUninterruptibly(4, TimeUnit.SECONDS);

        CanCommitTransaction canCommitTransaction = mock(CanCommitTransaction.class);
        doReturn("transaction-67").when(canCommitTransaction).getTransactionID();

        TestActorRef<MessageCollectorActor> sender =
                TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

        coordinator.handleCanCommit(canCommitTransaction, sender, null);

        List<Object> matching = MessageCollectorActor.getAllMatching(sender, Status.Failure.class);

        assertEquals(1, matching.size());

        verify(logger).error(anyString());
    }

    @Test
    public void testCohortEntryExplicitRemoval() throws Exception {

        Logger logger = mock(Logger.class);
        DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class);
        doReturn(com.google.common.util.concurrent.Futures.immediateFuture(true)).when(cohort).canCommit();

        ShardCommitCoordinator coordinator = new ShardCommitCoordinator(2, 100, logger , "coordinator");
        coordinator.transactionReady("transaction-67", cohort , mock(Modification.class));

        CanCommitTransaction canCommitTransaction = mock(CanCommitTransaction.class);
        doReturn("transaction-67").when(canCommitTransaction).getTransactionID();

        coordinator.removeCohortEntry("transaction-67");

        verify(logger, never()).error(anyString());
    }

}