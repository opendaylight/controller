/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.runtime;

import java.io.Closeable;

/**
 * Entry point for runtime bean functionality. Allows for registering root
 * runtime bean, returning an object that allows for hierarchical registrations.
 */
public interface RootRuntimeBeanRegistrator extends Closeable {

    HierarchicalRuntimeBeanRegistration registerRoot(RuntimeBean mxBean);

    /**
     * Close all runtime beans. This method is idempotent. It is allowed to use
     * this instance to register root or create new child registrators
     * afterwards, but it is not allowed to use closed registrations.
     */
    @Override
    void close();

}
