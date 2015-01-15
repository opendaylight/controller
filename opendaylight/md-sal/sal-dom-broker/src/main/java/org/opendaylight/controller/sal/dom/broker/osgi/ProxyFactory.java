/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import java.util.Arrays;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("unchecked")
@Deprecated
public class ProxyFactory {

    public static <T extends BrokerService> T createProxy(
            final ServiceReference<T> serviceRef, final T service) {

        final Object _createProxyImpl = ProxyFactory.createProxyImpl(serviceRef,
                service);
        return ((T) _createProxyImpl);
    }

    @Deprecated
    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final DataBrokerService service) {

        return new DataBrokerServiceProxy(
                ((ServiceReference<DataBrokerService>) ref), service);
    }

    @Deprecated
    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final DataProviderService service) {

        return new DataProviderServiceProxy(
                ((ServiceReference<DataProviderService>) ref), service);
    }

    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final NotificationPublishService service) {

        return new NotificationPublishServiceProxy(
                ((ServiceReference<NotificationPublishService>) ref), service);
    }

    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final NotificationService service) {

        return new NotificationServiceProxy(
                ((ServiceReference<NotificationService>) ref), service);
    }

    @Deprecated
    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final MountProvisionService service) {

        return new MountProviderServiceProxy(
                ((ServiceReference<MountProvisionService>) ref), service);
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

    private static Object _createProxyImpl(final ServiceReference<?> ref,
            final RpcProvisionRegistry service) {

        return new RpcProvisionRegistryProxy(
                ((ServiceReference<RpcProvisionRegistry>) ref), service);
    }

    private static DOMDataBrokerProxy _createProxyImpl(
            final ServiceReference<?> ref, final DOMDataBroker service) {

        return new DOMDataBrokerProxy(((ServiceReference<DOMDataBroker>) ref),
                service);
    }

    private static Object _createProxyImpl(final ServiceReference<?> reference,
            final BrokerService service) {

        throw new IllegalArgumentException("Not supported class: "
                + service.getClass().getName());
    }

    private static Object createProxyImpl(final ServiceReference<?> ref,
            final BrokerService service) {

        if (service instanceof DOMDataBroker) {
            return _createProxyImpl(ref, (DOMDataBroker) service);
        } else if (service instanceof RpcProvisionRegistry) {
            return _createProxyImpl(ref, (RpcProvisionRegistry) service);
        } else if (service instanceof DataProviderService) {
            return _createProxyImpl(ref, (DataProviderService) service);
        } else if (service instanceof MountProvisionService) {
            return _createProxyImpl(ref, (MountProvisionService) service);
        } else if (service instanceof NotificationPublishService) {
            return _createProxyImpl(ref, (NotificationPublishService) service);
        } else if (service instanceof DataBrokerService) {
            return _createProxyImpl(ref, (DataBrokerService) service);
        } else if (service instanceof SchemaService) {
            return _createProxyImpl(ref, (SchemaService) service);
        } else if (service instanceof NotificationService) {
            return _createProxyImpl(ref, (NotificationService) service);
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