/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import java.util.Hashtable;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.compat.BackwardsCompatibleDataBroker;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareDataStoreAdapter;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProviders;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class BrokerConfigActivator implements AutoCloseable {

    private static InstanceIdentifier ROOT = InstanceIdentifier.builder()
            .toInstance();

    private DataProviderService dataService;

    private ServiceRegistration<DataBrokerService> dataReg = null;
    private ServiceRegistration<DataProviderService> dataProviderReg = null;
    private ServiceRegistration<MountService> mountReg = null;
    private ServiceRegistration<MountProvisionService> mountProviderReg = null;
    private SchemaService schemaService = null;
    private ServiceRegistration<RpcProvisionRegistry> rpcProvisionRegistryReg = null;
    private MountPointManagerImpl mountService = null;

    private SchemaAwareDataStoreAdapter wrappedStore = null;

    public void start(final BrokerImpl broker, final DataStore store,
            final DOMDataBroker asyncBroker, final BundleContext context) {

        final Hashtable<String, String> emptyProperties = new Hashtable<String, String>();
        broker.setBundleContext(context);

        final ServiceReference<SchemaService> serviceRef = context
                .getServiceReference(SchemaService.class);
        schemaService = context.<SchemaService> getService(serviceRef);

        broker.setRouter(new SchemaAwareRpcBroker("/", SchemaContextProviders
                .fromSchemaService(schemaService)));

        if (asyncBroker == null) {
            dataService = new DataBrokerImpl();
            dataProviderReg = context.registerService(
                    DataProviderService.class, dataService, emptyProperties);

            wrappedStore = new SchemaAwareDataStoreAdapter();
            wrappedStore.changeDelegate(store);
            wrappedStore.setValidationEnabled(false);
            context.registerService(SchemaServiceListener.class, wrappedStore,
                    emptyProperties);

            dataService.registerConfigurationReader(ROOT, wrappedStore);
            dataService.registerCommitHandler(ROOT, wrappedStore);
            dataService.registerOperationalReader(ROOT, wrappedStore);
        } else {
            BackwardsCompatibleDataBroker compatibleDataBroker = new BackwardsCompatibleDataBroker(
                    asyncBroker);
            context.registerService(SchemaServiceListener.class,
                    compatibleDataBroker, emptyProperties);
            dataService = compatibleDataBroker;
        }

        mountService = new MountPointManagerImpl();
        dataReg = context.registerService(DataBrokerService.class, dataService,
                emptyProperties);
        mountReg = context.registerService(MountService.class, mountService,
                emptyProperties);
        mountProviderReg = context.registerService(MountProvisionService.class,
                mountService, emptyProperties);

        rpcProvisionRegistryReg = context
                .registerService(RpcProvisionRegistry.class,
                        broker.getRouter(), emptyProperties);
    }

    @Override
    public void close() {

        if (dataReg != null) {
            dataReg.unregister();
            dataReg = null;
        }
        if (dataProviderReg != null) {
            dataProviderReg.unregister();
            dataProviderReg = null;
        }
        if (mountReg != null) {
            mountReg.unregister();
            mountReg = null;
        }
        if (mountProviderReg != null) {
            mountProviderReg.unregister();
            mountProviderReg = null;
        }
        if (rpcProvisionRegistryReg != null) {
            rpcProvisionRegistryReg.unregister();
            rpcProvisionRegistryReg = null;
        }
    }

    /**
     * @return the dataService
     */
    public DataProviderService getDataService() {
        return dataService;
    }

    /**
     * @param dataService
     *            the dataService to set
     */
    public void setDataService(final DataProviderService dataService) {
        this.dataService = dataService;
    }
}
