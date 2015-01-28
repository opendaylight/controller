package org.opendaylight.controller.config.yang.config.features_service.impl;

import org.opendaylight.controller.features_service.impl.FeaturesServiceImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeaturesServiceImplModule extends org.opendaylight.controller.config.yang.config.features_service.impl.AbstractFeaturesServiceImplModule {

    private static final Logger log = LoggerFactory.getLogger(FeaturesServiceImplModule.class);

    public FeaturesServiceImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public FeaturesServiceImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.features_service.impl.FeaturesServiceImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final FeaturesServiceImpl featuresServiceImpl = new FeaturesServiceImpl();

        // Register to md-sal
        featuresServiceImpl.setNotificationProvider(getNotificationServiceDependency());

        DataBroker dataBrokerService = getDataBrokerDependency();
        featuresServiceImpl.setDataProvider(dataBrokerService);
        
        final ListenerRegistration<DataChangeListener> dataChangeListenerRegistration = 
                dataBrokerService.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION, 
                        FeaturesServiceImpl.FEATURE_SERVICE_IID, featuresServiceImpl, DataChangeScope.SUBTREE );

        final BindingAwareBroker.RpcRegistration<org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.FeaturesService> rpcRegistration = getRpcRegistryDependency()
                .addRpcImplementation(org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.FeaturesService.class, featuresServiceImpl);
        
        final class AutoCloseableFeaturesService implements AutoCloseable {

            @Override
            public void close() throws Exception {
                dataChangeListenerRegistration.close();
                featuresServiceImpl.close();
                // Close the RPC methods
                rpcRegistration.close();
                //TODO uncomment
                //runtimeReg.close();
                log.info("FeaturesServiceModule torn down.");
            }
        }
        
        return new AutoCloseableFeaturesService();
    }

}
