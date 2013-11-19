/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.api;

/**
 * Yang store OSGi service
 */
public interface YangStoreService {

    /**
     * Module entry objects mapped to module names and namespaces.
     *
     * @return actual view of what is available in OSGi service registry.
     */
    YangStoreSnapshot getYangStoreSnapshot() throws YangStoreException;

}
