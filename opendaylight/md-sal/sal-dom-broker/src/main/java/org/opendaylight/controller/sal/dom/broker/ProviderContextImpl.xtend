package org.opendaylight.controller.sal.dom.broker

import java.util.Collections
import java.util.HashMap
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession
import org.opendaylight.controller.sal.core.api.Provider
import org.opendaylight.controller.sal.core.api.RpcImplementation
import org.opendaylight.yangtools.yang.common.QName
import org.osgi.framework.BundleContext
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration
import static java.util.Collections.*
import java.util.Collections
import java.util.HashMap
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener

class ProviderContextImpl extends ConsumerContextImpl implements ProviderSession {

    @Property
    private val Provider provider;

    private val rpcImpls = Collections.synchronizedMap(new HashMap<QName, RpcImplementation>());

    new(Provider provider, BundleContext ctx) {
        super(null, ctx);
        this._provider = provider;
    }

    override addRpcImplementation(QName rpcType, RpcImplementation implementation) throws IllegalArgumentException {
        if (rpcType == null) {
            throw new IllegalArgumentException("rpcType must not be null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation must not be null");
        }
        broker.addRpcImplementation(rpcType, implementation);
        rpcImpls.put(rpcType, implementation);

        return new RpcRegistrationImpl(rpcType, implementation, this);
    }

    def removeRpcImplementation(RpcRegistrationImpl implToRemove) throws IllegalArgumentException {
        val localImpl = rpcImpls.get(implToRemove.type);
        if (localImpl !== implToRemove.instance) {
            throw new IllegalStateException("Implementation was not registered in this session");
        }
        broker.removeRpcImplementation(implToRemove.type, localImpl);
        rpcImpls.remove(implToRemove.type);
    }

    override close() {
        removeAllRpcImlementations
        super.close
    }

    private def removeAllRpcImlementations() {
        if (!rpcImpls.empty) {
            for (entry : rpcImpls.entrySet) {
                broker.removeRpcImplementation(entry.key, entry.value);
            }
            rpcImpls.clear
        }
    }

    override addMountedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getSupportedRpcs() {
        broker.getSupportedRpcs();
    }

    override addRpcRegistrationListener(RpcRegistrationListener listener) {
        broker.addRpcRegistrationListener(listener);
    }
}

class RpcRegistrationImpl extends AbstractObjectRegistration<RpcImplementation> implements RpcRegistration {

    @Property
    val QName type

    private var ProviderContextImpl context

    new(QName type, RpcImplementation instance, ProviderContextImpl ctx) {
        super(instance)
        _type = type
        context = ctx
    }

    override protected removeRegistration() {
        context.removeRpcImplementation(this)
        context = null
    }

}
