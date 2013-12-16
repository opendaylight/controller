/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractLockedPlatformMBeanServerTest;

import javax.management.ObjectName;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
            throw Throwables.propagate(lastException);
        }
    }

    @Test
    public void testQuotation() throws Exception {
        String serviceQName = "(namespace?revision=r)qname";
        String refName = "refName";
        String transaction = "transaction";
        ObjectName serviceReferenceON = ObjectNameUtil.createTransactionServiceON(transaction, serviceQName, refName);
        assertFalse(serviceReferenceON.isPattern());
        assertEquals(serviceQName, ObjectNameUtil.getServiceQName(serviceReferenceON));
        assertEquals(refName, ObjectNameUtil.getReferenceName(serviceReferenceON));
        assertEquals(transaction, ObjectNameUtil.getTransactionName(serviceReferenceON));

        serviceReferenceON = ObjectNameUtil.createReadOnlyServiceON(serviceQName, refName);
        assertFalse(serviceReferenceON.isPattern());
        assertEquals(serviceQName, ObjectNameUtil.getServiceQName(serviceReferenceON));
        assertEquals(refName, ObjectNameUtil.getReferenceName(serviceReferenceON));
        assertEquals(null, ObjectNameUtil.getTransactionName(serviceReferenceON));

    }
}
