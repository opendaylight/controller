/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.config.yang.config.icmp.provider.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sample.pingdiscovery.dependencies.mgrs.BundleContextDependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.dependencies.mgrs.MountingServiceDependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.impl.DeviceManagerImpl;
import org.opendaylight.controller.sample.pingdiscovery.impl.DeviceMountHandlerImpl;
import org.opendaylight.controller.sample.pingdiscovery.impl.IcmpDiscoveryServiceImpl;
import org.opendaylight.controller.sample.pingdiscovery.impl.IcmpProfileManagerImpl;
import org.opendaylight.controller.sample.pingdiscovery.util.AutoCloseableManager;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.IcmpDiscoveryService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This auto-generated stub is responsible for initializing our third-part plugin logic.
 *
 * This example creates an ICMP Discovery service which, when invoked, will create nodes for
 * "pingable" devices, and will mount into those pingable devices a yang module which provides
 * additional RPC / data read functionality.
 *
 * @author Devin Avery
 * @author Greg Hall
 */
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

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        //This is the Central "Provider". Provides routing of reads/writes and rpcs calls to
        //either other implementations, or to the backing data store.
        DataProviderService dataBrokerService = getDataBrokerDependency();

        //Holds references to service retrieve via the data broker.
        //DEMOSTRATES: How to get services via the config sub system (data broker).
        final MountingServiceDependencyManager dependencyManager = new MountingServiceDependencyManager();
        getDomRegistryDependency().registerProvider(dependencyManager, bundleContext);
        closeMgr.add( dependencyManager );

        //Holds references to service retrieve via the bundle context (pure OSGi)
        //DEMOSTRATES: How to get services from OSGi's bundle context when you can't get to them
        //via the BrokerService (config sub system).
        final BundleContextDependencyManager bundleContextMgr = new BundleContextDependencyManager();
        bundleContextMgr.setBundleContext( bundleContext );
        bundleContextMgr.init();
        closeMgr.add( bundleContextMgr );

        //This is the logic which is "Mounts" an RPC implementation and Data Readers onto each
        //node when it is created.
        //TODO: Turn this into a listener, which listens for nodes to be created...
        //DEMONSTRATES: How to mount RPC and DataReaders to a single node
        final DeviceMountHandlerImpl mountHandler = new DeviceMountHandlerImpl();
        mountHandler.setDepMgr( dependencyManager );
        mountHandler.setBundleContextDepMgr( bundleContextMgr );
        mountHandler.setDataBroker(dataBrokerService);
        closeMgr.add( mountHandler );

        //The profile manager is responsible for managing the discovery profiles, including CRUD
        //of the profiles, and the managing of running discoveries.
        //DEMONSTRATES: How to read / write data to an operational data store.
        final IcmpProfileManagerImpl profMgr = new IcmpProfileManagerImpl() ;
        profMgr.setDataBrokerService(dataBrokerService);
        profMgr.setNotificationProvider(getNotificationServiceDependency());

        //Responsible for creating device in the operational data store based on discovery results.
        //DEMONSTRATES: How to create a node, with augmented data, to the operational data store.
        final DeviceManagerImpl devMgr = new DeviceManagerImpl();
        devMgr.setDataBrokerService( dataBrokerService );
        devMgr.setRpcProvider( mountHandler );

        //Implements the actual ping (discovery) service RPC which was auto generated from the yang.
        //DEMOSTRATES: Implementing a singleton RPC service from a defined yang model.
        final IcmpDiscoveryServiceImpl pingServiceImpl = new IcmpDiscoveryServiceImpl();
        pingServiceImpl.setDevMgr(devMgr, profMgr);

        //registers the above service as the implementation for the given RPC.
        final BindingAwareBroker.RpcRegistration<IcmpDiscoveryService> rpcRegistration =
                getRpcRegistryDependency()
                .addRpcImplementation(IcmpDiscoveryService.class,
                        pingServiceImpl);
        closeMgr.add( rpcRegistration );

        logger.info("IcmpDiscoveryStart provider (instance {}) initialized.");

        return closeMgr;
    }

}
