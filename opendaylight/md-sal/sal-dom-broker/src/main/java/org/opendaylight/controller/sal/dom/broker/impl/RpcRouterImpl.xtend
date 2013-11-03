package org.opendaylight.controller.sal.dom.broker.impl

import org.opendaylight.controller.sal.dom.broker.spi.RpcRouter
import org.opendaylight.yangtools.concepts.Identifiable
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.controller.sal.core.api.RpcImplementation
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import static com.google.common.base.Preconditions.*;
import java.util.Map
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration
import java.util.concurrent.ConcurrentHashMap
import java.util.Set
import java.util.Collections
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import org.opendaylight.controller.md.sal.common.impl.ListenerRegistry
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener
import org.slf4j.LoggerFactory

class RpcRouterImpl implements RpcRouter, Identifiable<String> {

    static val log = LoggerFactory.getLogger(RpcRouterImpl)

    Map<QName, RpcRegistration> implementations = new ConcurrentHashMap();

    @Property
    val Set<QName> supportedRpcs = Collections.unmodifiableSet(implementations.keySet);

    private val rpcRegistrationListeners = new ListenerRegistry<RpcRegistrationListener>();

    @Property
    val String identifier;

    new(String name) {
        _identifier = name;
    }

    override addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
    }

    override addRpcImplementation(QName rpcType, RpcImplementation implementation) throws IllegalArgumentException {
        checkNotNull(rpcType, "Rpc Type should not be null");
        checkNotNull(implementation, "Implementation should not be null.");
        checkState(!implementations.containsKey(rpcType), "Provider for supplied rpc is already registered.");
        val reg = new RpcRegistrationImpl(rpcType, implementation, this);
        implementations.put(rpcType, reg)

        for (listener : rpcRegistrationListeners.listeners) {
            try {
                listener.instance.onRpcImplementationAdded(rpcType);
            } catch (Exception e) {
                log.error("Unhandled exception during invoking listener", e);
            }
        }

        return reg;

    }

    override invokeRpc(QName rpc, CompositeNode input) {
        checkNotNull(rpc, "Rpc Type should not be null");

        val impl = implementations.get(rpc);
        checkState(impl !== null, "Provider for supplied rpc is not registered.");

        return impl.instance.invokeRpc(rpc, input);
    }

    def remove(RpcRegistrationImpl impl) {
        val existing = implementations.get(impl.type);
        if (existing == impl) {
            implementations.remove(impl.type);
        }
        for (listener : rpcRegistrationListeners.listeners) {
            try {
                listener.instance.onRpcImplementationRemoved(impl.type);
            } catch (Exception e) {
                log.error("Unhandled exception during invoking listener", e);
            }
        }
    }
    
    override addRpcRegistrationListener(RpcRegistrationListener listener) {
        rpcRegistrationListeners.register(listener);
    }

}

class RpcRegistrationImpl extends AbstractObjectRegistration<RpcImplementation> implements RpcRegistration {

    @Property
    val QName type;

    @Property
    var RpcRouterImpl router;

    new(QName type, RpcImplementation instance, RpcRouterImpl router) {
        super(instance)
        _type = type
        _router = router
    }

    override protected removeRegistration() {
        router.remove(this);
    }

}
