/*
 * Copyright (c) 2014 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.implementation.internal.ProtocolService;
import org.opendaylight.controller.sal.utils.GlobalConstants;

/**
 * Unit test for {@link ProtocolService}.
 */
public class ProtocolServiceTest {
    private static final Logger LOG =
        LoggerFactory.getLogger(ProtocolServiceTest.class);

    @Test
    public void testInstance() {
        HashSet<ProtocolService<ITestService>> set = new HashSet<>();
        TestService sv1 = new TestService();
        HashMap<String, Object> prop1 = new HashMap<>();

        ProtocolService<ITestService> ps1 =
            new ProtocolService<ITestService>(prop1, sv1);
        assertEquals(sv1, ps1.getService());
        // Default priority is 0.
        assertEquals(0, ps1.getPriority());
        assertTrue(set.add(ps1));
        assertFalse(set.add(ps1));

        // Specify the same service and priority.
        String priKey = GlobalConstants.PROTOCOLPLUGINPRIORITY.toString();
        prop1.put(priKey, Integer.valueOf(0));
        ProtocolService<ITestService> ps2 =
            new ProtocolService<ITestService>(prop1, sv1);
        assertEquals(sv1, ps2.getService());
        assertEquals(0, ps2.getPriority());
        assertEquals(ps1, ps2);
        assertFalse(set.add(ps1));

        // Specify different priority.
        prop1.put(priKey, Integer.valueOf(Integer.MAX_VALUE));
        ps2 = new ProtocolService<ITestService>(prop1, sv1);
        assertEquals(sv1, ps2.getService());
        assertEquals(Integer.MAX_VALUE, ps2.getPriority());
        assertFalse(ps1.equals(ps2));
        assertTrue(set.add(ps2));
        assertFalse(set.add(ps2));

        // Specify another service.
        TestService sv2 = new TestService();
        prop1.put(priKey, Integer.valueOf(0));
        ps2 = new ProtocolService<ITestService>(prop1, sv2);
        assertEquals(sv2, ps2.getService());
        assertEquals(0, ps2.getPriority());
        assertFalse(ps1.equals(ps2));
        assertTrue(set.add(ps2));
        assertFalse(set.add(ps2));
    }

    @Test
    public void testSetUnsetError() {
        ConcurrentMap<String, ProtocolService<ITestService>> services =
            new ConcurrentHashMap<>();
        TestService sv = new TestService();
        Map<String, Object> props = new HashMap<>();

        // null service.
        ProtocolService.set(services, props, null, LOG);
        assertTrue(services.isEmpty());

        ProtocolService.unset(services, props, null, LOG);
        assertTrue(services.isEmpty());

        // null service property.
        ProtocolService.set(services, null, sv, LOG);
        assertTrue(services.isEmpty());

        ProtocolService.unset(services, null, sv, LOG);
        assertTrue(services.isEmpty());

        // Type is not specified.
        ProtocolService.set(services, props, sv, LOG);
        assertTrue(services.isEmpty());

        ProtocolService.unset(services, props, sv, LOG);
        assertTrue(services.isEmpty());

        // null service map.
        final String typeKey = GlobalConstants.PROTOCOLPLUGINTYPE.toString();
        assertEquals(null, props.put(typeKey, "OF"));
        ProtocolService.set(null, props, sv, LOG);
        assertTrue(services.isEmpty());

        ProtocolService.unset(null, props, sv, LOG);
        assertTrue(services.isEmpty());
    }

    @Test
    public void testSetUnset() {
        ConcurrentMap<String, ProtocolService<ITestService>> serviceMap =
            new ConcurrentHashMap<>();
        ConcurrentMap<String, ProtocolService<ITestService>> expected =
            new ConcurrentHashMap<>();

        final String typeKey = GlobalConstants.PROTOCOLPLUGINTYPE.toString();
        final String priKey = GlobalConstants.PROTOCOLPLUGINPRIORITY.toString();
        final String[] protocols = {"OF", "PE", "PK"};
        final int basePri = 0;
        final int loop = 5;

        // Should override the service if higher priority is specified.
        for (String proto: protocols) {
            for (int pri = basePri - loop + 1; pri <= basePri; pri++) {
                TestService sv = new TestService();
                Map<String, Object> props = new HashMap<>();
                assertEquals(null, props.put(typeKey, proto));
                assertEquals(null, props.put(priKey, Integer.valueOf(pri)));
                ProtocolService.set(serviceMap, props, sv, LOG);

                ProtocolService<ITestService> service = serviceMap.get(proto);
                assertNotNull(service);
                assertEquals(sv, service.getService());
                assertEquals(pri, service.getPriority());

                ProtocolService<ITestService> service1 =
                    new ProtocolService<ITestService>(props, sv);
                expected.put(proto, service1);
                assertEquals(expected, serviceMap);

                // Unset service request should be ignored if different
                // parameters are specified.
                TestService another = new TestService();
                ProtocolService.unset(serviceMap, props, another, LOG);
                assertEquals(expected, serviceMap);

                props.put(priKey, Integer.valueOf(Integer.MAX_VALUE));
                ProtocolService.unset(serviceMap, props, sv, LOG);
                assertEquals(expected, serviceMap);
            }
        }

        // Should reject the set service request if lower priority is specified.
        for (String proto: protocols) {
            for (int pri = basePri - loop; pri < basePri; pri++) {
                TestService sv = new TestService();
                Map<String, Object> props = new HashMap<>();
                assertEquals(null, props.put(typeKey, proto));
                assertEquals(null, props.put(priKey, Integer.valueOf(pri)));
                ProtocolService.set(serviceMap, props, sv, LOG);
                assertEquals(expected, serviceMap);
            }
        }

        // Unset protocol services.
        for (String proto: protocols) {
            ProtocolService<ITestService> service = expected.remove(proto);
            assertNotNull(service);

            ITestService sv = service.getService();
            Map<String, Object> props = new HashMap<>();
            assertEquals(null, props.put(typeKey, proto));
            assertEquals(null, props.put(priKey, Integer.valueOf(basePri)));
            ProtocolService.unset(serviceMap, props, sv, LOG);
            assertEquals(expected, serviceMap);

            // Should be ignored if the specified service does not exist.
            ProtocolService.unset(serviceMap, props, sv, LOG);
            assertEquals(expected, serviceMap);
        }

        assertTrue(serviceMap.isEmpty());
    }
}

interface ITestService {
}

class TestService implements ITestService {
}
