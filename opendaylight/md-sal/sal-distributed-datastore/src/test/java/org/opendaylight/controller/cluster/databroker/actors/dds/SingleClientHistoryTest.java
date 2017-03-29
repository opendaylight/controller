/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

public class SingleClientHistoryTest extends AbstractClientHistoryTest<SingleClientHistory> {
    private ActorSystem system;
    private AbstractDataStoreClientBehavior behavior;
    private ClientActorContext clientActorContext;
    private SingleClientHistory object;

    @Mock
    private AbstractTransactionCommitCohort cohort;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        system = ActorSystem.apply();

        final TestProbe clientContextProbe = new TestProbe(system, "client");
        final TestProbe actorContextProbe = new TestProbe(system, "actor-context");
        clientActorContext = AccessClientUtil.createClientActorContext(
                system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        final ActorContext actorContextMock = createActorContextMock(system, actorContextProbe.ref());
        behavior = new SimpleDataStoreClientBehavior(clientActorContext, actorContextMock, SHARD_NAME);

        object = new SingleClientHistory(behavior, HISTORY_ID);
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
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
    public void testDoCreateTransaction() throws Exception {
        final ClientTransaction clientTransaction = object().doCreateTransaction();
        Assert.assertEquals(object().getIdentifier(), clientTransaction.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testCreateHistoryProxy() throws Exception {
        final AbstractClientConnection<ShardBackendInfo> clientConnection = behavior.getConnection(0L);
        final ProxyHistory historyProxy = object().createHistoryProxy(HISTORY_ID, clientConnection);
        Assert.assertEquals(object().getIdentifier(), historyProxy.getIdentifier());
    }

    @Override
    @Test
    public void testDoCreateSnapshot() throws Exception {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        Assert.assertEquals(new TransactionIdentifier(object().getIdentifier(), object().nextTx()).getHistoryId(),
                clientSnapshot.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionComplete() throws Exception {
        final ClientTransaction transaction = object().createTransaction();
        // make transaction ready
        object().onTransactionReady(transaction, cohort);
        // complete transaction
        object().onTransactionComplete(transaction.getIdentifier());
        // it is possible to make transaction ready again
        final AbstractTransactionCommitCohort result = object().onTransactionReady(transaction, cohort);
        Assert.assertEquals(result, cohort);
    }

    @Override
    @Test
    public void testOnTransactionAbort() throws Exception {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        Assert.assertTrue(clientSnapshot.abort());
    }

    @Override
    @Test
    public void testOnTransactionReady() throws Exception {
        final AbstractTransactionCommitCohort result = object().onTransactionReady(
                object().createTransaction(), cohort);
        Assert.assertEquals(result, cohort);
    }

    @Override
    @Test(expected = IllegalStateException.class)
    public void testOnTransactionReadyDuplicate() throws Exception {
        final ClientTransaction transaction = object().createTransaction();
        object().onTransactionReady(transaction, cohort);
        object().onTransactionReady(transaction, cohort);
    }
}