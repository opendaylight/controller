/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommonConfigTest {

    @Test
    public void testCommonConfigDefaults(){
        CommonConfig config = new CommonConfig.Builder<>("testsystem").build();

        assertNotNull(config.getActorSystemName());
        assertNotNull(config.getMailBoxCapacity());
        assertNotNull(config.getMailBoxName());
        assertNotNull(config.getMailBoxPushTimeout());
        assertNotNull(config.isMetricCaptureEnabled());
    }

    @Test
    public void testCommonConfigOverride(){

        int expectedCapacity = 123;
        String timeoutValue = "1000ms";
        CommonConfig config = new CommonConfig.Builder<>("testsystem")
                .mailboxCapacity(expectedCapacity)
                .mailboxPushTimeout(timeoutValue)
                .metricCaptureEnabled(true)
                .build();

        assertEquals(expectedCapacity, config.getMailBoxCapacity().intValue());

        FiniteDuration expectedTimeout = FiniteDuration.create(1000, TimeUnit.MILLISECONDS);
        assertEquals(expectedTimeout.toMillis(), config.getMailBoxPushTimeout().toMillis());

        assertTrue(config.isMetricCaptureEnabled());
    }
}