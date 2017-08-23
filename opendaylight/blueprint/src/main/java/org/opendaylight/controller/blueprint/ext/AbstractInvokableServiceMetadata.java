/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.spi.rpc.RpcRoutingStrategy;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.osgi.service.blueprint.container.ComponentDefinitionException;

abstract class AbstractInvokableServiceMetadata extends AbstractDependentComponentFactoryMetadata {
    private final String interfaceName;

    private ListenerRegistration<DOMRpcAvailabilityListener> rpcListenerReg;
    private RpcProviderRegistry rpcRegistry;
    private Class<RpcService> rpcInterface;
    private Set<SchemaPath> rpcSchemaPaths;

    AbstractInvokableServiceMetadata(final String id, final String interfaceName) {
        super(id);
        this.interfaceName = Preconditions.checkNotNull(interfaceName);
    }

    Class<RpcService> rpcInterface() {
        return rpcInterface;
    }

    @SuppressWarnings({ "checkstyle:IllegalCatch", "unchecked" })
    @Override
    public final void init(final ExtendedBlueprintContainer container) {
        super.init(container);

        final Class<?> interfaceClass;
        try {
            interfaceClass = container().getBundleContext().getBundle().loadClass(interfaceName);
        } catch (final Exception e) {
            throw new ComponentDefinitionException(String.format("%s: Error obtaining interface class %s",
                    logName(), interfaceName), e);
        }

        if (!RpcService.class.isAssignableFrom(interfaceClass)) {
            throw new ComponentDefinitionException(String.format(
                "%s: The specified interface %s is not an RpcService", logName(), interfaceName));
        }

        rpcInterface = (Class<RpcService>)interfaceClass;
    }

    @Override
    protected final void startTracking() {
        // Request RpcProviderRegistry first ...
        retrieveService("RpcProviderRegistry", RpcProviderRegistry.class, this::onRpcRegistry);
    }

    private void onRpcRegistry(final Object service) {
        log.debug("{}: Retrieved RpcProviderRegistry {}", logName(), service);
        rpcRegistry = (RpcProviderRegistry)service;

        // Now acquire SchemaService...
        retrieveService("SchemaService", SchemaService.class, this::onSchemaService);
    }

    private void onSchemaService(final Object service) {
        log.debug("{}: Retrieved SchemaService {}", logName(), service);

        // Now get the SchemaContext and trigger RPC resolution
        retrievedSchemaContext(((SchemaService)service).getGlobalContext());
    }

    private void retrievedSchemaContext(final SchemaContext schemaContext) {
        log.debug("{}: retrievedSchemaContext", logName());

        final Collection<SchemaPath> schemaPaths = RpcUtil.decomposeRpcService(rpcInterface, schemaContext,
            rpcFilter());
        if (schemaPaths.isEmpty()) {
            log.warn("{}: interface {} has no accptable entries, assuming it is satisfied", logName(), rpcInterface);
            setSatisfied();
            return;
        }

        rpcSchemaPaths = ImmutableSet.copyOf(schemaPaths);
        log.debug("{}: Got SchemaPaths: {}", logName(), rpcSchemaPaths);

        // First get the DOMRpcService OSGi service. This will be used to register a listener to be notified
        // when the underlying DOM RPC service is available.
        retrieveService("DOMRpcService", DOMRpcService.class, this::retrievedDOMRpcService);
    }

    private void retrievedDOMRpcService(final Object service) {
        log.debug("{}: retrievedDOMRpcService {}", logName(), service);
        final DOMRpcService domRpcService = (DOMRpcService)service;

        setDependencyDesc("Available DOM RPC for binding RPC: " + rpcInterface);
        rpcListenerReg = domRpcService.registerRpcListener(new DOMRpcAvailabilityListener() {
            @Override
            public void onRpcAvailable(final Collection<DOMRpcIdentifier> rpcs) {
                onRpcsAvailable(rpcs);
            }

            @Override
            public void onRpcUnavailable(final Collection<DOMRpcIdentifier> rpcs) {
            }
        });
    }

    abstract Predicate<RpcRoutingStrategy> rpcFilter();

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public final Object create() throws ComponentDefinitionException {
        log.debug("{}: In create: interfaceName: {}", logName(), interfaceName);

        super.onCreate();

        try {
            RpcService rpcService = rpcRegistry.getRpcService(rpcInterface);

            log.debug("{}: create returning service {}", logName(), rpcService);

            return rpcService;
        } catch (final RuntimeException e) {
            throw new ComponentDefinitionException("Error getting RPC service for " + interfaceName, e);
        }
    }

    protected final void onRpcsAvailable(final Collection<DOMRpcIdentifier> rpcs) {
        for (DOMRpcIdentifier identifier : rpcs) {
            if (rpcSchemaPaths.contains(identifier.getType())) {
                log.debug("{}: onRpcsAvailable - found SchemaPath {}", logName(), identifier.getType());
                setSatisfied();
                break;
            }
        }
    }

    @Override
    public final void stopTracking() {
        super.stopTracking();
        closeRpcListenerReg();
    }

    private void closeRpcListenerReg() {
        if (rpcListenerReg != null) {
            rpcListenerReg.close();
            rpcListenerReg = null;
        }
    }

    @Override
    public final void destroy(final Object instance) {
        super.destroy(instance);
        closeRpcListenerReg();
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("id", getId()).add("interfaceName", interfaceName).toString();
    }
}
