/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;

public class ObjectNameUtilTest {

    private String moduleName;
    private String instanceName;

    @Before
    public void setUp() throws Exception {
        moduleName = "module";
        instanceName = "instance";
    }

    @Test
    public void testServiceReferenceName() throws Exception {
        String serviceQName = "(namespace?revision=r)qname";
        String refName = "refName";
        String transaction = "transaction";

        ObjectName serviceReferenceON = ObjectNameUtil.createTransactionServiceON(transaction, serviceQName, refName);
        ObjectNameUtil.checkType(serviceReferenceON, ObjectNameUtil.TYPE_SERVICE_REFERENCE);

        assertFalse(serviceReferenceON.isPattern());
        assertEquals(serviceQName, ObjectNameUtil.getServiceQName(serviceReferenceON));
        assertEquals(refName, ObjectNameUtil.getReferenceName(serviceReferenceON));
        assertEquals(transaction, ObjectNameUtil.getTransactionName(serviceReferenceON));
        assertEquals(ObjectNameUtil.createReadOnlyServiceON(serviceQName, refName), ObjectNameUtil.withoutTransactionName(serviceReferenceON));

        serviceReferenceON = ObjectNameUtil.createReadOnlyServiceON(serviceQName, refName);
        assertFalse(serviceReferenceON.isPattern());
        assertEquals(serviceQName, ObjectNameUtil.getServiceQName(serviceReferenceON));
        assertEquals(refName, ObjectNameUtil.getReferenceName(serviceReferenceON));
        assertEquals(null, ObjectNameUtil.getTransactionName(serviceReferenceON));
    }

    @Test
    public void testModuleName() throws Exception {
        String txName = "transaction";

        ObjectName on = ObjectNameUtil.createTransactionModuleON(txName, moduleName, instanceName);

        ObjectNameUtil.checkDomain(on);
        ObjectNameUtil.checkType(on, ObjectNameUtil.TYPE_MODULE);

        assertFalse(on.isPattern());
        assertEquals(moduleName, ObjectNameUtil.getFactoryName(on));
        assertEquals(instanceName, ObjectNameUtil.getInstanceName(on));
        assertEquals(txName, ObjectNameUtil.getTransactionName(on));
        assertEquals(4, ObjectNameUtil.getAdditionalProperties(on).size());

        ObjectName withoutTx = ObjectNameUtil.withoutTransactionName(on);
        assertEquals(ObjectNameUtil.createReadOnlyModuleON(moduleName, instanceName), withoutTx);
        assertEquals(moduleName, ObjectNameUtil.getFactoryName(withoutTx));
        assertEquals(instanceName, ObjectNameUtil.getInstanceName(withoutTx));
        assertEquals(null, ObjectNameUtil.getTransactionName(withoutTx));
        assertEquals(on, ObjectNameUtil.withTransactionName(withoutTx, txName));

        ObjectName pattern = ObjectNameUtil.createModulePattern(moduleName, null);
        assertPattern(withoutTx, pattern);
        pattern = ObjectNameUtil.createModulePattern(moduleName, null, txName);
        assertPattern(on, pattern);
    }

    private void assertPattern(final ObjectName test, final ObjectName pattern) {
        assertTrue(pattern.isPattern());
        assertTrue(pattern.apply(test));
    }

    @Test
    public void testRuntimeBeanName() throws Exception {

        Map<String, String> properties = Maps.newHashMap();
        properties.put("p1", "value");
        properties.put("p2", "value2");

        ObjectName on = ObjectNameUtil.createRuntimeBeanName(moduleName, instanceName, properties);

        ObjectNameUtil.checkDomain(on);
        ObjectNameUtil.checkTypeOneOf(on, ObjectNameUtil.TYPE_RUNTIME_BEAN);

        assertFalse(on.isPattern());
        assertEquals(moduleName, ObjectNameUtil.getFactoryName(on));
        assertEquals(instanceName, ObjectNameUtil.getInstanceName(on));
        assertEquals(2, ObjectNameUtil.getAdditionalPropertiesOfRuntimeBeanName(on).size());
        assertTrue(ObjectNameUtil.getAdditionalPropertiesOfRuntimeBeanName(on).containsKey("p1"));
        assertEquals("value", ObjectNameUtil.getAdditionalPropertiesOfRuntimeBeanName(on).get("p1"));
        assertTrue(ObjectNameUtil.getAdditionalProperties(on).containsKey("p2"));
        assertEquals("value2", ObjectNameUtil.getAdditionalPropertiesOfRuntimeBeanName(on).get("p2"));

        ObjectName pattern = ObjectNameUtil.createRuntimeBeanPattern(null, instanceName);
        assertPattern(on, pattern);
    }

    @Test
    public void testModuleIdentifier() throws Exception {
        ModuleIdentifier mi = new ModuleIdentifier(moduleName, instanceName);
        ObjectName on = ObjectNameUtil.createReadOnlyModuleON(mi);
        assertEquals(moduleName, ObjectNameUtil.getFactoryName(on));
        assertEquals(instanceName, ObjectNameUtil.getInstanceName(on));

        assertEquals(mi, ObjectNameUtil.fromON(on, ObjectNameUtil.TYPE_MODULE));
    }

    @Test
    public void testChecks() throws Exception {
        final ObjectName on = ObjectNameUtil.createON("customDomain", ObjectNameUtil.TYPE_KEY, ObjectNameUtil.TYPE_MODULE);

        assertFailure(new Runnable() {
            @Override
            public void run() {
                ObjectNameUtil.checkTypeOneOf(on, ObjectNameUtil.TYPE_RUNTIME_BEAN, ObjectNameUtil.TYPE_CONFIG_TRANSACTION);
            }
        }, IllegalArgumentException.class);

        assertFailure(new Runnable() {
            @Override
            public void run() {
                ObjectNameUtil.checkType(on, ObjectNameUtil.TYPE_RUNTIME_BEAN);
            }
        }, IllegalArgumentException.class);

        assertFailure(new Runnable() {
            @Override
            public void run() {
                ObjectNameUtil.checkDomain(on);
            }
        }, IllegalArgumentException.class);
    }

    private void assertFailure(final Runnable test, final Class<? extends Exception> ex) {
        try {
            test.run();
        } catch(Exception e) {
            assertTrue("Failed with wrong exception: " + Throwables.getStackTraceAsString(e),
                    e.getClass().isAssignableFrom(ex));
            return;
        }

        fail(test + " should have failed on " + ex);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateON() throws Exception {
        ObjectNameUtil.createON(">}+!#");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateON2() throws Exception {
        Map<String, String> map = new HashMap<>();
        ObjectNameUtil.createON(">}+!#", map);
    }
}
