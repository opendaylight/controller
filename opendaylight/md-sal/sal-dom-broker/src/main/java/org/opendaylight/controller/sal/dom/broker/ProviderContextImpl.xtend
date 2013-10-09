package org.opendaylight.controller.sal.dom.broker

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession
import org.opendaylight.controller.sal.core.api.Provider
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.controller.sal.core.api.RpcImplementation
import org.osgi.framework.BundleContext
import static java.util.Collections.*
import java.util.Collections
import java.util.HashMap

class ProviderContextImpl extends ConsumerContextImpl implements ProviderSession {

    @Property
    private val Provider provider;

    private val rpcImpls = Collections.synchronizedMap(new HashMap<QName, RpcImplementation>());

    new(Provider provider, BundleContext ctx) {
        super(null, ctx);
        this._provider = provider;
    }

    override addRpcImplementation(QName rpcType, RpcImplementation implementation) throws IllegalArgumentException {
        if(rpcType == null) {
            throw new IllegalArgumentException("rpcType must not be null");
        }
        if(implementation == null) {
            throw new IllegalArgumentException("Implementation must not be null");
        }
        broker.addRpcImplementation(rpcType, implementation);
        rpcImpls.put(rpcType, implementation);
        //FIXME: Return registration
        return null;
    }

    def removeRpcImplementation(QName rpcType, RpcImplementation implToRemove) throws IllegalArgumentException {
        val localImpl = rpcImpls.get(rpcType);
        if(localImpl != implToRemove) {
            throw new IllegalStateException(
                "Implementation was not registered in this session");
        }

        broker.removeRpcImplementation(rpcType, implToRemove);
        rpcImpls.remove(rpcType);
    }
    
    override addMountedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
    override addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
}
