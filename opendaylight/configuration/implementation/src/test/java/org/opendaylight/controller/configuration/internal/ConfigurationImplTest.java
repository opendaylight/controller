
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration.internal;

import org.junit.*;
import org.opendaylight.controller.configuration.IConfigurationAware;

public class ConfigurationImplTest { 
	

	@Test
	public void testAddRemoveSaveConfiguration() {
		
		ConfigurationImpl configurationImpl = new ConfigurationImpl();
		IConfigurationAware testConfigurationAware = new ConfigurationAwareTest();
		
		configurationImpl.addConfigurationAware(testConfigurationAware);
		configurationImpl.addConfigurationAware(testConfigurationAware);
		
		Assert.assertEquals(1, configurationImpl.getConfigurationAwareListSize());
		
		ConfigurationAwareTest testConfigurationAware1 = new ConfigurationAwareTest();
		configurationImpl.addConfigurationAware(testConfigurationAware1);
		
		Assert.assertEquals(2, configurationImpl.getConfigurationAwareListSize());
		
		ConfigurationAwareTest testConfigurationAware2 = new ConfigurationAwareTest();
		configurationImpl.addConfigurationAware(testConfigurationAware2);
		
		Assert.assertEquals(3, configurationImpl.getConfigurationAwareListSize());
		
		ConfigurationAwareTest testConfigurationAware3 = new ConfigurationAwareTest();
		configurationImpl.addConfigurationAware(testConfigurationAware3);
		
		Assert.assertEquals(4, configurationImpl.getConfigurationAwareListSize());
		
		
		configurationImpl.removeConfigurationAware(testConfigurationAware);
		Assert.assertEquals(3, configurationImpl.getConfigurationAwareListSize());
		
		configurationImpl.removeConfigurationAware(testConfigurationAware);
		Assert.assertEquals(3, configurationImpl.getConfigurationAwareListSize());
		
		configurationImpl.removeConfigurationAware(testConfigurationAware3);
		Assert.assertEquals(2, configurationImpl.getConfigurationAwareListSize());
		
		configurationImpl.removeConfigurationAware(testConfigurationAware1);
		Assert.assertEquals(1, configurationImpl.getConfigurationAwareListSize());
		
		configurationImpl.removeConfigurationAware(testConfigurationAware2);
		Assert.assertEquals(0, configurationImpl.getConfigurationAwareListSize());
		
	}
	
}

