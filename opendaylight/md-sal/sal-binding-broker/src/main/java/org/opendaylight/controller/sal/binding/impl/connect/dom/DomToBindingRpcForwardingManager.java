package org.opendaylight.controller.sal.binding.impl.connect.dom;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

/**
 * Manager responsible for instantiating forwarders responsible for
 * forwarding of RPC invocations from DOM Broker to Binding Aware Broker
 *
 */
class DomToBindingRpcForwardingManager implements
    RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>,
    RpcProviderRegistryImpl.RouterInstantiationListener,
    RpcProviderRegistryImpl.GlobalRpcRegistrationListener, RpcRegistrationListener {

    private final Map<Class<? extends RpcService>, DomToBindingRpcForwarder> forwarders = new WeakHashMap<>();
    private final BindingIndependentMappingService mappingService;
    private final RpcProvisionRegistry biRpcRegistry;
    private final RpcProviderRegistry baRpcRegistry;
    private RpcProviderRegistryImpl registryImpl;

    DomToBindingRpcForwardingManager(final BindingIndependentMappingService mappingService, final RpcProvisionRegistry biRpcRegistry,
        final RpcProviderRegistry baRpcRegistry) {
        this.mappingService = mappingService;
        this.biRpcRegistry = biRpcRegistry;
        this.baRpcRegistry = baRpcRegistry;
    }

    public RpcProviderRegistryImpl getRegistryImpl() {
        return registryImpl;
    }

    public void setRegistryImpl(final RpcProviderRegistryImpl registryImpl) {
        this.registryImpl = registryImpl;
    }

    @Override
    public void onGlobalRpcRegistered(final Class<? extends RpcService> cls) {
        getRpcForwarder(cls, null).registerToDOMBroker();
    }

    @Override
    public void onGlobalRpcUnregistered(final Class<? extends RpcService> cls) {
        // NOOP
    }

    @Override
    public void onRpcRouterCreated(final RpcRouter<?> router) {
        Class<? extends BaseIdentity> ctx = router.getContexts().iterator().next();
        getRpcForwarder(router.getServiceType(), ctx);
    }

    @Override
    public void onRouteChange(final RouteChange<RpcContextIdentifier, InstanceIdentifier<?>> change) {
        // Process removals first
        for (Entry<RpcContextIdentifier, Set<InstanceIdentifier<?>>> entry : change.getRemovals().entrySet()) {
            final Class<? extends BaseIdentity> context = entry.getKey().getRoutingContext();
            if (context != null) {
                final Class<? extends RpcService> service = entry.getKey().getRpcService();
                getRpcForwarder(service, context).removePaths(context, service, entry.getValue());
            }
        }

        for (Entry<RpcContextIdentifier, Set<InstanceIdentifier<?>>> entry : change.getAnnouncements().entrySet()) {
            final Class<? extends BaseIdentity> context = entry.getKey().getRoutingContext();
            if (context != null) {
                final Class<? extends RpcService> service = entry.getKey().getRpcService();
                getRpcForwarder(service, context).registerPaths(context, service, entry.getValue());
            }
        }
    }

    private DomToBindingRpcForwarder getRpcForwarder(final Class<? extends RpcService> service,
        final Class<? extends BaseIdentity> context) {
        DomToBindingRpcForwarder potential = forwarders.get(service);
        if (potential != null) {
            return potential;
        }
        if (context == null) {
            potential = new DomToBindingRpcForwarder(service, mappingService, biRpcRegistry, baRpcRegistry,registryImpl);
        } else {
            potential = new DomToBindingRpcForwarder(service, context, mappingService, biRpcRegistry, baRpcRegistry,registryImpl);
        }

        forwarders.put(service, potential);
        return potential;
    }

    @Override
    public void onRpcImplementationAdded(final QName name) {

        final Optional<Class<? extends RpcService>> rpcInterface = mappingService.getRpcServiceClassFor(
            name.getNamespace().toString(), name.getFormattedRevision());
        if (rpcInterface.isPresent()) {
            getRpcForwarder(rpcInterface.get(), null).registerToBindingBroker();
        }
    }

    @Override
    public void onRpcImplementationRemoved(final QName name) {

    }
}
