/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.dom.api.LeaderLocation;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListener;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListenerRegistration;
import org.opendaylight.controller.cluster.raft.LeadershipTransferFailedException;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

@Deprecated(forRemoval = true)
public class CDSShardAccessImplTest extends AbstractActorTest {

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private CDSShardAccessImpl shardAccess;
    private ActorUtils context;

    @Before
    public void setUp() {
        context = mock(ActorUtils.class);
        final DatastoreContext datastoreContext = DatastoreContext.newBuilder().build();
        doReturn(Optional.of(getSystem().deadLetters())).when(context).findLocalShard(any());
        doReturn(datastoreContext).when(context).getDatastoreContext();
        doReturn(getSystem()).when(context).getActorSystem();
        shardAccess = new CDSShardAccessImpl(TEST_ID, context);
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testRegisterLeaderLocationListener() {
        final LeaderLocationListener listener1 = mock(LeaderLocationListener.class);

        // first registration should be OK
        shardAccess.registerLeaderLocationListener(listener1);

        // second registration should fail with IllegalArgumentEx
        try {
            shardAccess.registerLeaderLocationListener(listener1);
            fail("Should throw exception");
        } catch (final Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        // null listener registration should fail with NPE
        try {
            shardAccess.registerLeaderLocationListener(null);
            fail("Should throw exception");
        } catch (final Exception e) {
            assertTrue(e instanceof NullPointerException);
        }

        // registering listener on closed shard access should fail with IllegalStateEx
        final LeaderLocationListener listener2 = mock(LeaderLocationListener.class);
        shardAccess.close();
        try {
            shardAccess.registerLeaderLocationListener(listener2);
            fail("Should throw exception");
        } catch (final Exception ex) {
            assertTrue(ex instanceof IllegalStateException);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testOnLeaderLocationChanged() {
        final LeaderLocationListener listener1 = mock(LeaderLocationListener.class);
        doThrow(new RuntimeException("Failed")).when(listener1).onLeaderLocationChanged(any());
        final LeaderLocationListener listener2 = mock(LeaderLocationListener.class);
        doNothing().when(listener2).onLeaderLocationChanged(any());
        final LeaderLocationListener listener3 = mock(LeaderLocationListener.class);
        doNothing().when(listener3).onLeaderLocationChanged(any());

        final LeaderLocationListenerRegistration<?> reg1 = shardAccess.registerLeaderLocationListener(listener1);
        final LeaderLocationListenerRegistration<?> reg2 = shardAccess.registerLeaderLocationListener(listener2);
        final LeaderLocationListenerRegistration<?> reg3 = shardAccess.registerLeaderLocationListener(listener3);

        // Error in listener1 should not affect dispatching change to other listeners
        shardAccess.onLeaderLocationChanged(LeaderLocation.LOCAL);
        verify(listener1).onLeaderLocationChanged(eq(LeaderLocation.LOCAL));
        verify(listener2).onLeaderLocationChanged(eq(LeaderLocation.LOCAL));
        verify(listener3).onLeaderLocationChanged(eq(LeaderLocation.LOCAL));

        // Closed listeners shouldn't see new leader location changes
        reg1.close();
        reg2.close();
        shardAccess.onLeaderLocationChanged(LeaderLocation.REMOTE);
        verify(listener3).onLeaderLocationChanged(eq(LeaderLocation.REMOTE));
        verifyNoMoreInteractions(listener1);
        verifyNoMoreInteractions(listener2);

        // Closed shard access should not dispatch any new events
        shardAccess.close();
        shardAccess.onLeaderLocationChanged(LeaderLocation.UNKNOWN);
        verifyNoMoreInteractions(listener1);
        verifyNoMoreInteractions(listener2);
        verifyNoMoreInteractions(listener3);

        reg3.close();
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testGetShardIdentifier() {
        assertEquals(shardAccess.getShardIdentifier(), TEST_ID);

        // closed shard access should throw illegal state
        shardAccess.close();
        try {
            shardAccess.getShardIdentifier();
            fail("Exception expected");
        } catch (final Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testGetLeaderLocation() {
        // new shard access does not know anything about leader location
        assertEquals(shardAccess.getLeaderLocation(), LeaderLocation.UNKNOWN);

        // we start getting leader location changes notifications
        shardAccess.onLeaderLocationChanged(LeaderLocation.LOCAL);
        assertEquals(shardAccess.getLeaderLocation(), LeaderLocation.LOCAL);

        shardAccess.onLeaderLocationChanged(LeaderLocation.REMOTE);
        shardAccess.onLeaderLocationChanged(LeaderLocation.UNKNOWN);
        assertEquals(shardAccess.getLeaderLocation(), LeaderLocation.UNKNOWN);

        // closed shard access throws illegal state
        shardAccess.close();
        try {
            shardAccess.getLeaderLocation();
            fail("Should have failed with IllegalStateEx");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testMakeLeaderLocal() throws Exception {
        final FiniteDuration timeout = new FiniteDuration(5, TimeUnit.SECONDS);
        final ActorRef localShardRef = mock(ActorRef.class);
        final Future<ActorRef> localShardRefFuture = Futures.successful(localShardRef);
        doReturn(localShardRefFuture).when(context).findLocalShardAsync(any());

        // MakeLeaderLocal will reply with success
        doReturn(Futures.successful(null)).when(context).executeOperationAsync((ActorRef) any(), any(), any());
        doReturn(getSystem().dispatcher()).when(context).getClientDispatcher();
        assertEquals(waitOnAsyncTask(shardAccess.makeLeaderLocal(), timeout), null);

        // MakeLeaderLocal will reply with failure
        doReturn(Futures.failed(new LeadershipTransferFailedException("Failure")))
                .when(context).executeOperationAsync((ActorRef) any(), any(), any());

        try {
            waitOnAsyncTask(shardAccess.makeLeaderLocal(), timeout);
            fail("makeLeaderLocal operation should not be successful");
        } catch (final Exception e) {
            assertTrue(e instanceof LeadershipTransferFailedException);
        }

        // we don't even find local shard
        doReturn(Futures.failed(new LocalShardNotFoundException("Local shard not found")))
                .when(context).findLocalShardAsync(any());

        try {
            waitOnAsyncTask(shardAccess.makeLeaderLocal(), timeout);
            fail("makeLeaderLocal operation should not be successful");
        } catch (final Exception e) {
            assertTrue(e instanceof LeadershipTransferFailedException);
            assertTrue(e.getCause() instanceof LocalShardNotFoundException);
        }

        // closed shard access should throw IllegalStateEx
        shardAccess.close();
        try {
            shardAccess.makeLeaderLocal();
            fail("Should have thrown IllegalStateEx. ShardAccess is closed");
        } catch (final Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }
}