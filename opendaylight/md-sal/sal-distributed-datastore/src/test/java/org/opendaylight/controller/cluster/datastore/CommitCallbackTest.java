/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

public class CommitCallbackTest {

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
        for(int i=1;i<11;i++){
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

    }

    @Test
    public void testSuccess(){
        CommitCallback commitCallback = new CommitCallback(actorContext);
        commitCallback.run();
        commitCallback.success();

        // Verify that the creation limit was changed to 292.89682539682536 (based on setup)
        verify(actorContext, timeout(5000)).setTxCreationLimit(292.89682539682536);

    }

    @Test
    public void testSuccessWithoutRun(){
        CommitCallback commitCallback = new CommitCallback(actorContext);

        try {
            commitCallback.success();
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e){

        }

        verify(actorContext, never()).setTxCreationLimit(anyDouble());

    }


    @Test
    public void testFailure(){
        CommitCallback commitCallback = new CommitCallback(actorContext);
        commitCallback.run();
        commitCallback.failure();

        verify(actorContext, never()).setTxCreationLimit(anyDouble());

    }
}