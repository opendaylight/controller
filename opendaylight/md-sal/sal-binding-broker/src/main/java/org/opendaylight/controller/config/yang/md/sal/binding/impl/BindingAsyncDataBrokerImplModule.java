package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedBindingDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

public class BindingAsyncDataBrokerImplModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractBindingAsyncDataBrokerImplModule implements
        Provider {

    public BindingAsyncDataBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BindingAsyncDataBrokerImplModule(
            final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModule oldModule,
            final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Broker domBroker = getDomAsyncBrokerDependency();
        BindingToNormalizedNodeCodec mappingService = getBindingMappingServiceDependency();

        // FIXME: Switch this to DOM Broker registration which would not require
        // BundleContext when API are updated.
        ProviderSession session = domBroker.registerProvider(this, null);
        DOMDataBroker domDataBroker = session.getService(DOMDataBroker.class);
        SchemaService schemaService = session.getService(SchemaService.class);
        return new ForwardedBindingDataBroker(domDataBroker, mappingService, schemaService);
    }





    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(final ProviderSession arg0) {
        // intentional NOOP
    }

}
