/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingIndependentMappingServiceTracker implements ServiceTrackerCustomizer<BindingIndependentMappingService, BindingIndependentMappingService> {
    private static final Logger logger = LoggerFactory.getLogger(BindingIndependentMappingServiceTracker.class);

    private final ConfigManagerActivator activator;
    private final BundleContext ctx;
    private BindingIndependentMappingService service;

    public BindingIndependentMappingServiceTracker(BundleContext context, ConfigManagerActivator activator) {
        this.ctx = context;
        this.activator = activator;
    }

    @Override
    public synchronized BindingIndependentMappingService addingService(
            ServiceReference<BindingIndependentMappingService> moduleFactoryServiceReference) {

        if (service != null) {
            // FIXME
            // Second registration appears from
            // org.opendaylight.controller.config.yang.md.sal.binding.impl.RuntimeMappingModule
            logger.debug("BindingIndependentMappingService was already added as {}" + " now added as {}",
                    service, ctx.getService(moduleFactoryServiceReference));
            return null;
        }

        BindingIndependentMappingService service = ctx.getService(moduleFactoryServiceReference);
        this.service = service;
        CodecRegistry codecRegistry = service.getCodecRegistry();
        logger.debug("Codec registry acquired {}", codecRegistry);
//        activator.initConfigManager(ctx, codecRegistry);
        return service;
    }

    @Override
    public void modifiedService(ServiceReference <BindingIndependentMappingService> moduleFactoryServiceReference, BindingIndependentMappingService o) {
        // TODO crash
    }

    @Override
    public void removedService(ServiceReference<BindingIndependentMappingService> moduleFactoryServiceReference, BindingIndependentMappingService o) {
        // TODO crash
    }
}
