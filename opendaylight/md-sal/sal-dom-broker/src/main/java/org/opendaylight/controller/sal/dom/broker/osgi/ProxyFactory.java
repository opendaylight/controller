/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.ServiceReference;

import java.util.Arrays;

@SuppressWarnings("unchecked")
public class ProxyFactory {

    public static <T extends BrokerService> T createProxy(
            final ServiceReference<T> serviceRef, final T service) {

        Object _createProxyImpl = ProxyFactory.createProxyImpl(serviceRef,
                service);
        return ((T) _createProxyImpl);
    }

    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final DOMMountPointService service) {

        return new DOMMountPointServiceProxy(
                ((ServiceReference<DOMMountPointService>) ref), service);
    }

    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final SchemaService service) {

        return new SchemaServiceProxy(((ServiceReference<SchemaService>) ref),
                service);
    }

    private static DOMDataBrokerProxy _createProxyImpl(
            final ServiceReference<?> ref, final DOMDataBroker service) {

        return new DOMDataBrokerProxy(((ServiceReference<DOMDataBroker>) ref),
                service);
    }

    private static Object _createProxyImpl(final ServiceReference<?> reference,
            final BrokerService service) {

       return service;
    }

    private static Object createProxyImpl(final ServiceReference<?> ref,
            final BrokerService service) {

        if (service instanceof DOMDataBroker) {
            return _createProxyImpl(ref, (DOMDataBroker) service);
        } else if (service instanceof SchemaService) {
            return _createProxyImpl(ref, (SchemaService) service);
        } else if (service instanceof DOMMountPointService) {
            return _createProxyImpl(ref, (DOMMountPointService) service);
        } else if (service != null) {
            return _createProxyImpl(ref, service);
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: "
                    + Arrays.<Object> asList(ref, service).toString());
        }
    }
}