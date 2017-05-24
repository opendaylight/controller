/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static org.junit.Assert.assertSame;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for TimeoutNow.
 *
 * @author Thomas Pantelis
 */
public class TimeoutNowTest {

    @Test
    public void test() {
        TimeoutNow cloned = (TimeoutNow) SerializationUtils.clone(TimeoutNow.INSTANCE);
        assertSame("Cloned instance", TimeoutNow.INSTANCE, cloned);
    }
}
