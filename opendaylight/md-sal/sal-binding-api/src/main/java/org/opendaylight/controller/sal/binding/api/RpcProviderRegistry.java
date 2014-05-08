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
 * Provides a registry for Remote Procedure Call (RPC) service implementations. The RPCs are
 * defined in YANG models.
 * <p>
 * There are 2 types of RPCs:
 * <ul>
 * <li>Global</li>
 * <li>Routed</li>
 * </ul>
 *
 * <h2>Global RPC</h2>
 * <p>
 * An RPC is global if there is intended to be only 1 registered implementation. A global RPC is not
 * explicitly declared as such, essentially any RPC that is not defined to be routed is considered global.
 * <p>
 * Global RPCs are registered using the
 * {@link #addRpcImplementation(Class, RpcService)} method.
 *
 * <h2>Routed RPC</h2>
 * <p>
 * MD-SAL supports routing of RPC between multiple implementations where the appropriate
 * implementation is selected at run time based on the content of the RPC message as described in
 * YANG model.
 * <p>
 * RPC routing is based on:
 * <ul>
 * <li><b>Route identifier</b> -
 * An {@link org.opendaylight.yangtools.yang.binding.InstanceIdentifier InstanceIdentifier} value
 * which is part of the RPC input. This value is used to select the correct
 * implementation at run time.</li>
 * <li><b>Context Type</b> - A YANG-defined construct which constrains the subset of
 * valid route identifiers for a particular RPC.</li>
 * </ul>
 *
 * <h3>Context type</h3>
 * <p>
 * A context type is modeled in YANG using a combination of a YANG <code>identity</code>
 * and Opendaylight specific extensions from <code>yang-ext</code> module. These extensions are:
 * <ul>
 * <li><b>context-instance</b> - This is used in the data tree part of a YANG model to
 * define a context type that associates nodes with a specified context <code>identity</code>.
 * Instance identifiers that reference these nodes are valid route identifiers for RPCs that
 * reference this context type.</li>
 * <li><b>context-reference</b> - This is used in RPC input to mark a leaf of type
 * <code>instance-identifier</code> as a reference to the particular context type defined by the
 * specified context <code>identity</code>. The value of this
 * leaf is used by the RPC broker at run time to route the RPC request to the correct implementation.
 * Note that <code>context-reference</code> may only be used on leaf elements of type
 * <code>instance-identifier</code> or a type derived from <code>instance-identifier</code>.</li>
 * </ul>
 *
 *
 * <h3>Routed RPC example</h3>
 * <p>
 * <h5>1. Defining a Context Type</h5>
 * <p>
 * The following snippet declares a simple YANG <code>identity</code> named <code>example-context</code>:
 *
 * <pre>
 * module example {
 *     ...
 *     identity example-context {
 *          description "Identity used to define an example-context type";
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * We then use the declared identity to define a context type by using it in combination
 * with the <code>context-instance</code> YANG extension. We'll associate the context type
 * with a list element in the data tree. This defines the set of nodes whose instance
 * identifiers are valid for the <code>example-context</code> context type.
 * <p>
 * The following YANG snippet imports the <code>yang-ext</code> module and defines the list
 * element named <code>item</code> inside a container named <code>foo</code>:
 *
 * <pre>
 * module foo {
 *     ...
 *     import yang-ext {prefix ext;}
 *     ...
 *     container foo {
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
 * The statement <code>ext:context-instance "example-context";</code> inside the list element
 * declares that any instance identifier referencing <code>item</code> in the data
 * tree is valid for <code>example-context</code>. For example, the following instance
 * identifier:
 * <pre>
 *     InstanceIdentifier.create(Foo.class).child(Item.class,new ItemKey("Foo"))
 * </pre>
 * is valid for <code>example-context</code>. However the following:
 * <pre>
 *     InstanceIdentifier.create(Example.class)
 * </pre>
 * is not valid.
 * <p>
 * So using an <code>identity</code> in combination with <code>context-instance</code> we
 * have effectively defined a context type that can be referenced in a YANG RPC input.
 *
 * <h5>2. Defining an RPC to use the Context Type</h5>
 * <p>
 * To define an RPC to be routed based on the context type we need to add an input leaf element
 * that references the context type which will hold an instance identifier value to be
 * used to route the RPC.
 * <p>
 * The following snippet defines an RPC named <code>show-item</code> with 2 leaf elements
 * as input: <code>item</code> of type <code>instance-identifier</code> and <code>description</code>:
 *
 * <pre>
 * module foo {
 *      ...
 *      import yang-ext {prefix ext;}
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
 * We mark the <code>item</code> leaf with a <code>context-reference</code> statement that
 * references the <code>example-context</code> context type. RPC calls will then be routed
 * based on the instance identifier value contained in <code>item</code>. Only instance
 * identifiers that point to a <code>foo/item</code> node are valid as input.
 * <p>
 * The generated RPC Service interface for the module is:
 *
 * <pre>
 * interface FooService implements RpcService {
 *      Future&lt;RpcResult&lt;Void&gt;&gt; showItem(ShowItemInput input);
 * }
 * </pre>
 * <p>
 * For constructing the RPC input, there are generated classes ShowItemInput and ShowItemInputBuilder.
 *
 * <h5>3. Registering a routed RPC implementation</h5>
 * <p>
 * To register a routed implementation for the <code>show-item</code> RPC, we must use the
 * {@link #addRoutedRpcImplementation(Class, RpcService)} method. This
 * will return a {@link RoutedRpcRegistration} instance which can then be used to register /
 * unregister routed paths associated with the registered implementation.
 * <p>
 * The following snippet registers <code>myImpl</code> as the RPC implementation for an
 * <code>item</code> with key <code>"foo"</code>:
 * <pre>
 * // Create the instance identifier path for item "foo"
 * InstanceIdentifier path = InstanceIdentifier.create(Foo.class).child(Item.class, new ItemKey(&quot;foo&quot;));
 *
 * // Register myImpl as the implementation for the FooService RPC interface
 * RoutedRpcRegistration reg = rpcRegistry.addRoutedRpcImplementation(FooService.class, myImpl);
 *
 * // Now register for the context type and specific path ID. The context type is specified by the
 * // YANG-generated class for the example-context identity.
 * reg.registerPath(ExampleContext.class, path);
 * </pre>
 * <p>
 * It is also possible to register the same implementation for multiple paths:
 *
 * <pre>
 * InstanceIdentifier one = InstanceIdentifier.create(Foo.class).child(Item.class, new ItemKey(&quot;One&quot;));
 * InstanceIdentifier two = InstanceIdentifier.create(Foo.class).child(Item.class, new ItemKey(&quot;Two&quot;));
 *
 * RoutedRpcRegistration reg = rpcRegistry.addRoutedRpcImplementation(FooService.class, myImpl);
 * reg.registerPath(ExampleContext.class, one);
 * reg.registerPath(ExampleContext.class, two);
 * </pre>
 *
 * <p>
 * When another client invokes the <code>showItem(ShowItemInput)</code> method on the proxy instance
 * retrieved via {@link RpcConsumerRegistry#getRpcService(Class)}, the proxy will inspect the
 * arguments in ShowItemInput, extract the InstanceIdentifier value of the <code>item</code> leaf and select
 * the implementation whose registered path matches the InstanceIdentifier value of the <code>item</code> leaf.
 *
 * <h2>Notes for RPC Implementations</h2>
 *
 * <h3>RpcResult</h3>
 * <p>
 * The generated interfaces require implementors to return
 *  {@link java.util.concurrent.Future Future}&lt;{@link org.opendaylight.yangtools.yang.common.RpcResult RpcResult}&lt;{RpcName}Output&gt;&gt; instances.
 *
 * Implementations should do processing of RPC calls asynchronously and update the
 * returned {@link java.util.concurrent.Future Future} instance when processing is complete.
 * However using {@link com.google.common.util.concurrent.Futures#immediateFuture(Object) Futures.immediateFuture}
 * is valid only if the result is immediately available and asynchronous processing is unnecessary and
 * would only introduce additional complexity.
 *
 * <p>
 * The {@link org.opendaylight.yangtools.yang.common.RpcResult RpcResult} is a generic
 * wrapper for the RPC output payload, if any, and also allows for attaching error or
 * warning information (possibly along with the payload) should the RPC processing partially
 * or completely fail. This is intended to provide additional human readable information
 * for users of the API and to transfer warning / error information across the system
 * so it may be visible via other external APIs such as Restconf.
 * <p>
 * It is recommended to use the {@link org.opendaylight.yangtools.yang.common.RpcResult RpcResult}
 * for conveying appropriate error information
 * on failure rather than purposely throwing unchecked exceptions if at all possible.
 * While unchecked exceptions will fail the returned {@link java.util.concurrent.Future Future},
 * using the intended RpcResult to convey the error information is more user-friendly.
 */
public interface RpcProviderRegistry extends //
        RpcConsumerRegistry, //
        RouteChangePublisher<RpcContextIdentifier, InstanceIdentifier<?>> {
    /**
     * Registers a global implementation of the provided RPC service interface.
     * All methods of the interface are required to be implemented.
     *
     * @param serviceInterface the YANG-generated interface of the RPC Service for which to register.
     * @param implementation "the implementation of the RPC service interface.
     * @return an RpcRegistration instance that should be used to unregister the RPC implementation
     *         when no longer needed by calling {@link RpcRegistration#close()}.
     *
     * @throws IllegalStateException
     *             if the supplied RPC interface is a routed RPC type.
     */
    <T extends RpcService> RpcRegistration<T> addRpcImplementation(Class<T> serviceInterface, T implementation)
            throws IllegalStateException;

    /**
     * Registers an implementation of the given routed RPC service interface.
     * <p>
     * See the {@link RpcProviderRegistry class} documentation for information and example on
     * how to use routed RPCs.
     *
     * @param serviceInterface the YANG-generated interface of the RPC Service for which to register.
     * @param implementation the implementation instance to register.
     * @return a RoutedRpcRegistration instance which can be used to register paths for the RPC
     *         implementation via invoking {@link RoutedRpcRegistration#registerPath(....).
     *         {@link RoutedRpcRegistration#close()} should be called to unregister the implementation
     *         and all previously registered paths when no longer needed.
     *
     * @throws IllegalStateException
     *            if the supplied RPC interface is not a routed RPC type.
     */
    <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> serviceInterface,
                                                                               T implementation)
            throws IllegalStateException;
}
