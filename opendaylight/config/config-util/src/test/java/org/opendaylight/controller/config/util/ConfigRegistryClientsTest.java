/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConfigRegistry;

public class ConfigRegistryClientsTest {

    private TestingConfigRegistry testingRegistry;
    private ObjectName testingRegistryON;
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private ConfigRegistryClient jmxRegistryClient;
    private ConfigTransactionClient jmxTransactionClient;
    private Map<String, ObjectName> map;

    @Before
    public void setUp() throws Exception {
        testingRegistry = new TestingConfigRegistry();
        testingRegistryON = ConfigRegistry.OBJECT_NAME;
        mbs.registerMBean(testingRegistry, testingRegistryON);
        jmxRegistryClient = new ConfigRegistryJMXClient(
                ManagementFactory.getPlatformMBeanServer());
        map = new HashMap<>();
    }

    @After
    public void cleanUp() throws Exception {
        if (testingRegistryON != null) {
            mbs.unregisterMBean(testingRegistryON);
        }
    }

    @Test
    public void testCreateTransaction() throws Exception{
        jmxTransactionClient = jmxRegistryClient.createTransaction();
        assertNotNull(jmxTransactionClient);
    }

    @Test
    public void testGetConfigTransactionClient2() throws Exception{
        jmxTransactionClient = jmxRegistryClient.getConfigTransactionClient("transactionName");
        assertNotNull(jmxTransactionClient);
    }

    @Test
    public void testGetConfigTransactionClient() throws Exception{
        jmxTransactionClient = jmxRegistryClient.getConfigTransactionClient(testingRegistryON);
        assertNotNull(jmxTransactionClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewMXBeanProxy() throws Exception{
        if (jmxRegistryClient instanceof ConfigRegistryJMXClient) {
            ConfigRegistryJMXClient client = (ConfigRegistryJMXClient) jmxRegistryClient;
            assertNull(client.newMXBeanProxy(testingRegistryON, String.class));
        } else {
            throw new AssertionError("brm msg");
        }
    }

    @Test
    public void testBeginConfig() throws Exception{
        Assert.assertNotNull(jmxRegistryClient.beginConfig());
    }

    @Test
    public void testCommitConfig() throws Exception{
        assertNull(jmxRegistryClient.commitConfig(testingRegistryON));
    }

    @Test
    public void testGetOpenConfigs() throws Exception{
        assertNull(jmxRegistryClient.getOpenConfigs());
    }

    @Test(expected = RuntimeException.class)
    public void testGetVersion() throws Exception{
        assertEquals(3, jmxRegistryClient.getVersion());
    }

    @Test
    public void testGetAvailableModuleNames() throws Exception{
        assertNull(jmxRegistryClient.getAvailableModuleNames());
    }

    @Test
    public void testIsHealthy() throws Exception{
        assertEquals(false, jmxRegistryClient.isHealthy());
    }

    @Test
    public void testLookupConfigBeans3() throws Exception{
        Set<ObjectName> son = jmxRegistryClient.lookupConfigBeans();
        assertEquals(3, son.size());
    }

    @Test
    public void testLookupConfigBeans2() throws Exception{
        Set<ObjectName> son = jmxRegistryClient.lookupConfigBeans(TestingConfigRegistry.moduleName1);
        assertEquals(2, son.size());
    }

    @Test
    public void testLookupConfigBeans() throws Exception{
        Set<ObjectName> son = jmxRegistryClient.lookupConfigBeans(TestingConfigRegistry.moduleName1, TestingConfigRegistry.instName1);
        Set<ObjectName> on = Sets.newHashSet(TestingConfigRegistry.conf2);
        assertEquals(on, son);
    }

    @Test
    public void testLookupConfigBean() throws Exception{
        ObjectName on = jmxRegistryClient.lookupConfigBean(TestingConfigRegistry.moduleName1, null);
        assertEquals(TestingConfigRegistry.conf3, on);
    }

    @Test
    public void testLookupRuntimeBeans() throws Exception {
        Set<ObjectName> jmxLookup = lookupRuntimeBeans(jmxRegistryClient);
        assertEquals(Sets.newHashSet(TestingConfigRegistry.run2, TestingConfigRegistry.run1, TestingConfigRegistry.run3), jmxLookup);
    }

    private Set<ObjectName> lookupRuntimeBeans(final ConfigRegistryClient client)
            throws Exception {
        Set<ObjectName> beans = client.lookupRuntimeBeans();
        for (ObjectName on : beans) {
            assertEquals("RuntimeBean", on.getKeyProperty("type"));
        }
        assertEquals(3, beans.size());
        return beans;
    }

    @Test
    public void testLookupRuntimeBeansWithIfcNameAndInstanceName()
            throws InstanceNotFoundException {
        Set<ObjectName> jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName1);
        assertEquals(1, jmxLookup.size());
        assertEquals(Sets.newHashSet(TestingConfigRegistry.run2), jmxLookup);

        jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName2,
                TestingConfigRegistry.instName2);
        assertEquals(1, jmxLookup.size());
        assertEquals(Sets.newHashSet(TestingConfigRegistry.run3), jmxLookup);

        jmxLookup = clientLookupRuntimeBeansWithModuleAndInstance(
                jmxRegistryClient, TestingConfigRegistry.moduleName1,
                TestingConfigRegistry.instName2);
        assertEquals(0, jmxLookup.size());
        assertEquals(Sets.newHashSet(), jmxLookup);
    }

    private Set<ObjectName> clientLookupRuntimeBeansWithModuleAndInstance(
            final ConfigRegistryClient client, final String moduleName, final String instanceName) {
        Set<ObjectName> beans = client.lookupRuntimeBeans(moduleName, instanceName);
        if (beans.size() > 0) {
            assertEquals("RuntimeBean",
                    beans.iterator().next().getKeyProperty("type"));
        }
        return beans;
    }

    @Test
    public void testCheckConfigBeanExists() throws Exception{
        jmxRegistryClient.checkConfigBeanExists(testingRegistryON);
        assertEquals(true, TestingConfigRegistry.checkBool);
    }

    @Test
    public void testLookupConfigBeanByServiceInterfaceName() throws Exception{
        ObjectName on = clientLookupConfigBeanByServiceInterfaceName();
        assertEquals(TestingConfigRegistry.conf1, on);
    }

    private ObjectName clientLookupConfigBeanByServiceInterfaceName(){
        return jmxRegistryClient.lookupConfigBeanByServiceInterfaceName("qnameA", "refA");
    }

    @Test
    public void testGetServiceMapping() throws Exception{
        assertNull(jmxRegistryClient.getServiceMapping());
    }

    @Test
    public void testLookupServiceReferencesByServiceInterfaceName() throws Exception{
        map.put("conf2", TestingConfigRegistry.conf2);
        assertEquals(map, jmxRegistryClient.lookupServiceReferencesByServiceInterfaceName("qnameB"));
    }

    @Test
    public void testLookupServiceInterfaceNames() throws Exception{
        assertThat(clientLookupServiceInterfaceNames(testingRegistryON), hasItem(TestingConfigRegistry.serviceQName1));
        assertThat(clientLookupServiceInterfaceNames(testingRegistryON), hasItem(TestingConfigRegistry.serviceQName2));
    }

    private Set<String> clientLookupServiceInterfaceNames(final ObjectName client) throws InstanceNotFoundException{
        return jmxRegistryClient.lookupServiceInterfaceNames(client);
    }

    @Test
    public void testGetServiceInterfaceName() throws Exception{
        assertNull(jmxRegistryClient.getServiceInterfaceName(null, null));
    }

    @Test(expected = RuntimeException.class)
    public void testInvokeMethod() throws Exception{
        assertNull(jmxRegistryClient.invokeMethod(testingRegistryON, "name", null, null));
    }

    @Test(expected = RuntimeException.class)
    public void testGetAttributeCurrentValue() throws Exception{
        assertNull(jmxRegistryClient.getAttributeCurrentValue(testingRegistryON, "attrName"));
    }

    @Test
    public void testGetAvailableModuleFactoryQNames() throws Exception{
        for(String str : jmxRegistryClient.getAvailableModuleFactoryQNames()){
            if(str != TestingConfigRegistry.moduleName1){
                assertEquals(TestingConfigRegistry.moduleName2, str);
            }
            else{
                assertEquals(TestingConfigRegistry.moduleName1, str);
            }
        }
    }

    @Test
    public void testGetServiceReference() throws Exception{
        Assert.assertNotNull(jmxRegistryClient.getServiceReference(null, null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testcheckServiceReferenceExists() throws Exception{
        jmxRegistryClient.checkServiceReferenceExists(testingRegistryON);
    }
}
