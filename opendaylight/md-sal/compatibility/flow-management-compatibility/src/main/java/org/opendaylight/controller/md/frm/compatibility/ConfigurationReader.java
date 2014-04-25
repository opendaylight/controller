/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.frm.compatibility;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.FlowsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;

public class ConfigurationReader implements FlowManagementReader {

    private IForwardingRulesManager manager;

    public Flows readAllFlows() {
        List<FlowConfig> staticFlows = this.manager.getStaticFlows();
        List<Flow> flowMap = new ArrayList<Flow>(staticFlows.size());
        
        for (FlowConfig conf : staticFlows) {
            flowMap.add(FlowConfigMapping.toConfigurationFlow(conf));
        }
        final FlowsBuilder flowsBuilder = new FlowsBuilder();
        return (flowsBuilder.setFlow(flowMap)).build();
    }

    public Flow readFlow(final FlowKey key) {
        try {
            final FlowConfig flowConf = 
                    this.manager.getStaticFlow(String.valueOf(key.getId()), NodeMapping.toADNode(key.getNode()));
            return FlowConfigMapping.toConfigurationFlow(flowConf);
        } catch (Throwable _e) {
            throw new RuntimeException(_e);
        }
    }

    public IForwardingRulesManager getManager() {
        return this.manager;
    }

    public void setManager(final IForwardingRulesManager manager) {
        this.manager = manager;
    }
}
