package org.opendaylight.controller.sal.core.api;

import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.yangtools.yang.common.QName;

public interface RpcProvisionRegistry {

    /**
     * Registers an implementation of the rpc.
     * 
     * <p>
     * The registered rpc functionality will be available to all other
     * consumers and providers registered to the broker, which are aware of
     * the {@link QName} assigned to the rpc.
     * 
     * <p>
     * There is no assumption that rpc type is in the set returned by
     * invoking {@link RpcImplementation#getSupportedRpcs()}. This allows
     * for dynamic rpc implementations.
     * 
     * @param rpcType
     *            Name of Rpc
     * @param implementation
     *            Provider's Implementation of the RPC functionality
     * @throws IllegalArgumentException
     *             If the name of RPC is invalid
     */
    RpcRegistration addRpcImplementation(QName rpcType, RpcImplementation implementation)
            throws IllegalArgumentException;

    RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation);
}
