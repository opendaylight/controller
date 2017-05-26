/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import java.util.EventListener;
import javax.annotation.Nonnull;

/**
 * An {@link EventListener} used to track RPC implementations becoming (un)available a {@link DOMRpcService}. Note that
 * the reported {@link DOMRpcIdentifier}s form an identifier space shared between RFC7950 actions and RFC6020 RPCs,
 * the former being also known as 'Routed RPCs'.
 *
 * <p>
 * Interpretation of DOMRpcIdentifiers has to be evaluated in the context of one of these types, which need to be
 * determined by matching {@link DOMRpcIdentifier#getType()} against a
 * {@link org.opendaylight.yangtools.yang.model.api.SchemaContext}, which determines actual semantics of
 * {@link DOMRpcIdentifier#getContextReference()}. Corresponding SchemaNode is required to be a known sub-interface
 * of {@link org.opendaylight.yangtools.yang.model.api.OperationDefinition}.
 *
 * <p>
 * For RFC6020 RPCs, reported context reference is always non-null and empty. It indicates an RPC implementation has
 * been registered and invocations can be reasonably (with obvious distributed system caveats coming from asynchronous
 * events) expected to succeed.
 *
 * <p>
 * For RFC7950 actions with a non-empty context-reference, the indication is the same as for RFC6020 RPCs.
 *
 * <p>
 * For RFC7950 actions with an empty context-reference, the indication is that the corresponding actions are
 * potentially available, but are subject to dynamic lifecycle of their context references. This includes two primary
 * use cases:
 * <ul>
 *     <li>dynamic action instantiation (when a device connects)</li>
 *     <li>dynamic action translation, such as transforming one action into another</li>
 * </ul>
 * First use case will provide further availability events with non-empty context references as they become available,
 * which can be safely ignored if the listener is interested in pure invocation-type integration.
 *
 * <p>
 * Second use case will not be providing further events, but rather will attempt to map any incoming invocation onto
 * some other RPC or action, or similar, which can separately fail. If a sub-request fails, such implementations are
 * required do report {@link DOMRpcImplementationNotAvailableException} as the invocation result, with the underlying
 * failure being linked as a cause.
 */
public interface DOMRpcAvailabilityListener extends EventListener {
    /**
     * Method invoked whenever an RPC type becomes available.
     *
     * @param rpcs RPC types newly available
     */
    void onRpcAvailable(@Nonnull Collection<DOMRpcIdentifier> rpcs);

    /**
     * Method invoked whenever an RPC type becomes unavailable.
     *
     * @param rpcs RPC types which became unavailable
     */
    void onRpcUnavailable(@Nonnull Collection<DOMRpcIdentifier> rpcs);

    /**
     * Implementation filtering method. This method is useful for forwarding RPC implementations,
     * which need to ensure they do not re-announce their own implementations. Without this method
     * a forwarder which registers an implementation would be notified of its own implementation,
     * potentially re-exporting it as local -- hence creating a forwarding loop.
     *
     * @param impl RPC implementation being registered
     * @return False if the implementation should not be reported, defaults to true.
     */
    default boolean acceptsImplementation(final DOMRpcImplementation impl) {
        return true;
    }
}
