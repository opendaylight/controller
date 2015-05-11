/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

public class TransactionRateLimitingCommitCallbackTest {
    @Mock
    public ActorContext actorContext;

    @Mock
    public DatastoreContext datastoreContext;

    @Mock
    public Timer commitTimer;

    @Mock
    private Timer.Context commitTimerContext;

    @Mock
    private Snapshot commitSnapshot;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        doReturn(datastoreContext).when(actorContext).getDatastoreContext();
        doReturn(30).when(datastoreContext).getShardTransactionCommitTimeoutInSeconds();
        doReturn(commitTimer).when(actorContext).getOperationTimer("commit");
        doReturn(commitTimerContext).when(commitTimer).time();
        doReturn(commitSnapshot).when(commitTimer).getSnapshot();
    }

    @Test
    public void testSuccess(){
        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.success();

        verify(commitTimerContext).stop();
    }

    @Test
    public void testSuccessWithoutRun(){
        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);

        try {
            commitCallback.success();
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e){

        }
        verify(commitTimerContext, never()).stop();
    }


    @Test
    public void testFailure(){
        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.failure();

        verify(commitTimerContext, never()).stop();
    }

}