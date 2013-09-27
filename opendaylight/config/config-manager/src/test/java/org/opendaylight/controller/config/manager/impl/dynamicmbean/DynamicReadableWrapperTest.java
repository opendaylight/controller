/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.spi.Module;

import java.lang.management.ManagementFactory;

public class DynamicReadableWrapperTest extends AbstractDynamicWrapperTest {

	@Override
	protected AbstractDynamicWrapper getDynamicWrapper(Module module, ModuleIdentifier moduleIdentifier) {
		return new DynamicReadableWrapper(module, null, moduleIdentifier, internalServer, ManagementFactory.getPlatformMBeanServer());
	}

}
