/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ConfigTransactionClientsTest {
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private TestingConfigTransactionController transactionController;
    private ObjectName transactionControllerON;
    private ConfigTransactionClient jmxTransactionClient;

    @Before
    public void setUp() throws Exception {
        transactionController = new TestingConfigTransactionController();
        transactionControllerON = new ObjectName(ObjectNameUtil.ON_DOMAIN + ":"
                + ObjectNameUtil.TYPE_KEY + "=TransactionController");
        mbs.registerMBean(transactionController, transactionControllerON);
        jmxTransactionClient = new ConfigTransactionJMXClient(null, transactionControllerON,
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
                transactionController.conf2, transactionController.conf3), jmxLookup);
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
    
    private boolean testClientGetObjectName(ConfigTransactionClient client){
    	return transactionControllerON.equals(client.getObjectName());    
    }
    
    @Test
    public void testGetAvailableModuleNames() throws Exception {
    	Set<String> jmxMN = testClientGetAvailableModuleNames(jmxTransactionClient);    	
    	assertEquals(null, jmxMN);
    }
    
    private Set<String> testClientGetAvailableModuleNames(ConfigTransactionClient client){
    	Set<String> moduleNames = client.getAvailableModuleNames();
    	if(moduleNames != null){
    		for(String str : moduleNames){
        		assertEquals(null, str);
        	}
    	}    	
    	return client.getAvailableModuleNames();    	
    }
     
    @Test
    public void testGetTransactionName() throws Exception {
    	String jmxTN = testClientGetTransactionName(jmxTransactionClient);
    	assertEquals(null, jmxTN);
    }
    
    private String testClientGetTransactionName(ConfigTransactionClient client){
    	return client.getTransactionName();    	
    }
    
    @Ignore
    public void testGetVersion() throws Exception {
    	long jmxVersion = jmxTransactionClient.getVersion();
    	assertEquals((Long)null, (Long)jmxVersion);    	
    }
    
    @Ignore
    public void testGetParentVersion() throws Exception {
    	long jmxParentVersion = jmxTransactionClient.getParentVersion();
    	assertEquals((Long)null, (Long)jmxParentVersion);    	
    }
    
    
}
