/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory metadata corresponding to the "rpc-service" element that gets an RPC service implementation from
 * the RpcProviderRegistry and provides it to the Blueprint container.
 *
 * @author Thomas Pantelis
 */
class RpcServiceMetadata extends AbstractDependentComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(RpcServiceMetadata.class);

    private final String interfaceName;
    private volatile Set<SchemaPath> rpcSchemaPaths;
    private volatile RpcProviderRegistry rpcRegistry;
    private volatile ListenerRegistration<DOMRpcAvailabilityListener> rpcListenerReg;
    private volatile Class<RpcService> rpcInterface;

    RpcServiceMetadata(String id, String interfaceName) {
        super(id);
        this.interfaceName = interfaceName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(ExtendedBlueprintContainer container) {
        super.init(container);

        try {
            Class<?> interfaceClass = container().getBundleContext().getBundle().loadClass(interfaceName);
            if(!RpcService.class.isAssignableFrom(interfaceClass)) {
                throw new ComponentDefinitionException(String.format(
                        "%s: The specified interface %s is not an RpcService", logName(), interfaceName));
            }

            rpcInterface = (Class<RpcService>)interfaceClass;
        } catch(Exception e) {
            throw new ComponentDefinitionException(String.format("%s: Error obtaining interface class %s",
                    logName(), interfaceName), e);
        }
    }

    @Override
    protected void startTracking() {
        // First get the SchemaContext. This will be used to get the RPC SchemaPaths.

        retrieveService("SchemaService", SchemaService.class,
                service -> retrievedSchemaContext(((SchemaService)service).getGlobalContext()));
    }

    private void retrievedSchemaContext(SchemaContext schemaContext) {
        LOG.debug("{}: retrievedSchemaContext", logName());

        QNameModule moduleName = BindingReflections.getQNameModule(rpcInterface);
        Module module = schemaContext.findModuleByNamespaceAndRevision(moduleName.getNamespace(), moduleName.getRevision());

        LOG.debug("{}: Got Module: {}", logName(), module);

        rpcSchemaPaths = new HashSet<>();
        for(RpcDefinition rpcDef : module.getRpcs()) {
            rpcSchemaPaths.add(rpcDef.getPath());
        }

        LOG.debug("{}: Got SchemaPaths: {}", logName(), rpcSchemaPaths);

        // First get the DOMRpcService OSGi service. This will be used to register a listener to be notified
        // when the underlying DOM RPC service is available.

        retrieveService("DOMRpcService", DOMRpcService.class, service -> retrievedDOMRpcService((DOMRpcService)service));
    }

    private void retrievedDOMRpcService(DOMRpcService domRpcService) {
        LOG.debug("{}: retrievedDOMRpcService", logName());

        rpcListenerReg = domRpcService.registerRpcListener(new DOMRpcAvailabilityListener() {
            @Override
            public void onRpcAvailable(Collection<DOMRpcIdentifier> rpcs) {
                onRpcsAvailable(rpcs);
            }

            @Override
            public void onRpcUnavailable(Collection<DOMRpcIdentifier> rpcs) {
            }
        });
    }

    protected void onRpcsAvailable(Collection<DOMRpcIdentifier> rpcs) {
        for(DOMRpcIdentifier identifier: rpcs) {
            if(rpcSchemaPaths.contains(identifier.getType())) {
                LOG.debug("{}: onRpcsAvailable - found SchemaPath {}", logName(), identifier.getType());

                retrieveService("RpcProviderRegistry", RpcProviderRegistry.class, service -> {
                    rpcRegistry = (RpcProviderRegistry)service;
                    setSatisfied();
                });

                break;
            }
        }
    }

    @Override
    public Object create() throws ComponentDefinitionException {
        LOG.debug("{}: In create: interfaceName: {}", logName(), interfaceName);

        super.onCreate();

        try {
            RpcService rpcService = rpcRegistry.getRpcService(rpcInterface);

            LOG.debug("{}: create returning service {}", logName(), rpcService);

            return rpcService;
        } catch(Exception e) {
            throw new ComponentDefinitionException("Error getting RPC service for " + interfaceName, e);
        }
    }

    @Override
    public void stopTracking() {
        super.stopTracking();
        closeRpcListenerReg();
    }

    private void closeRpcListenerReg() {
        if(rpcListenerReg != null) {
            rpcListenerReg.close();
            rpcListenerReg = null;
        }
    }

    @Override
    public void destroy(Object instance) {
        super.destroy(instance);
        closeRpcListenerReg();
    }

    @Override
    public String toString() {
        return "RpcServiceMetadata [id=" + getId() + ", interfaceName=" + interfaceName + "]";
    }
}
