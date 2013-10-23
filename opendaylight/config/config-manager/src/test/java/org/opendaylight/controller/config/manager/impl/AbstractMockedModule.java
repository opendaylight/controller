/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.spi.Module;

public abstract class AbstractMockedModule implements Module {

    protected final AutoCloseable instance;

    public AbstractMockedModule() throws Exception {
        instance = prepareMockedInstance();
    }

    protected abstract AutoCloseable prepareMockedInstance() throws Exception;

    public AbstractMockedModule(DynamicMBeanWithInstance old) {
        instance = old.getInstance();
    }

    @Override
    public void validate() {
    }

    @Override
    public AutoCloseable getInstance() {
        return instance;
    }


}
