/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.ConfigRegistry;

/**
 * Exposes version of config registry.
 */
public interface ConfigRegistryImplMXBean extends ConfigRegistry {
    /**
     * @return version of last committed transaction that is now used as base
     *         version. Transactions can only be committed if their parent
     *         version matches this value, that means, transaction must be
     *         started after last one has been committed.
     */
    long getVersion();

}
