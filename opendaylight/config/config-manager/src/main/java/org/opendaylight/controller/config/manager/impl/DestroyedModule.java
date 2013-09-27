/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.protocol.concepts.NamedObject;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.jmx.ModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.osgi.BeanToOsgiServiceManager.OsgiRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transfer object representing already committed module that needs to be destroyed. Implements comparable in order
 * to preserve order in which modules were created. Module instances should be closed in order defined by the
 * compareTo method.
 */
public class DestroyedModule implements NamedObject<ModuleIdentifier>, AutoCloseable, Comparable<DestroyedModule> {
	private static final Logger logger = LoggerFactory.getLogger(DestroyedModule.class);

	private final ModuleIdentifier name;
	private final AutoCloseable instance;
	private final ModuleJMXRegistrator oldJMXRegistrator;
	private final OsgiRegistration osgiRegistration;
	private final int orderingIdx;

	DestroyedModule(ModuleIdentifier name, AutoCloseable instance, ModuleJMXRegistrator oldJMXRegistrator,
	                OsgiRegistration osgiRegistration, int orderingIdx) {
		this.name = name;
		this.instance = instance;
		this.oldJMXRegistrator = oldJMXRegistrator;
		this.osgiRegistration = osgiRegistration;
		this.orderingIdx = orderingIdx;
	}

	@Override
	public ModuleIdentifier getName() {
		return name;
	}

	@Override
	public void close() {
		logger.info("Destroying {}", name);
		try{
			instance.close();
		}catch(Exception e){
			logger.error("Error while closing instance of {}", name, e);
		}
		try{
			oldJMXRegistrator.close();
		}catch(Exception e){
			logger.error("Error while closing jmx registrator of {}", name, e);
		}
		try{
			osgiRegistration.close();
		}catch(Exception e){
			logger.error("Error while closing osgi registration of {}", name, e);
		}
	}

	@Override
	public int compareTo(DestroyedModule o) {
		return Integer.compare(orderingIdx, o.orderingIdx);
	}
}
