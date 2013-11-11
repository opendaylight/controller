
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.datastore.internal;

import org.apache.felix.dm.Component;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ClusteredDataStoreManagerTest {
	
	private  static ClusteredDataStoreManager clusteredDSMgr = null;
	private IClusterGlobalServices icClusterGlbServices =  mock(IClusterGlobalServices.class);
	 
	@BeforeClass
	public static void construct(){
		clusteredDSMgr = new ClusteredDataStoreManager();
        assertNotNull(clusteredDSMgr);
	}
	 
	@Test
	public void construct_OnSetClusterGlobalServices_AssertNoException(){
		icClusterGlbServices =  mock(IClusterGlobalServices.class);
		 
		clusteredDSMgr.setClusterGlobalServices(icClusterGlbServices);
	 }
	 
	 @Test
	 public void construct_OnUnSetClusterGlobalServices_AssertNoException(){
		 IClusterGlobalServices icClusterGlbServices =  mock(IClusterGlobalServices.class);
		 
		 clusteredDSMgr.unsetClusterGlobalServices(icClusterGlbServices);
	 }
	 
	 @Test
	 public void construct_init_AssertNoException() throws CacheExistException,CacheConfigException{
		 ClusteredDataStoreImpl clusteredDSImpl =  mock(ClusteredDataStoreImpl.class);
		 
		 ClusteredDataStoreManager clusteredDSManager = spy(new ClusteredDataStoreManager());
		 doReturn(clusteredDSImpl).when(clusteredDSManager).createClusteredDataStore(any(Component.class));
		 Component c = mock(Component.class);
		 
		 clusteredDSManager.init(c);
	 }
	 
	 @Test(expected = IllegalStateException.class)
	 public void construct_init_AssertCacheExistException() throws CacheExistException,CacheConfigException{
		 ClusteredDataStoreManager clusteredDSManager = spy(new ClusteredDataStoreManager());
		 doThrow(CacheExistException.class).when(clusteredDSManager).createClusteredDataStore(any(Component.class));
		 Component c = mock(Component.class);
		 
		 clusteredDSManager.init(c);
	 }
	 
	 @Test(expected = IllegalStateException.class)
	 public void construct_init_AssertCacheConfigException() throws CacheExistException,CacheConfigException{
		 ClusteredDataStoreManager clusteredDSManager = spy(new ClusteredDataStoreManager());
		 doThrow(CacheConfigException.class).when(clusteredDSManager).createClusteredDataStore(any(Component.class));
		 Component c = mock(Component.class);
		 
		 clusteredDSManager.init(c);
	 }
	 
	 @Test
	 public void construct_readOperationalData_AssertNoException() throws CacheExistException,CacheConfigException{
		 ClusteredDataStoreImpl clusteredDSImpl =  mock(ClusteredDataStoreImpl.class);
		 
		 ClusteredDataStoreManager clusteredDSManager = spy(new ClusteredDataStoreManager());
		 doReturn(clusteredDSImpl).when(clusteredDSManager).createClusteredDataStore(any(Component.class));
		 Component c = mock(Component.class);
		 
		 clusteredDSManager.init(c);
		 
		 Object o = mock(Object.class);
		 
		 when(clusteredDSImpl.readOperationalData(any(InstanceIdentifier.class))).thenReturn(o);
		 assertEquals(o,clusteredDSManager.readOperationalData(any(InstanceIdentifier.class)));
	 }
	 
	 
	 @Test
	 public void construct_readConfigurationData_AssertNoException() throws CacheExistException,CacheConfigException{
		 ClusteredDataStoreImpl clusteredDSImpl =  mock(ClusteredDataStoreImpl.class);
		 
		 ClusteredDataStoreManager clusteredDSManager = spy(new ClusteredDataStoreManager());
		 doReturn(clusteredDSImpl).when(clusteredDSManager).createClusteredDataStore(any(Component.class));
		 Component c = mock(Component.class);
		 
		 clusteredDSManager.init(c);
		 Object o = mock(Object.class);
		 
		 when(clusteredDSImpl.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(o);
		 assertEquals(o,clusteredDSManager.readConfigurationData(any(InstanceIdentifier.class)));
	 }
	 
	 @Test
	 public void construct_requestCommit_AssertNoException() throws CacheExistException,CacheConfigException{
		 ClusteredDataStoreImpl clusteredDSImpl =  mock(ClusteredDataStoreImpl.class);
		 
		 ClusteredDataStoreManager clusteredDSManager = spy(new ClusteredDataStoreManager());
		 doReturn(clusteredDSImpl).when(clusteredDSManager).createClusteredDataStore(any(Component.class));
		 Component c = mock(Component.class);
		 
		 clusteredDSManager.init(c);
		 DataCommitTransaction dataCommitTransaction = mock(DataCommitTransaction.class);
		 
		 when(clusteredDSImpl.requestCommit(any(DataModification.class))).thenReturn(dataCommitTransaction);
		 assertEquals(dataCommitTransaction,clusteredDSManager.requestCommit(any(DataModification.class)));
	 }
}
