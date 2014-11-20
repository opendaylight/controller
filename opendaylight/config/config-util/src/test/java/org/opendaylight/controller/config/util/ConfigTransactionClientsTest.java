/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Sets;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.management.Attribute;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.ValidationException.ExceptionMessageWithStackTrace;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

public class ConfigTransactionClientsTest {
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private TestingConfigTransactionController transactionController;
    private ObjectName transactionControllerON;
    private ConfigTransactionClient jmxTransactionClient;
    Attribute attr;


    @Before
    public void setUp() throws Exception {
        transactionController = new TestingConfigTransactionController();
        transactionControllerON = new ObjectName(ObjectNameUtil.ON_DOMAIN + ":"
                + ObjectNameUtil.TYPE_KEY + "=TransactionController");
        mbs.registerMBean(transactionController, transactionControllerON);
        jmxTransactionClient = new ConfigTransactionJMXClient(null,
                transactionControllerON,
                ManagementFactory.getPlatformMBeanServer());
    }

    @After
    public void cleanUp() throws Exception {
        if (transactionControllerON != null) {
            mbs.unregisterMBean(transactionControllerON);
        }
    }

    @Test
    public void testLookupConfigBeans() throws Exception {
        Set<ObjectName> jmxLookup = testClientLookupConfigBeans(jmxTransactionClient);
        assertEquals(Sets.newHashSet(transactionController.conf1,
                transactionController.conf2, transactionController.conf3),
                jmxLookup);
    }

    private Set<ObjectName> testClientLookupConfigBeans(
            ConfigTransactionClient client) {
        Set<ObjectName> beans = client.lookupConfigBeans();
        for (ObjectName on : beans) {
            assertEquals("Module", on.getKeyProperty("type"));
        }
        assertEquals(3, beans.size());
        return beans;
    }

    @Test
    public void testGetObjectName() throws Exception {
        testClientGetObjectName(jmxTransactionClient);
        assertEquals(testClientGetObjectName(jmxTransactionClient), true);
    }

    private boolean testClientGetObjectName(ConfigTransactionClient client) {
        return transactionControllerON.equals(client.getObjectName());
    }

    @Test
    public void testGetAvailableModuleNames() throws Exception {
        Set<String> jmxMN = testClientGetAvailableModuleNames(jmxTransactionClient);
        assertNull(jmxMN);
    }

    private Set<String> testClientGetAvailableModuleNames(
            ConfigTransactionClient client) {
        return client.getAvailableModuleNames();
    }

    @Test
    public void testGetTransactionName() throws Exception {
        String jmxTN = testClientGetTransactionName(jmxTransactionClient);
        assertEquals("transactionName", jmxTN);
    }

    private String testClientGetTransactionName(ConfigTransactionClient client) {
        return client.getTransactionName();
    }

    @Ignore
    public void testGetVersion() throws Exception {
        long jmxVersion = jmxTransactionClient.getVersion();
        assertNull(jmxVersion);
    }

    @Ignore
    public void testGetParentVersion() throws Exception {
        long jmxParentVersion = jmxTransactionClient.getParentVersion();
        assertNull(jmxParentVersion);
    }

    @Test
    public void testValidateConfig() throws Exception {
        jmxTransactionClient.validateConfig();
    }

    @Test
    public void testAbortConfig() throws Exception {
        jmxTransactionClient.abortConfig();
    }

    @Test
    public void testDestroyModule2() throws Exception {
        jmxTransactionClient.destroyModule("moduleB", "instB");
        assertNull(transactionController.conf4);
    }

    @Test
    public void testDestroyModule() throws Exception {
        ObjectName on = testClientCreateModule(jmxTransactionClient);
        jmxTransactionClient.destroyModule(on);
    }

    @Test
    public void testCreateModule() throws Exception {
        ObjectName on = testClientCreateModule(jmxTransactionClient);
        Assert.assertNotNull(on);
    }

    private ObjectName testClientCreateModule(ConfigTransactionClient client)
            throws Exception {
        return client.createModule("testModuleName", "testInstanceName");
    }

    @Ignore
    public void testAssertVersion() {
        jmxTransactionClient.assertVersion((int)jmxTransactionClient.getParentVersion(),
            (int)jmxTransactionClient.getVersion());
    }

    @Test(expected = NullPointerException.class)
    public void testCommit() throws Exception {
        jmxTransactionClient.commit();
    }

    @Test
    public void testLookupConfigBeans2() throws Exception {
        Set<ObjectName> jmxLookup = testClientLookupConfigBeans2(
                jmxTransactionClient, "moduleB");
        assertEquals(Sets.newHashSet(transactionController.conf3), jmxLookup);
    }

    private Set<ObjectName> testClientLookupConfigBeans2(
            ConfigTransactionClient client, String moduleName) {
        Set<ObjectName> beans = client.lookupConfigBeans(moduleName);
        assertEquals(1, beans.size());
        return beans;
    }

    @Test
    public void testLookupConfigBean() throws Exception {
        Set<ObjectName> jmxLookup = testClientLookupConfigBean(
                jmxTransactionClient, "moduleB", "instB");
        assertEquals(Sets.newHashSet(transactionController.conf3), jmxLookup);
    }

    private Set<ObjectName> testClientLookupConfigBean(
            ConfigTransactionClient client, String moduleName,
            String instanceName) {
        Set<ObjectName> beans = client.lookupConfigBeans(moduleName,
                instanceName);
        assertEquals(1, beans.size());
        return beans;
    }

    @Test
    public void testLookupConfigBeans3() throws Exception {
        Set<ObjectName> jmxLookup = testClientLookupConfigBeans3(
                jmxTransactionClient, "moduleB", "instB");
        assertEquals(Sets.newHashSet(transactionController.conf3), jmxLookup);
    }

    private Set<ObjectName> testClientLookupConfigBeans3(
            ConfigTransactionClient client, String moduleName,
            String instanceName) {
        Set<ObjectName> beans = client.lookupConfigBeans(moduleName,
                instanceName);
        assertEquals(1, beans.size());
        return beans;
    }

    @Test
    public void testCheckConfigBeanExists() throws Exception {
        jmxTransactionClient.checkConfigBeanExists(transactionControllerON);
        assertEquals("configBeanExists", transactionController.check);
    }

    @Test
    public void testSaveServiceReference() throws Exception {
        assertEquals(transactionControllerON, jmxTransactionClient.saveServiceReference("serviceInterfaceName", "refName", transactionControllerON));
    }

    @Test
    public void testRemoveServiceReference() throws Exception {
        jmxTransactionClient.removeServiceReference("serviceInterface", "refName");
        assertEquals("refName", transactionController.check);
    }

    @Test
    public void testRemoveAllServiceReferences() throws Exception {
        jmxTransactionClient.removeAllServiceReferences();
        assertNull(transactionController.check);
    }

    @Test
    public void testLookupConfigBeanByServiceInterfaceName() throws Exception {
        assertEquals(transactionController.conf3, jmxTransactionClient.lookupConfigBeanByServiceInterfaceName("serviceInterface", "refName"));
    }

    @Test
    public void testGetServiceMapping() throws Exception {
        Assert.assertNotNull(jmxTransactionClient.getServiceMapping());
    }

    @Test
    public void testLookupServiceReferencesByServiceInterfaceName() throws Exception {
        Assert.assertNotNull(jmxTransactionClient.lookupServiceReferencesByServiceInterfaceName("serviceInterfaceQName"));
    }

    @Test
    public void testLookupServiceInterfaceNames() throws Exception {
        assertEquals(Sets.newHashSet("setA"), jmxTransactionClient.lookupServiceInterfaceNames(transactionControllerON));
    }

    @Test
    public void testGetServiceInterfaceName() throws Exception {
        assertEquals("namespace" + "localName", jmxTransactionClient.getServiceInterfaceName("namespace", "localName"));
    }

    @Test
    public void removeServiceReferences() throws Exception {
        assertEquals(true, jmxTransactionClient.removeServiceReferences(transactionControllerON));
    }

    @Test
    public void testGetServiceReference() throws Exception {
        assertEquals(transactionController.conf3, jmxTransactionClient.getServiceReference("serviceInterfaceQName", "refName"));
    }

    @Test
    public void testCheckServiceReferenceExists() throws Exception {
        jmxTransactionClient.checkServiceReferenceExists(transactionControllerON);
        assertEquals("referenceExist", transactionController.check);
    }

    @Test(expected = RuntimeException.class)
    public void testValidateBean() throws Exception {
        jmxTransactionClient.validateBean(transactionControllerON);
    }

    @Test(expected = ValidationException.class)
    public void testValidateBean2() throws Exception {
        MBeanServer mbsLocal = mock(MBeanServer.class);
        MBeanException mBeanException = new MBeanException(new ValidationException(
                Collections.<String, Map<String, ExceptionMessageWithStackTrace>>emptyMap()));
        doThrow(mBeanException).when(mbsLocal).invoke(transactionControllerON, "validate", null, null);

        ConfigTransactionJMXClient jmxTransactionClientFake = new ConfigTransactionJMXClient(null,
                transactionControllerON,
                mbsLocal);
        jmxTransactionClientFake.validateBean(transactionControllerON);
    }

    @Test(expected = RuntimeException.class)
    public void testValidateBean3() throws Exception {
        MBeanServer mbsLocal = mock(MBeanServer.class);
        MBeanException mBeanException = new MBeanException(new RuntimeException());
        doThrow(mBeanException).when(mbsLocal).invoke(transactionControllerON, "validate", null, null);
        ConfigTransactionJMXClient jmxTransactionClientFake = new ConfigTransactionJMXClient(null,
                transactionControllerON,
                mbsLocal);
        jmxTransactionClientFake.validateBean(transactionControllerON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAttribute() throws Exception {
        attr = null;
        jmxTransactionClient.setAttribute(transactionControllerON, "attrName", attr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAttribute() throws Exception {
        attr = jmxTransactionClient.getAttribute(transactionController.conf3, "attrName");
        assertNull(attr);
    }

    @Test
    public void testGetAvailableModuleFactoryQNames() throws Exception {
        Assert.assertNotNull(jmxTransactionClient.getAvailableModuleFactoryQNames());
    }
}
