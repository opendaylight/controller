/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import javax.management.DynamicMBean;

import org.opendaylight.controller.config.spi.Module;

/**
 * Each {@link org.opendaylight.controller.config.spi.Module} that is committed
 * will be wrapped into this interface.
 */
public interface DynamicMBeanWithInstance extends DynamicMBean {

    /**
     * Get original module that is wrapped with this instance.
     */
    Module getModule();

    /**
     * Gets 'live object' associated with current config object. Useful when
     * reconfiguring {@link org.opendaylight.controller.config.spi.Module}
     * instances.
     */
    AutoCloseable getInstance();

}
