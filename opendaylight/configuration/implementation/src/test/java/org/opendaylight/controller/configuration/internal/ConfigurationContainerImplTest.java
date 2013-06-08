
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration.internal;

import org.junit.*;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;



public class ConfigurationContainerImplTest { 
	

	@Test
	public void testAddRemoveSaveConfiguration() {
		
		ConfigurationContainerImpl configurationContainerImpl = new ConfigurationContainerImpl();
		IConfigurationContainerAware testConfigurationContainerAware = new ConfigurationContainerAwareTest();
		
		configurationContainerImpl.addConfigurationContainerAware(testConfigurationContainerAware);
		configurationContainerImpl.addConfigurationContainerAware(testConfigurationContainerAware);
		
		Assert.assertEquals(1, configurationContainerImpl.getConfigurationAwareListSize());
		
		IConfigurationContainerAware testConfigurationAware1 = new ConfigurationContainerAwareTest();
		configurationContainerImpl.addConfigurationContainerAware(testConfigurationAware1);
		
		Assert.assertEquals(2, configurationContainerImpl.getConfigurationAwareListSize());
		
		IConfigurationContainerAware testConfigurationAware2 = new ConfigurationContainerAwareTest();
		configurationContainerImpl.addConfigurationContainerAware(testConfigurationAware2);
		
		Assert.assertEquals(3, configurationContainerImpl.getConfigurationAwareListSize());
		
		IConfigurationContainerAware testConfigurationAware3 = new ConfigurationContainerAwareTest();
		configurationContainerImpl.addConfigurationContainerAware(testConfigurationAware3);
		
		Assert.assertEquals(4, configurationContainerImpl.getConfigurationAwareListSize());
		
		configurationContainerImpl.removeConfigurationContainerAware(testConfigurationContainerAware);
		Assert.assertEquals(3, configurationContainerImpl.getConfigurationAwareListSize());
		
		configurationContainerImpl.removeConfigurationContainerAware(testConfigurationContainerAware);
		Assert.assertEquals(3, configurationContainerImpl.getConfigurationAwareListSize());
		
		configurationContainerImpl.removeConfigurationContainerAware(testConfigurationAware3);
		Assert.assertEquals(2, configurationContainerImpl.getConfigurationAwareListSize());
		
		configurationContainerImpl.removeConfigurationContainerAware(testConfigurationAware2);
		Assert.assertEquals(1, configurationContainerImpl.getConfigurationAwareListSize());
		
		configurationContainerImpl.removeConfigurationContainerAware(testConfigurationAware1);
		Assert.assertEquals(0, configurationContainerImpl.getConfigurationAwareListSize());
		
		
	}
	
}

