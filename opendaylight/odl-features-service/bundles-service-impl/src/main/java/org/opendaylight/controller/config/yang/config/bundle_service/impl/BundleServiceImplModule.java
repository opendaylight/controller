package org.opendaylight.controller.config.yang.config.bundle_service.impl;

import org.opendaylight.controller.bundle_service.impl.BundleServiceImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.BundleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleServiceImplModule extends org.opendaylight.controller.config.yang.config.bundle_service.impl.AbstractBundleServiceImplModule {
	private static final Logger log = LoggerFactory.getLogger(BundleServiceImplModule.class);
    public BundleServiceImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, 
    		                        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BundleServiceImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, 
    		                        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, 
    		                        final org.opendaylight.controller.config.yang.config.bundle_service.impl.BundleServiceImplModule oldModule, 
    		                        final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
    	final BundleServiceImpl bundlesServiceImpl = new BundleServiceImpl(getDataBrokerDependency(), getRpcRegistryDependency());

    	bundlesServiceImpl.setNotificationProvider(getNotificationServiceDependency());

//    	DataBroker dataBrokerService = super.getDataBrokerDependency();
//    	bundlesServiceImpl.setDataProvider(dataBrokerService);
    	
//        final ListenerRegistration<DataChangeListener> dataChangeListenerRegistration = dataBrokerService
//                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, bundlesServiceImpl.BUNDLE_IID,
//                        bundlesServiceImpl, DataChangeScope.SUBTREE);

//    	final BindingAwareBroker.RpcRegistration<BundleService> rpcRegistration = getRpcRegistryDependency()
//                .addRpcImplementation(BundleService.class, bundlesServiceImpl);

    	final class AutoCloseableBundle implements AutoCloseable {

            @Override
            public void close() throws Exception {
                //dataChangeListenerRegistration.close();
                closeQuietly(bundlesServiceImpl);
//                rpcRegistration.close();
            }

            private void closeQuietly(final AutoCloseable resource) {
                try {
                    resource.close();
                } catch (final Exception e) {
                    log.debug("Ignoring exception while closing {}", resource, e);
                }
            }
        }

    	AutoCloseable ret =  new AutoCloseableBundle();
    	log.info("BundlesServiceImpl (instance {}) initialized.", ret);
    	System.out.println("createInstance() done");
    	return ret;
    }

}
