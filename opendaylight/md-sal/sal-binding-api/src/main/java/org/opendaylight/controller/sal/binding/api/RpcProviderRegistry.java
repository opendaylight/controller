/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Rpc Broker which allows invocation of RPCs and registering implementations of
 * RPCs.
 *
 * Interface defining provider's access to the Rpc Registry which could be used
 * to register their implementations of service to the MD-SAL.
 *
 * <h2>RPC Implementations</h2>
 *
 * <h3>RPC Implementation types</h3>
 * <ul>
 * <li><b>Global Service</b> - only one implementation of RPC per broker
 * instance</li>
 * <li><b>Routed Service</b> - support for multiple implementations of service,
 * implementation is selected based on contents of RPC message as described in
 * YANG model.</li>
 *
 * <h3>Access to RPC services</h3>
 *
 * RPC implementation registered by user using
 * {@link #addRpcImplementation(Class, RpcService)} or
 * {@link #addRoutedRpcImplementation(Class, RpcService)} is exposed via all
 * available APIs (and payload formats) of MD-SAL (e.g. DOM Rpcs, Restconf),
 * which are associated with this Rpc Broker.
 * <p>
 * MD-SAL implementation is responsible for adaptation of payload format and
 * providing seamless translation to DTO generated from YANG model, which is
 * input of RPC.
 *
 * <h3>Notes for RPC implementations</h3>
 *
 * <h4>Rpc result</h4>
 *
 * Generated interfaces requires implementors to return
 * <code>Future<RpcResult<{RpcName}Output</code> object.
 *
 * Implementations should do processing of RPC asynchronously, and should return
 * an instance of {@link java.util.concurrent.Future}, which will complete once
 * RPC is processed.
 *
 * Nested {@link org.opendaylight.yangtools.yang.common.RpcResult} is generic
 * wrapper, which allows attaching additional information (such as warning and
 * errors) to result (along with payload). This is intented to provide
 * additional human readable information of users of public APIs and to transfer
 * warning / error information across system and may it visible via other APIs
 * such as Restconf.
 *
 * In Java for error conditions Exceptions are fine, and implementations
 * should use exceptions for error, be it {@link IllegalArgumentException}
 * which is thrown during callback invocation or any other exception,
 * which should failed returned future.
 *
 * Java does not have capability to attach warnings and that is why
 * <code>RpcResult</code> is part of the response.
 *
 *
 * <h4>Asynchronous Processing</h4>
 * RPC API contract requires clients to return {@link java.util.concurrent.Future}
 * it is encouraged to use {@link com.google.common.util.concurrent.ListenableFuture}
 * from Google Guava, since it allows to register listeners for future completion
 * or failures.
 *
 * Processing of rpc callback SHOULD NOT DO any blocking calls or any CPU-intensive
 * computation and should finish as soon as possible. Please use future and update
 * it asynchronously in order to provide result.
 *
 * Using {@link com.google.common.util.concurrent.Futures#immediateFuture(Object)}
 * is valid only, if result is immediately available and asynchronous processing
 * will introduce only additional complexity.
 *
 * <h4>Error reporting</h4>
 *
 * During invocation of RPC callback implementations of RPC may throw:
 * <ul>
 * <li>{@link NullPointerException} - If it is immediately obvious, that input is invalid. Exception MUST have human readable message.
 *      NullPointerException must be thrown only if input is missing some required parameters.
 * </li>
 * <li>{@link IllegalArgumentException} - If it is immediately obvious, that input is invalid. Exception MUST have human readable message.</li>
 * </ul>
 *
 *
 */
public interface RpcProviderRegistry extends //
        RpcConsumerRegistry, //
        RouteChangePublisher<RpcContextIdentifier, InstanceIdentifier<?>> {
    /**
     * Registers a global RPC service implementation.
     *
     * <p>
     * Registers user-supplied RPC implementation of provided service type. User
     * is required to implement all rpc methods of
     *
     * @param type
     *            RPC Service type
     * @param implementation
     *            Implementation of RPC service
     * @return Registration object, invoking <code>close()</code> method
     *         unregisters RPC implementation from MD-SAL.
     *
     * @throws IllegalArgumentException
     *             if supplied RPC Type is routed RPC type.
     */
    <T extends RpcService> RpcRegistration<T> addRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException;

    /**
     *
     * Register a Routed RpcService where routing is determined on annotated (in
     * YANG model) context-reference and value of annotated leaf.
     *
     * @param type
     *            Type of RpcService, use generated interface class, not your
     *            implementation class
     * @param implementation
     *            Implementation of RpcService
     * @return Registration object for routed RPC which could be used to
     *         unregister implementation and all paths associated with it, by
     *         invoking {@link RoutedRpcRegistration#close()} or associating new
     *         path with RPC implementation via invoking
     *         {@link RoutedRpcRegistration#registerPath(Object, org.opendaylight.yangtools.concepts.Path)}
     *
     * @throws IllegalStateException
     */
    <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException;
}
