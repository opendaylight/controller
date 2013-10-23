/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.jmx.ModuleJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.osgi.BeanToOsgiServiceManager.OsgiRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transfer object representing already committed module that needs to be
 * destroyed. Implements comparable in order to preserve order in which modules
 * were created. Module instances should be closed in order defined by the
 * compareTo method.
 */
public class DestroyedModule implements AutoCloseable,
        Comparable<DestroyedModule>, Identifiable<ModuleIdentifier> {
    private static final Logger logger = LoggerFactory
            .getLogger(DestroyedModule.class);

    private final ModuleIdentifier identifier;
    private final AutoCloseable instance;
    private final ModuleJMXRegistrator oldJMXRegistrator;
    private final OsgiRegistration osgiRegistration;
    private final int orderingIdx;

    DestroyedModule(ModuleIdentifier identifier, AutoCloseable instance,
            ModuleJMXRegistrator oldJMXRegistrator,
            OsgiRegistration osgiRegistration, int orderingIdx) {
        this.identifier = identifier;
        this.instance = instance;
        this.oldJMXRegistrator = oldJMXRegistrator;
        this.osgiRegistration = osgiRegistration;
        this.orderingIdx = orderingIdx;
    }

    @Override
    public void close() {
        logger.info("Destroying {}", identifier);
        try {
            instance.close();
        } catch (Exception e) {
            logger.error("Error while closing instance of {}", identifier, e);
        }
        try {
            oldJMXRegistrator.close();
        } catch (Exception e) {
            logger.error("Error while closing jmx registrator of {}", identifier, e);
        }
        try {
            osgiRegistration.close();
        } catch (Exception e) {
            logger.error("Error while closing osgi registration of {}", identifier, e);
        }
    }

    @Override
    public int compareTo(DestroyedModule o) {
        return Integer.compare(orderingIdx, o.orderingIdx);
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return identifier;
    }
}
