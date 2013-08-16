/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Hashtable;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerActivator implements BundleActivator {

	private static final Logger log = LoggerFactory.getLogger(BrokerActivator.class);
	private BindingAwareBrokerImpl baSal;
	private ServiceRegistration<BindingAwareBroker> baSalRegistration;
	
	
	@Override
	public void start(BundleContext context) throws Exception {
		log.info("Binding Aware Broker initialized");
		baSal = new BindingAwareBrokerImpl();
		baSal.setBrokerBundleContext(context);
		baSal.start();
		
		BindingAwareBroker baSalService = baSal;
		Hashtable<String, String> properties = new Hashtable<>();
		this.baSalRegistration = context.registerService(BindingAwareBroker.class,baSalService, properties);
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		log.info("Binding Aware Broker stopped");
		baSalRegistration.unregister();
	}

}
