/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * A {@link DOMService} which allows registration of RPC implementations with a conceptual
 * router. The client counterpart of this service is {@link DOMRpcService}.
 */
public interface DOMRpcProviderService extends DOMService {
    /**
     * Register an {@link DOMRpcImplementation} object with this service.
     *
     * @param implementation RPC implementation, must not be null
     * @param types Supported types. Must not be null or empty.
     * @return A {@link DOMRpcImplementationRegistration} object, guaranteed to be non-null.
     * @throws NullPointerException if implementation or types is null
     * @throws IllegalArgumentException if types is empty.
     */
    @Nonnull DOMRpcImplementationRegistration registerRpcImplementation(@Nonnull DOMRpcImplementation implementation, SchemaPath... types);
}
