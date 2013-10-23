package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Base interface defining contract for retrieving MD-SAL
 * version of RpcServices
 * 
 */
public interface RpcConsumerRegistry {
    /**
     * Returns a session specific instance (implementation) of requested
     * YANG module implentation / service provided by consumer.
     * 
     * @param service
     *            Broker service
     * @return Session specific implementation of service
     */
    <T extends RpcService> T getRpcService(Class<T> module);
}
