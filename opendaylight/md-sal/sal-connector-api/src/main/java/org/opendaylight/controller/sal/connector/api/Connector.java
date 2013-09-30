package org.opendaylight.controller.sal.connector.api;

import java.util.Set;

import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;

public interface Connector extends RpcImplementation, NotificationListener {

    
    
    Set<InstanceIdentifier> getConfigurationPrefixes();
    Set<InstanceIdentifier> getRuntimePrefixes();
    
    void registerListener(ConnectorListener listener);
    void unregisterListener(ConnectorListener listener);
}
