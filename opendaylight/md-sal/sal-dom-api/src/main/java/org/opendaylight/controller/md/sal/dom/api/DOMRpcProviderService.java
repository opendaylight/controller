/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A {@link DOMService} which allows registration of RPC implementations with a conceptual router. The client
 * counterpart of this service is {@link DOMRpcService}.
 *
 * <p>
 * This interface supports both RFC6020 RPCs and RFC7950 actions (formerly known as 'Routed RPCs'. Invocation for
 * RFC6020 RPCs is always based on an empty context reference. Invocation of actions requires a non-empty context
 * reference and is matched against registered implementations as follows:
 * <ul>
 *     <li>First, attempt to look up the implementation based on exact match. If a match is found the invocation is
 *         on that implementation, returning its result.</li>
 *     <li>Second, attempt to look up the implementation which registered for empty context reference. If a such an
 *         implementation exists, invoke that implementation, returning its result</li>
 *     <li>Throw {@link DOMRpcImplementationNotAvailableException}
 * </ul>
 *
 * <p>
 * All implementations are required to perform these steps as specified above.
 */
public interface DOMRpcProviderService extends DOMService {
    /**
     * Register an {@link DOMRpcImplementation} object with this service.
     *
     * @param implementation RPC implementation, must not be null
     * @param rpcs Array of supported RPC identifiers. Must not be null, empty, or contain a null element.
     *             Each identifier is added exactly once, no matter how many times it occurs.
     * @return A {@link DOMRpcImplementationRegistration} object, guaranteed to be non-null.
     * @throws NullPointerException if implementation or types is null
     * @throws IllegalArgumentException if types is empty or contains a null element.
     */
    @Nonnull <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            @Nonnull T implementation, @Nonnull DOMRpcIdentifier... rpcs);

    /**
     * Register an {@link DOMRpcImplementation} object with this service.
     *
     * @param implementation RPC implementation, must not be null
     * @param rpcs Set of supported RPC identifiers. Must not be null, empty, or contain a null element.
     * @return A {@link DOMRpcImplementationRegistration} object, guaranteed to be non-null.
     * @throws NullPointerException if implementation or types is null
     * @throws IllegalArgumentException if types is empty or contains a null element.
     */
    @Nonnull <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(
            @Nonnull T implementation, @Nonnull Set<DOMRpcIdentifier> rpcs);
}
