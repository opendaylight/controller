package org.opendaylight.controller.config.yang.config.bundle_provider.impl;
import org.opendaylight.controller.bundle_service.impl.OpendaylightBundleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class BundleProviderModule extends org.opendaylight.controller.config.yang.config.bundle_provider.impl.AbstractBundleProviderModule {
    private static final Logger log = LoggerFactory
            .getLogger(BundleProviderModule.class);
    public BundleProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BundleProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.bundle_provider.impl.BundleProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final OpendaylightBundleProvider bundlesServiceImpl = new OpendaylightBundleProvider(
                getDataBrokerDependency(), getRpcRegistryDependency());

        bundlesServiceImpl
                .setNotificationProvider(getNotificationServiceDependency());

        // DataBroker dataBrokerService = super.getDataBrokerDependency();
        // bundlesServiceImpl.setDataProvider(dataBrokerService);

        // final ListenerRegistration<DataChangeListener>
        // dataChangeListenerRegistration = dataBrokerService
        // .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
        // bundlesServiceImpl.BUNDLE_IID,
        // bundlesServiceImpl, DataChangeScope.SUBTREE);

        // final BindingAwareBroker.RpcRegistration<BundleService>
        // rpcRegistration = getRpcRegistryDependency()
        // .addRpcImplementation(BundleService.class, bundlesServiceImpl);

        final class AutoCloseableBundle implements AutoCloseable {

            @Override
            public void close() throws Exception {
                // dataChangeListenerRegistration.close();
                closeQuietly(bundlesServiceImpl);
                // rpcRegistration.close();
            }

            private void closeQuietly(final AutoCloseable resource) {
                try {
                    resource.close();
                } catch (final Exception e) {
                    log.debug("Ignoring exception while closing {}", resource,
                            e);
                }
            }
        }

        AutoCloseable ret = new AutoCloseableBundle();
        log.info("BundlesServiceImpl (instance {}) initialized.", ret);
        return ret;
    }
}
