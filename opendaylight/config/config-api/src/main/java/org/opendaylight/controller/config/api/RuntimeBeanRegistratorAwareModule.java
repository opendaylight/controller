/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator;
import org.opendaylight.controller.config.spi.Module;

/**
 * Module implementing this interface will receive
 * {@link RootRuntimeBeanRegistrator} before getInstance() is invoked.
 */
public interface RuntimeBeanRegistratorAwareModule extends Module {
    /**
     * Configuration framework will call this setter on all modules implementing
     * this interface. It is responsibility of modules or rather their instances
     * to close registrator in their {@link Closeable#close()} method. Same
     * module will get the same registrator during reconfiguration.
     *
     * @param rootRuntimeBeanRegistrator
     */
    public void setRuntimeBeanRegistrator(
            RootRuntimeBeanRegistrator rootRuntimeBeanRegistrator);

}
