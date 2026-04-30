/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;

import java.util.List;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Status;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.InternalCommand;
import org.opendaylight.controller.cluster.access.client.TerminatedException;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import scala.concurrent.Future;

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
        assertEquals(0, behavior.resolveShardForPath(YangInstanceIdentifier.of()));
    }

    @Test
    public void testHaltClient() {
        behavior.haltClient(new RuntimeException());
    }

    @Test
    public void testOnCommand() {
        final var probe = new TestProbe(system);
        final var request = new GetClientRequest(probe.ref());
        final var nextBehavior = behavior.onCommand(request);
        assertEquals(behavior,  probe.expectMsgClass(Status.Success.class).status());
        assertSame(behavior, nextBehavior);
    }

    @Test
    public void testOnCommandUnhandled() {
        final var nextBehavior = behavior.onCommand("unhandled");
        assertSame(behavior, nextBehavior);
    }

    @Test
    public void testCreateLocalHistory() {
        final var history = behavior.createLocalHistory();
        assertEquals(behavior.getIdentifier(), history.getIdentifier().getClientId());
    }

    @Test
    public void testCreateTransaction() {
        final var transaction = behavior.createTransaction();
        assertEquals(behavior.getIdentifier(), transaction.getIdentifier().getHistoryId().getClientId());
    }

    @Test
    public void testCreateSnapshot() {
        final var snapshot = behavior.createSnapshot();
        assertEquals(behavior.getIdentifier(), snapshot.getIdentifier().getHistoryId().getClientId());
    }

    @Test
    public void testClose() {
        behavior.close();
        behavior.close();

        // idempotent: there should be only one message
        final var internalCommand = clientActorProbe.expectMsgClass(InternalCommand.class);
        clientActorProbe.expectNoMessage();

        // abort transition after close
        assertNull(behavior.aborted());
        internalCommand.execute(behavior);
        assertInstanceOf(TerminatedException.class, behavior.aborted());

        final var ex = assertThrows(IllegalStateException.class, () -> behavior.createLocalHistory());
        final var cause = assertInstanceOf(TerminatedException.class, ex.getCause());
        assertEquals(cause.toString(), ex.getMessage());
    }

    @Test
    public void testNextHistoryId() {
        // starts at one
        assertEquals(1, behavior.nextHistoryId());
        // increments by one
        assertEquals(2, behavior.nextHistoryId());
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
        final var modification = mock(CursorAwareDataTreeModification.class);
        doReturn(Optional.empty()).when(modification).readNode(YangInstanceIdentifier.of());
        final var snapshot = mock(DataTreeSnapshot.class);
        doReturn(modification).when(snapshot).newModification();
        final var dataTree = mock(DataTree.class);
        doReturn(snapshot).when(dataTree).takeSnapshot();

        final var backendProbe = new TestProbe(system, "backend");
        final long shard = 0L;

        behavior.createTransaction().read(YangInstanceIdentifier.of());
        final var connection = behavior.getConnection(shard);
        //check cached connection for same shard
        assertSame(connection, behavior.getConnection(shard));

        final var connectClientRequest = actorContextProbe.expectMsgClass(ConnectClientRequest.class);
        assertEquals(CLIENT_ID, connectClientRequest.getTarget());
        final long sequence = 0L;
        assertEquals(sequence, connectClientRequest.getSequence());
        actorContextProbe.reply(new ConnectClientSuccess(CLIENT_ID, sequence, backendProbe.ref(), List.of(), dataTree,
                3));
        assertEquals(clientActorProbe.ref(), connection.localActor());
        //capture and execute command passed to client context
        final var command = clientActorProbe.expectMsgClass(InternalCommand.class);
        command.execute(behavior);
        //check, whether command was reaplayed
        verify(modification).readNode(YangInstanceIdentifier.of());
    }

    private static ActorUtils createActorContextMock(final ActorSystem system, final ActorRef actor) {
        final var actorUtils = mock(ActorUtils.class);
        doReturn(ExecutionContexts.global()).when(actorUtils).getClientDispatcher();
        doReturn(Future.successful(new PrimaryShardInfo(system.actorSelection(actor.path()), (short) 0)))
            .when(actorUtils).findPrimaryShardAsync(SHARD);
        return actorUtils;
    }
}
