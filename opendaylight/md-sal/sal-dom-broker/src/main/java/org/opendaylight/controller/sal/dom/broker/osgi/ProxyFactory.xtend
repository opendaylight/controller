package org.opendaylight.controller.sal.dom.broker.osgi

import org.opendaylight.controller.sal.core.api.BrokerService
import org.osgi.framework.ServiceReference
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.data.DataProviderService
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService
import org.opendaylight.controller.sal.core.api.notify.NotificationService
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService

class ProxyFactory {

    static def <T extends BrokerService> T createProxy(ServiceReference<T> serviceRef, T service) {
        return createProxyImpl(serviceRef, service) as T;
    }

    private static def dispatch createProxyImpl(ServiceReference<?> ref, DataBrokerService service) {
        new DataBrokerServiceProxy(ref as ServiceReference<DataBrokerService>, service);
    }

    private static def dispatch createProxyImpl(ServiceReference<?> ref, DataProviderService service) {
        new DataProviderServiceProxy(ref as ServiceReference<DataProviderService>, service);
    }
    
    private static def dispatch createProxyImpl(ServiceReference<?> ref, NotificationPublishService service) {
        new NotificationPublishServiceProxy(ref as ServiceReference<NotificationPublishService>, service);
    }
    
    private static def dispatch createProxyImpl(ServiceReference<?> ref, NotificationService service) {
        new NotificationServiceProxy(ref as ServiceReference<NotificationService>, service);
    }

    private static def dispatch createProxyImpl(ServiceReference<?> ref, MountProvisionService service) {
        new MountProviderServiceProxy(ref as ServiceReference<MountProvisionService>, service);
    }


    private static def dispatch createProxyImpl(ServiceReference<?> ref, SchemaService service) {
        new SchemaServiceProxy(ref as ServiceReference<SchemaService>, service);
    }

    private static def dispatch createProxyImpl(ServiceReference<?> reference, BrokerService service) {
        throw new IllegalArgumentException("Not supported class");
    }

}
