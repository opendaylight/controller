package org.opendaylight.controller.sal.dom.broker.spi;

import java.util.Set;

import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface RpcRouter extends RpcProvisionRegistry, RpcImplementation {

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation);
    
    @Override
    public RpcRegistration addRpcImplementation(QName rpcType, RpcImplementation implementation)
            throws IllegalArgumentException;
    
    @Override
    public Set<QName> getSupportedRpcs();
    
    @Override
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input);

    ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(RpcRegistrationListener listener);
}
