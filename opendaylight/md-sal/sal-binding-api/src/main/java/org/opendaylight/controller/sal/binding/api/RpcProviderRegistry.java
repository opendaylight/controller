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
 * <h2>RPC types</h2>
 * <ul>
 * <li><b>Global RPC</b> - only one implementation of RPC per broker instance</li>
 * <li><b>Routed RPC</b> - support for multiple implementations of service,
 * implementation is selected based on contents of RPC message as described in
 * YANG model.</li>
 * </ul>
 *
 * <h3>Global RPC</h3> RPCs modelled in YANG usually are meant to have single
 * implementation, which process that RPC.
 * <p>
 * In the context of MD-SAL any RPC which is not routed is considered global for
 * particular RPC Broker.
 * <p>
 * Global RPCs are registered using
 * {@link #addRpcImplementation(Class, RpcService)} method.
 *
 *
 * <h3>Routed RPC</h3>
 * MD-SAL supports routing of RPC between multiple implementations based on
 * content of RPC input.
 * <p>
 * RPC routing is based on:
 * <ul>
 * <li><b>RPC Qualified Name</b> - QName derived from YANG statement which
 * defines RPC</li>
 * <li><b>Context Type</b> - YANG Identity (QName) which identifies subset of
 * valid routes (
 * {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier})</li>
 * <li><b>Route</b> -
 * {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier} value,
 * which is part of RPC input. This value is used to select correct
 * implementation.</li>
 * </ul>
 *
 * <b>RPC Qualified Name</b> and <b>Context Type</b> are defined in YANG model
 * and are used to model routing table of MD-SAL for particular RPC type and
 * defines constraints for
 * {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier} which
 * could be used as a route.
 *
 * <h4>Context type</h4>
 *
 * Context type is in YANG modeled by combination of YANG <code>identity</code>
 * and Opendaylight specific extensions from <code>yang-ext</code> module:
 * <ul>
 * <li><code>context-instance</code> - Used in data tree part of YANG model to
 * identify nodes which belongs to specified context. Instance Identifiers
 * referencing this nodes are valid routes if this context-type is used for RPC
 * routing.</li>
 * <li><code>context-reference</code> - Used in RPC input to mark a leaf as
 * reference to particular instance of context type. Vale of this leaf is used
 * by RPC Broker to route RPC to correct implementation.
 * <code>context-reference</code> may only be used on <code>leaf</code>
 * statement with type <code>instance-identifier</code> or types derived from
 * <code>instance-identifier</code>.</li>
 * </ul>
 *
 *
 * <h4>Example</h4>
 *
 * <h5>1. Declaring Context Type</h5>
 * <p>
 * Following snippet declares simple identity <code>foo-context</code>.
 *
 * <pre>
 * module example {
 *     ...
 *     identity example-context {
 *          description "Identity used to mark example-context";
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * We could use declared identity as content-type by using it in combination
 * with <code>yang-ext</code> extensions.
 * <p>
 * Following snippet import <code>yang-ext</code> module and declares set of
 * valid Instance Identifiers for context type <code>example-context</code>
 *
 * <pre>
 * module example {
 *     ...
 *     import yang-ext {prefix ext;}
 *     ...
 *     container example {
 *          list item {
 *              key "id";
 *              leaf id {type string;}
 *              ext:context-instance "example-context";
 *          }
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * We introduced container <code>foo</code> to data tree, which contains a list
 * of items. Statement <code>ext:context-instance "example-context";</code>
 * declares that any Instance Identifier referencing <code>item</code> in data
 * tree, is valid in <code>example-context</code>. For example Instance
 * Identifier
 * <code>InstanceIdentifier.create(Example.class).child(Item.class,new ItemKey("Foo"))</code>
 * is valid in example-context, but
 * <code>InstanceIdentifier.create(Example.class)</code> is not valid.
 * <p>
 * This actually limits set of values which could be valid as input.
 * <p>
 * Using identity in combination with <code>context-instance</code> we
 * effectively declared Context type.
 *
 * <h5>2. Declaring RPC</h5>
 *
 * Following snippet declares simple RPC named <code>show-item</code>.
 *
 * <pre>
 * module example {
 *      ...
 *      rpc show-item {
 *          input {
 *              leaf item {
 *                  type instance-identifier;
 *              }
 *
 *              leaf description {
 *                  type "string";
 *              }
 *          }
 *      }
 * }
 * </pre>
 * <p>
 * We defined RPC <code>show-item</code>, which input is <code>item</code> and
 * <code>description</code>.
 * <p>
 * Generated RPC Service interface for example module is:
 *
 * <pre>
 * interface ExampleService implements RpcService {
 *      Future<RpcResult<Void>> showItem(ShowItemInput input);
 * }
 * </pre>
 * <p>
 *
 * For input there is generated interface ShowItemInput and ShowItemInputBuilder
 * which describes RPC input payload.
 *
 * <h5>3. Declaring RPC to be routed</h5>
 *
 * To declare RPC to be routed based on context type we need to add leaf which
 * will hold value, which will be used as route.
 *
 * Following snippet marks leaf <code>item</code> in <code>show-item</code> RPC
 * as <code>context-reference</code> for <code>example-context</code>, which
 * effectively marks this leaf as container for route value.
 *
 * <pre>
 * module example {
 *      ...
 *      rpc show-item {
 *          input {
 *              leaf item {
 *                  type instance-identifier;
 *                  ext:context-reference example-context;
 *              }
 *              leaf description {
 *                  type "string";
 *              }
 *          }
 *      }
 * }
 * </pre>
 * <p>
 * Now RPC <code>show-item</code> is routed RPC based on value <code>item</code>
 * in RPC input. Valid values are only Instance Identifiers which points to
 * example/item.
 *
 * <h5>4. Registering routed RPC implementation</h5>
 *
 * To register routed RPC implementation method
 * {@link #addRoutedRpcImplementation(Class, RpcService)} must be used. This
 * will return {@link RoutedRpcRegistration} object, which then is used to add /
 * remove routes associated with registered implementation.
 *
 * Following snippet registers <code>impl1</code> as RPC implementation for
 * <code>Item</code> named <code>"Foo"</code>.
 *
 * <pre>
 * InstanceIdentifier path = InstanceIdentifier.create(Example.class).child(Item.class, new ItemKey(&quot;Foo&quot;));
 * RoutedRpcRegistration reg = session.addRoutedRpcImplementation(ExampleService.class, impl1);
 * reg.registerPath(ExampleContext.class, path);
 * </pre>
 * <p>
 * It is also possible to register one implementation for multiple values:
 *
 * <pre>
 * InstanceIdentifier one = InstanceIdentifier.create(Example.class).child(Item.class, new ItemKey(&quot;One&quot;));
 * InstanceIdentifier two = InstanceIdentifier.create(Example.class).child(Item.class, new ItemKey(&quot;Two&quot;));
 *
 * RoutedRpcRegistration reg = session.addRoutedRpcImplementation(ExampleService.class, impl2);
 * reg.registerPath(ExampleContext.class, one);
 * reg.registerPath(ExampleContext.class, two);
 * </pre>
 *
 * <p>
 * When anyone invokes <code>showItem(ShowItemInput)</code> method on public proxy retrieved
 * via {@link RpcConsumerRegistry#getRpcService(Class)}, Rpc Broker will inspect
 * argument of showItem method, extract value of leaf item and selects
 * an implementation which registered path matches value of leaf item.
 *
 *
 * <h2>RPC Implementations</h2>
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
 * In Java for error conditions Exceptions are fine, and implementations should
 * use exceptions for error, be it {@link IllegalArgumentException} which is
 * thrown during callback invocation or any other exception, which should failed
 * returned future.
 *
 * Java does not have capability to attach warnings and that is why
 * <code>RpcResult</code> is part of the response.
 *
 *
 * <h4>Asynchronous Processing</h4>
 * RPC API contract requires clients to return
 * {@link java.util.concurrent.Future} it is encouraged to use
 * {@link com.google.common.util.concurrent.ListenableFuture} from Google Guava,
 * since it allows to register listeners for future completion or failures.
 *
 * Processing of rpc callback SHOULD NOT DO any blocking calls or any
 * CPU-intensive computation and should finish as soon as possible. Please use
 * future and update it asynchronously in order to provide result.
 *
 * Using
 * {@link com.google.common.util.concurrent.Futures#immediateFuture(Object)} is
 * valid only, if result is immediately available and asynchronous processing
 * will introduce only additional complexity.
 *
 * <h4>Error reporting</h4>
 *
 * During invocation of RPC callback implementations of RPC may throw:
 * <ul>
 * <li>{@link NullPointerException} - If it is immediately obvious, that input
 * is invalid. Exception MUST have human readable message. NullPointerException
 * must be thrown only if input is missing some required parameters.</li>
 * <li>{@link IllegalArgumentException} - If it is immediately obvious, that
 * input is invalid. Exception MUST have human readable message.</li>
 * </ul>
 *
 *
 * <h2>Routed RPC example</h2>
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
