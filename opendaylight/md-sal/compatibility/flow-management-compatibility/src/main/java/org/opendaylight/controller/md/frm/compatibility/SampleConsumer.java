/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.frm.compatibility;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SampleConsumer {

    ConsumerContext context;

    void addFlowExample() {

        DataBrokerService dataService = context.getSALService(DataBrokerService.class);

        DataModificationTransaction transaction = dataService.beginTransaction();
        Flow flow = createSampleFlow("foo", null);
        InstanceIdentifier<Flow> path = InstanceIdentifier.builder(Flows.class).child(Flow.class, flow.getKey())
                .toInstance();
        transaction.putConfigurationData(path, flow);

        transaction.commit();

        dataService.readConfigurationData(path);
    }

    Flow createSampleFlow(String name, NodeRef node) {
        FlowBuilder ret = new FlowBuilder();
        FlowKey key = new FlowKey(Long.parseLong(name), node);
        ret.setKey(key);
        return ret.build();
    }
}
