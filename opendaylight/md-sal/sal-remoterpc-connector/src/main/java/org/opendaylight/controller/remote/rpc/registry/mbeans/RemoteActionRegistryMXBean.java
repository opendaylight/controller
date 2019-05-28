package org.opendaylight.controller.remote.rpc.registry.mbeans;

import java.util.Map;
import java.util.Set;

public interface RemoteActionRegistryMXBean {

    String getBucketVersions();

    Set<String> getLocalRegisteredAction();

    Map<String,String> findActionByName(String name);

    Map<String,String> findActionByRoute(String route);
}
