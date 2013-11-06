
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.datastore.internal;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ServiceDependency;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActivatorTest {

	private static  ServiceDependency serviceDependency;
	
	@BeforeClass 
	public static void initialize(){
		serviceDependency = mock(ServiceDependency.class);
	}
	
	private class ActivatorTestImpl extends Activator{
		 protected ServiceDependency createServiceDependency() {
		        return ActivatorTest.serviceDependency;
		    }
	}
	
    @Test
    public void construct(){
        assertNotNull(new Activator());
    }
    
    @Test
    public void construct_OnInvokeOfGlobalImpl_ShouldReturnNotNullObject(){
        Activator activator = new Activator();
        
        assertNotNull(activator.getGlobalImplementations());
        assertEquals(ClusteredDataStoreManager.class,activator.getGlobalImplementations()[0]);
    }
    
    @Test
    public void construct_OnInvokeOfConfigGlobalInstance_ShouldNotThrowAnyExceptions(){
    	Activator activator = new ActivatorTestImpl();
    	
    	Component c = mock(Component.class);
    	Object clusterDataStoreMgr = ClusteredDataStoreManager.class;
    	
    	when(serviceDependency.setService(IClusterGlobalServices.class)).thenReturn(serviceDependency);
    	when(serviceDependency.setCallbacks("setClusterGlobalServices",
                    "unsetClusterGlobalServices")).thenReturn(serviceDependency);
    	when(serviceDependency.setRequired(true)).thenReturn(serviceDependency);
	
    	
    	activator.configureGlobalInstance(c, clusterDataStoreMgr);
    	
    	
    }
    
}
