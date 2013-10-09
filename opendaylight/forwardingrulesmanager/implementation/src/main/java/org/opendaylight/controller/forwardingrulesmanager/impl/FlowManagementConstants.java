package org.opendaylight.controller.forwardingrulesmanager.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class FlowManagementConstants {

    
    public static final InstanceIdentifier<Flows> FLOWS_PATH = new InstanceIdentifier<Flows>(Flows.class);
    
    private FlowManagementConstants() {
        throw new UnsupportedOperationException();
    }
}
