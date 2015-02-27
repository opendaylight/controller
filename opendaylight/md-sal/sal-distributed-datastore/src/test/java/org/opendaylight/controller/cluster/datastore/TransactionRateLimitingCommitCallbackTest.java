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
import static org.mockito.Mockito.verify;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.util.concurrent.TimeUnit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
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

        for(int i=1;i<11;i++){
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }


        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.success();

        verify(actorContext).setTxCreationLimit(Matchers.doubleThat(approximately(292)));
    }

    @Test
    public void testSuccessPercentileValueZero(){

        for(int i=1;i<11;i++){
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        doReturn(TimeUnit.MILLISECONDS.toNanos(0) * 1D).when(commitSnapshot).getValue(0.1);

        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.success();

        verify(actorContext).setTxCreationLimit(Matchers.doubleThat(approximately(192)));
    }

    @Test
    public void testSuccessOnePercentileValueVeryHigh(){

        for(int i=1;i<11;i++){
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        // ten seconds
        doReturn(TimeUnit.MILLISECONDS.toNanos(10000) * 1D).when(commitSnapshot).getValue(1.0);

        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.success();

        verify(actorContext).setTxCreationLimit(Matchers.doubleThat(approximately(282)));
    }

    @Test
    public void testSuccessWithAllPercentileValueVeryHigh(){

        for(int i=1;i<11;i++){
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(10000) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.success();

        verify(actorContext).setTxCreationLimit(Matchers.doubleThat(approximately(0)));
    }

    @Test
    public void testSuccessWithRealPercentileValues(){

        for(int i=1;i<11;i++){
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(8) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        doReturn(TimeUnit.MILLISECONDS.toNanos(20) * 1D).when(commitSnapshot).getValue( 0.7);
        doReturn(TimeUnit.MILLISECONDS.toNanos(100) * 1D).when(commitSnapshot).getValue( 0.9);
        doReturn(TimeUnit.MILLISECONDS.toNanos(200) * 1D).when(commitSnapshot).getValue( 1.0);

        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.success();

        verify(actorContext).setTxCreationLimit(Matchers.doubleThat(approximately(101)));
    }


    @Test
    public void testSuccessWithoutRun(){
        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);

        try {
            commitCallback.success();
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e){

        }

        verify(actorContext, never()).setTxCreationLimit(anyDouble());

    }


    @Test
    public void testFailure(){
        TransactionRateLimitingCallback commitCallback = new TransactionRateLimitingCallback(actorContext);
        commitCallback.run();
        commitCallback.failure();

        verify(actorContext, never()).setTxCreationLimit(anyDouble());

    }

    public Matcher<Double> approximately(final double val){
        return new BaseMatcher<Double>() {
            @Override
            public boolean matches(Object o) {
                Double aDouble = (Double) o;
                return aDouble > val && aDouble < val+1;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("> " + val +" < " + (val+1));
            }
        };
    }


}