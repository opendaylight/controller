/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.runtime;

import javax.management.ObjectName;

public interface HierarchicalRuntimeBeanRegistration extends AutoCloseable {

    ObjectName getObjectName();

    HierarchicalRuntimeBeanRegistration register(String key, String value,
            RuntimeBean mxBean);

    /**
     * Unregister beans that were registered using this registrator and its
     * child registrators. This method is not idempotent, it is not allowed to
     * use this instance or child registrators after they are closed.
     */
    @Override
    void close();

}
