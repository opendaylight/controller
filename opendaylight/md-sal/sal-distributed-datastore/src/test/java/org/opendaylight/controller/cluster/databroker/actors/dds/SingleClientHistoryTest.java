/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SingleClientHistoryTest extends AbstractClientHistoryTest<SingleClientHistory> {
    private ActorSystem system;
    private AbstractDataStoreClientBehavior behavior;
    private ClientActorContext clientActorContext;
    private SingleClientHistory object;

    @Mock
    private AbstractTransactionCommitCohort cohort;

    @Before
    public void setUp() {
        system = ActorSystem.apply();

        final var clientContextProbe = new TestProbe(system, "client");
        final var actorContextProbe = new TestProbe(system, "actor-context");
        clientActorContext = AccessClientUtil.createClientActorContext(
                system, clientContextProbe.ref(), TestUtils.CLIENT_ID, PERSISTENCE_ID);
        final var actorUtilsMock = createActorUtilsMock(system, actorContextProbe.ref());
        behavior = new SimpleDataStoreClientBehavior(clientActorContext, actorUtilsMock, SHARD_NAME);

        object = new SingleClientHistory(behavior, HISTORY_ID);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Override
    protected SingleClientHistory object() {
        return object;
    }

    @Override
    protected ClientActorContext clientActorContext() {
        return clientActorContext;
    }

    @Override
    @Test
    public void testDoCreateTransaction() {
        final ClientTransaction clientTransaction = object().doCreateTransaction();
        assertEquals(object().getIdentifier(), clientTransaction.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testCreateHistoryProxy() {
        final var clientConnection = behavior.getConnection(0L);
        final var historyProxy = object().createHistoryProxy(HISTORY_ID, clientConnection);
        assertEquals(object().getIdentifier(), historyProxy.getIdentifier());
    }

    @Override
    @Test
    public void testDoCreateSnapshot() {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        assertEquals(new TransactionIdentifier(object().getIdentifier(), object().nextTx()).getHistoryId(),
                clientSnapshot.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionComplete() {
        final ClientTransaction transaction = object().createTransaction();
        // make transaction ready
        object().onTransactionReady(transaction, cohort);
        // complete transaction
        object().onTransactionComplete(transaction.getIdentifier());
        // it is possible to make transaction ready again
        final AbstractTransactionCommitCohort result = object().onTransactionReady(transaction, cohort);
        assertEquals(result, cohort);
    }

    @Override
    @Test
    public void testOnTransactionAbort() {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        assertTrue(clientSnapshot.abort());
    }

    @Override
    @Test
    public void testOnTransactionReady() {
        final AbstractTransactionCommitCohort result = object().onTransactionReady(
                object().createTransaction(), cohort);
        assertEquals(result, cohort);
    }

    @Override
    @Test
    public void testOnTransactionReadyDuplicate() {
        final ClientTransaction transaction = object().createTransaction();
        object().onTransactionReady(transaction, cohort);
        final IllegalStateException ise = assertThrows(IllegalStateException.class,
            () -> object().onTransactionReady(transaction, cohort));
        assertThat(ise.getMessage(), startsWith("Duplicate cohort "));
    }
}
