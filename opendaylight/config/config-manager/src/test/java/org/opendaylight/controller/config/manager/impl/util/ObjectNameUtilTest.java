/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import java.util.Set;

import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.manager.impl.AbstractLockedPlatformMBeanServerTest;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

public class ObjectNameUtilTest extends AbstractLockedPlatformMBeanServerTest {
    private Set<ObjectName> unregisterONs;

    @Before
    public void initUnregisterList() {
        unregisterONs = Sets.newHashSet();
    }

    @After
    public void unregisterONs() {
        Exception lastException = null;
        for (ObjectName on : unregisterONs) {
            try {
                platformMBeanServer.unregisterMBean(on);
            } catch (Exception e) {
                lastException = e;
            }
        }
        if (lastException != null) {
            Throwables.propagate(lastException);
        }
    }
}
