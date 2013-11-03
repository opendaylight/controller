package org.opendaylight.controller.sal.dom.broker

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession
import org.opendaylight.controller.sal.core.api.Provider
import org.opendaylight.controller.sal.core.api.RpcImplementation
import org.opendaylight.yangtools.yang.common.QName
import org.osgi.framework.BundleContext
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener
import org.opendaylight.yangtools.concepts.Registration

import java.util.Set
import java.util.HashSet

class ProviderContextImpl extends ConsumerContextImpl implements ProviderSession {

    @Property
    private val Provider provider;

    private val Set<Registration<?>> registrations = new HashSet();

    new(Provider provider, BundleContext ctx) {
        super(null, ctx);
        this._provider = provider;
    }

    override addRpcImplementation(QName rpcType, RpcImplementation implementation) throws IllegalArgumentException {
        val origReg = broker.router.addRpcImplementation(rpcType, implementation);
        val newReg = new RpcRegistrationWrapper(origReg);
        registrations.add(newReg);
        return newReg;
    }

    protected def removeRpcImplementation(RpcRegistrationWrapper implToRemove) throws IllegalArgumentException {
        registrations.remove(implToRemove);
    }
    
    override close() {
        
        for (reg : registrations) {
            reg.close()
        }
        super.close
    }

    override addMountedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getSupportedRpcs() {
        broker.router.supportedRpcs;
    }

    override addRpcRegistrationListener(RpcRegistrationListener listener) {
        broker.router.addRpcRegistrationListener(listener);
    }
}

class RpcRegistrationWrapper implements RpcRegistration {


    @Property
    val RpcRegistration delegate

    new(RpcRegistration delegate) {
        _delegate = delegate
    }

    override getInstance() {
        delegate.instance
    }

    override close() {
        delegate.close
    }

    override getType() {
        delegate.type
    }
}

