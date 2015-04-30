package org.opendaylight.controller.remote.rpc.registry.mbeans;


import java.util.Map;
import java.util.Set;

/**
 * JMX bean to check remote rpc registry
 */

public interface RemoteRpcRegistryMXBean {

    Set<String> getGlobalRpc();

    String getBucketVersions();

    Set<String> getLocalRegisteredRoutedRpc();

    Map<String,String> findRpcByName(String name);

    Map<String,String> findRpcByRoute(String route);
}
