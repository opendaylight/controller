package org.opendaylight.controller.sal.binding.impl.forward;

import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;

interface DomForwardedBroker {

    public BindingIndependentConnector getConnector();
    
    public void setConnector(BindingIndependentConnector connector);
    
    public void setDomProviderContext(ProviderSession domProviderContext);

    public ProviderSession getDomProviderContext();

    void startForwarding();
}
