/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Status;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.InternalCommand;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import scala.concurrent.Promise;

public abstract class AbstractDataStoreClientBehaviorTest {

    protected static final String SHARD = "default";
    private static final String PERSISTENCE_ID = "per-1";

    private ActorSystem system;
    private ClientActorContext clientContext;
    private TestProbe clientActorProbe;
    private TestProbe actorContextProbe;
    private AbstractDataStoreClientBehavior behavior;
    private ActorUtils util;

    @Before
    public void setUp() {
        system = ActorSystem.apply();
        clientActorProbe = new TestProbe(system, "client");
        actorContextProbe = new TestProbe(system, "actor-context");
        util = createActorContextMock(system, actorContextProbe.ref());
        clientContext =
                AccessClientUtil.createClientActorContext(system, clientActorProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        behavior = createBehavior(clientContext, util);
    }

    @SuppressWarnings("checkstyle:hiddenField")
    protected abstract AbstractDataStoreClientBehavior createBehavior(ClientActorContext clientContext,
                                                                      ActorUtils context);

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testResolveShardForPath() {
        assertEquals(0L, behavior.resolveShardForPath(YangInstanceIdentifier.empty()).longValue());
    }

    @Test
    public void testHaltClient() {
        behavior.haltClient(new RuntimeException());
    }

    @Test
    public void testOnCommand() {
        final TestProbe probe = new TestProbe(system);
        final GetClientRequest request = new GetClientRequest(probe.ref());
        final AbstractDataStoreClientBehavior nextBehavior = behavior.onCommand(request);
        final Status.Success success = probe.expectMsgClass(Status.Success.class);
        assertEquals(behavior, success.status());
        assertSame(behavior, nextBehavior);
    }

    @Test
    public void testOnCommandUnhandled() {
        final AbstractDataStoreClientBehavior nextBehavior = behavior.onCommand("unhandled");
        assertSame(behavior, nextBehavior);
    }

    @Test
    public void testCreateLocalHistory() {
        final ClientLocalHistory history = behavior.createLocalHistory();
        assertEquals(behavior.getIdentifier(), history.getIdentifier().getClientId());
    }

    @Test
    public void testCreateTransaction() {
        final ClientTransaction transaction = behavior.createTransaction();
        assertEquals(behavior.getIdentifier(), transaction.getIdentifier().getHistoryId().getClientId());
    }

    @Test
    public void testCreateSnapshot() {
        final ClientSnapshot snapshot = behavior.createSnapshot();
        assertEquals(behavior.getIdentifier(), snapshot.getIdentifier().getHistoryId().getClientId());
    }

    @Test
    public void testClose() {
        behavior.close();
        final InternalCommand<ShardBackendInfo> internalCommand =
                clientActorProbe.expectMsgClass(InternalCommand.class);
        internalCommand.execute(behavior);

        assertThrows(IllegalStateException.class, () -> behavior.createLocalHistory());
    }

    @Test
    public void testGetIdentifier() {
        assertEquals(CLIENT_ID, behavior.getIdentifier());
    }

    @Test
    public void testGetConnection() {
        final var datastoreContext = mock(DatastoreContext.class);
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(util).getDatastoreContext();

        //set up data tree mock
        final CursorAwareDataTreeModification modification = mock(CursorAwareDataTreeModification.class);
        doReturn(Optional.empty()).when(modification).readNode(YangInstanceIdentifier.empty());
        final DataTreeSnapshot snapshot = mock(DataTreeSnapshot.class);
        doReturn(modification).when(snapshot).newModification();
        final DataTree dataTree = mock(DataTree.class);
        doReturn(snapshot).when(dataTree).takeSnapshot();

        final TestProbe backendProbe = new TestProbe(system, "backend");
        final long shard = 0L;

        behavior.createTransaction().read(YangInstanceIdentifier.empty());
        final AbstractClientConnection<ShardBackendInfo> connection = behavior.getConnection(shard);
        //check cached connection for same shard
        assertSame(connection, behavior.getConnection(shard));

        final ConnectClientRequest connectClientRequest = actorContextProbe.expectMsgClass(ConnectClientRequest.class);
        assertEquals(CLIENT_ID, connectClientRequest.getTarget());
        final long sequence = 0L;
        assertEquals(sequence, connectClientRequest.getSequence());
        actorContextProbe.reply(new ConnectClientSuccess(CLIENT_ID, sequence, backendProbe.ref(), List.of(), dataTree,
                3));
        assertEquals(clientActorProbe.ref(), connection.localActor());
        //capture and execute command passed to client context
        final InternalCommand<ShardBackendInfo> command = clientActorProbe.expectMsgClass(InternalCommand.class);
        command.execute(behavior);
        //check, whether command was reaplayed
        verify(modification).readNode(YangInstanceIdentifier.empty());
    }

    private static ActorUtils createActorContextMock(final ActorSystem system, final ActorRef actor) {
        final ActorUtils mock = mock(ActorUtils.class);
        final Promise<PrimaryShardInfo> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        doReturn(promise.future()).when(mock).findPrimaryShardAsync(SHARD);
        return mock;
    }
}
