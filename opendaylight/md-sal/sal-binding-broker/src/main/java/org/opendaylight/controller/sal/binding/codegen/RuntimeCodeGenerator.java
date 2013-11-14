/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen;

import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;
import org.opendaylight.controller.sal.binding.spi.RpcRouter;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.annotations.RoutingContext;

public interface RuntimeCodeGenerator {

    /**
     * Returns an instance of provided RpcService type which delegates all calls
     * to the delegate.
     * 
     * <p>
     * Returned instance:
     * <ul>
     * <li>implements provided subclass of RpcService type and
     * {@link DelegateProxy} interface.
     * <li>
     * <p>
     * delegates all invocations of methods, which are defined in RpcService
     * subtype to delegate which is defined by
     * {@link DelegateProxy#setDelegate(Object)}.
     * <p>
     * If delegate is not defined (<code>getDelegate() == null</code>)
     * implementation throws {@link IllegalStateException}
     * <li>{@link DelegateProxy#getDelegate()} - returns the delegate to which
     * all calls are delegated.
     * <li>{@link DelegateProxy#setDelegate(Object)} - sets the delegate for
     * particular instance
     * 
     * </ul>
     * 
     * @param serviceType
     *            - Subclass of RpcService for which direct proxy is to be
     *            generated.
     * @return Instance of RpcService of provided serviceType which implements
     *         and {@link DelegateProxy}
     * @throws IllegalArgumentException
     * 
     */
    <T extends RpcService> T getDirectProxyFor(Class<T> serviceType) throws IllegalArgumentException;

    /**
     * Returns an instance of provided RpcService type which routes all calls to
     * other instances selected on particular input field.
     * 
     * <p>
     * Returned instance:
     * <ul>
     * <li>Implements:
     * <ul>
     * <li>{@link DelegateProxy}
     * <li>{@link RpcRouter}
     * </ul>
     * <li>
     * routes all invocations of methods, which are defined in RpcService
     * subtype based on method arguments and routing information defined in the
     * RpcRoutingTables for this instance
     * {@link RpcRouter#getRoutingTable(Class)}.
     * <ul>
     * <li>
     * Implementation uses
     * {@link RpcRouter#getService(Class, InstanceIdentifier)} method to
     * retrieve particular instance to which call will be routed.
     * <li>
     * Instance of {@link InstanceIdentifier} is determined by first argument of
     * method and is retrieved via method which is annotated with
     * {@link RoutingContext}. Class representing Routing Context Identifier is
     * retrieved by {@link RoutingContext}.
     * <li>If first argument is not defined / {@link RoutingContext} annotation
     * is not present on any field invocation will be delegated to default
     * service {@link RpcRouter#getDefaultService()}.
     * </ul>
     * 
     * @param serviceType
     *            - Subclass of RpcService for which Router is to be generated.
     * @return Instance of RpcService of provided serviceType which implements
     *         also {@link RpcRouter}<T> and {@link DelegateProxy}
     */
    <T extends RpcService> RpcRouter<T> getRouterFor(Class<T> serviceType) throws IllegalArgumentException;

    NotificationInvokerFactory getInvokerFactory();
}
