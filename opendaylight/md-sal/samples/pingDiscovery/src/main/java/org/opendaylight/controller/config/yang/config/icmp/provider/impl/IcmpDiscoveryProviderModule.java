package org.opendaylight.controller.config.yang.config.icmp.provider.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sample.pingdiscovery.DependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.IcmpDiscoveryServiceImpl;
import org.opendaylight.controller.sample.pingdiscovery.impl.DeviceManagerImpl;
import org.opendaylight.controller.sample.pingdiscovery.impl.DeviceMountHandlerImpl;
import org.opendaylight.controller.sample.pingdiscovery.impl.IcmpProfileManagerImpl;
import org.opendaylight.controller.sample.pingdiscovery.util.AutoCloseableManager;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.IcmpDiscoveryService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcmpDiscoveryProviderModule
extends
org.opendaylight.controller.config.yang.config.icmp.provider.impl.AbstractIcmpDiscoveryProviderModule {

    private final Logger logger = LoggerFactory.getLogger(IcmpDiscoveryProviderModule.class);
    private BundleContext bundleContext;
    private final AutoCloseableManager closeMgr = new AutoCloseableManager();

    public IcmpDiscoveryProviderModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public IcmpDiscoveryProviderModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.icmp.provider.impl.IcmpDiscoveryProviderModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        DataProviderService dataBrokerService = getDataBrokerDependency();

        //holds references to dependecy managers
        final DependencyManager dependencyManager = new DependencyManager();
        dependencyManager.initDependenciesFromBundleContext( bundleContext );
        getDomRegistryDependency().registerProvider(dependencyManager, bundleContext);
        closeMgr.add( dependencyManager );

        //responsible for mounting IcmpData mountpoint into the deices.
        final DeviceMountHandlerImpl mountHandler = new DeviceMountHandlerImpl();
        mountHandler.setDepMgr( dependencyManager );
        mountHandler.setDataBroker(dataBrokerService);
        closeMgr.add( mountHandler );

        //Responsible for creating profile in config data store.
        final IcmpProfileManagerImpl profMgr = new IcmpProfileManagerImpl() ;
        profMgr.setDataBrokerService(dataBrokerService);
        profMgr.setNotificationProvider(getNotificationServiceDependency());

        //Responsible for creating device in the operational data store based on discovery results.
        final DeviceManagerImpl devMgr = new DeviceManagerImpl();
        devMgr.setDataBrokerService( dataBrokerService );
        devMgr.setRpcProvider( mountHandler );

        //Implements the actual ping (discovery) service.
        final IcmpDiscoveryServiceImpl pingServiceImpl = new IcmpDiscoveryServiceImpl();
        pingServiceImpl.setDevMgr(devMgr, profMgr);
        final BindingAwareBroker.RpcRegistration<IcmpDiscoveryService> rpcRegistration =
                getRpcRegistryDependency()
                .addRpcImplementation(IcmpDiscoveryService.class,
                        pingServiceImpl);
        closeMgr.add( rpcRegistration );

        logger.info("IcmpDiscoveryStart provider (instance {}) initialized.");

        return closeMgr;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
