/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Status;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.InternalCommand;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import scala.concurrent.Promise;

public abstract class AbstractDataStoreClientBehaviorTest {

    protected static final String SHARD = "default";
    private static final String PERSISTENCE_ID = "per-1";

    private ActorSystem system;
    private ClientActorContext clientContext;
    private TestProbe clientActorProbe;
    private TestProbe actorContextProbe;
    private AbstractDataStoreClientBehavior behavior;

    @Before
    public void setUp() throws Exception {
        system = ActorSystem.apply();
        clientActorProbe = new TestProbe(system, "client");
        actorContextProbe = new TestProbe(system, "actor-context");
        final ActorContext context = createActorContextMock(system, actorContextProbe.ref());
        clientContext =
                AccessClientUtil.createClientActorContext(system, clientActorProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        behavior = createBehavior(clientContext, context);
    }

    protected abstract AbstractDataStoreClientBehavior createBehavior(ClientActorContext clientContext,
                                                                      ActorContext context);

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void testResolveShardForPath() throws Exception {
        Assert.assertEquals(0L, behavior.resolveShardForPath(YangInstanceIdentifier.EMPTY).longValue());
    }

    @Test
    public void testHaltClient() throws Exception {
        behavior.haltClient(new RuntimeException());
    }

    @Test
    public void testOnCommand() throws Exception {
        final TestProbe probe = new TestProbe(system);
        final GetClientRequest request = new GetClientRequest(probe.ref());
        final AbstractDataStoreClientBehavior nextBehavior = behavior.onCommand(request);
        final Status.Success success = probe.expectMsgClass(Status.Success.class);
        Assert.assertEquals(behavior, success.status());
        Assert.assertSame(behavior, nextBehavior);
    }

    @Test
    public void testOnCommandUnhandled() throws Exception {
        final AbstractDataStoreClientBehavior nextBehavior = behavior.onCommand("unhandled");
        Assert.assertSame(behavior, nextBehavior);
    }

    @Test
    public void testCreateLocalHistory() throws Exception {
        final ClientLocalHistory history = behavior.createLocalHistory();
        Assert.assertEquals(behavior.getIdentifier(), history.getIdentifier().getClientId());
    }

    @Test
    public void testCreateTransaction() throws Exception {
        final ClientTransaction transaction = behavior.createTransaction();
        Assert.assertEquals(behavior.getIdentifier(), transaction.getIdentifier().getHistoryId().getClientId());
    }

    @Test
    public void testCreateSnapshot() throws Exception {
        final ClientSnapshot snapshot = behavior.createSnapshot();
        Assert.assertEquals(behavior.getIdentifier(), snapshot.getIdentifier().getHistoryId().getClientId());
    }

    @Test
    public void testClose() throws Exception {
        behavior.close();
        final InternalCommand<ShardBackendInfo> internalCommand =
                clientActorProbe.expectMsgClass(InternalCommand.class);
        internalCommand.execute(behavior);
        try {
            behavior.createLocalHistory();
            Assert.fail("Behavior is closed and shouldn't allow to create new history.");
        } catch (final IllegalStateException e) {
            //ok
        }
    }

    @Test
    public void testGetIdentifier() throws Exception {
        Assert.assertEquals(CLIENT_ID, behavior.getIdentifier());
    }

    @Test
    public void testGetConnection() throws Exception {
        //set up data tree mock
        final CursorAwareDataTreeModification modification = mock(CursorAwareDataTreeModification.class);
        when(modification.readNode(YangInstanceIdentifier.EMPTY)).thenReturn(com.google.common.base.Optional.absent());
        final DataTreeSnapshot snapshot = mock(DataTreeSnapshot.class);
        when(snapshot.newModification()).thenReturn(modification);
        final DataTree dataTree = mock(DataTree.class);
        when(dataTree.takeSnapshot()).thenReturn(snapshot);

        final TestProbe backendProbe = new TestProbe(system, "backend");
        final long shard = 0L;
        behavior.createTransaction().read(YangInstanceIdentifier.EMPTY);
        final AbstractClientConnection<ShardBackendInfo> connection = behavior.getConnection(shard);
        //check cached connection for same shard
        Assert.assertSame(connection, behavior.getConnection(shard));

        final ConnectClientRequest connectClientRequest = actorContextProbe.expectMsgClass(ConnectClientRequest.class);
        Assert.assertEquals(CLIENT_ID, connectClientRequest.getTarget());
        final long sequence = 0L;
        Assert.assertEquals(sequence, connectClientRequest.getSequence());
        actorContextProbe.reply(new ConnectClientSuccess(CLIENT_ID, sequence, backendProbe.ref(),
                Collections.emptyList(), dataTree, 3));
        Assert.assertEquals(clientActorProbe.ref(), connection.localActor());
        //capture and execute command passed to client context
        final InternalCommand<ShardBackendInfo> command = clientActorProbe.expectMsgClass(InternalCommand.class);
        command.execute(behavior);
        //check, whether command was reaplayed
        verify(modification).readNode(YangInstanceIdentifier.EMPTY);
    }

    private static ActorContext createActorContextMock(final ActorSystem system, final ActorRef actor) {
        final ActorContext mock = mock(ActorContext.class);
        final Promise<PrimaryShardInfo> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        when(mock.findPrimaryShardAsync(SHARD)).thenReturn(promise.future());
        return mock;
    }

}