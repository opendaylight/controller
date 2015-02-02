/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.util.concurrent.CheckedFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * A {@link DOMService} which allows clients to invoke RPCs. The conceptual model of this
 * service is that of a dynamic router, where the set of available RPC services can change
 * dynamically. The service allows users to add a listener to track the process of
 * RPCs becoming available.
 */
public interface DOMRpcService extends DOMService {
    /**
     * Initiate invocation of an RPC. This method is guaranteed to not block on any external
     * resources.
     *
     * @param type SchemaPath of the RPC to be invoked
     * @param input Input arguments, null if the RPC does not take any.
     * @return A {@link CheckedFuture} which will return either a result structure,
     *         or report a subclass of {@link DOMRpcException} reporting a transport
     *         error.
     */
    @Nonnull CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull SchemaPath type, @Nullable NormalizedNode<?, ?> input);

    /**
     * Register a {@link DOMRpcAvailabilityListener} with this service to receive notifications
     * about RPC implementations becoming (un)available. The listener will be invoked with the
     * current implementations reported and will be kept uptodate as implementations come and go.
     *
     * Users should note that using a listener does not necessarily mean that {@link #invokeRpc(SchemaPath, NormalizedNode)}
     * will not report a failure due to {@link DOMRpcImplementationNotAvailableException} and
     * need to be ready to handle it. Implementations are encouraged to take reasonable precautions
     * to prevent this scenario from occurring.
     *
     * @param listener {@link DOMRpcAvailabilityListener} instance to register
     * @return A {@link DOMRpcAvailabilityListenerRegistration} representing this registration. Performing
     *         a {@link DOMRpcAvailabilityListenerRegistration#close()} will cancel it. Returned object
     *         is guaranteed to be non-null.
     */
    @Nonnull <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull T listener);
}
