/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.frm.compatibility;

import java.text.MessageFormat;
import java.util.Iterator;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.sal.compatibility.MDFlowMapping;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowAdded;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowConfigMapping {

    private final static Logger LOG = LoggerFactory.getLogger(FlowConfigMapping.class);

    /* nodes/node/flow  -> 0 / 1 / 2  */
    private static final int FLOW_KEY_IDENTIFIER_DEEP = 2;

    public static Flow toConfigurationFlow(final FlowConfig sourceCfg) {
        final FlowAdded source = MDFlowMapping.flowAdded(sourceCfg.getFlow());
        final FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setInstructions(source.getInstructions());
        flowBuilder.setCookie(source.getCookie());
        flowBuilder.setHardTimeout(source.getHardTimeout());
        flowBuilder.setIdleTimeout(source.getIdleTimeout());
        flowBuilder.setMatch(source.getMatch());
        flowBuilder.setNode(source.getNode());

        FlowKey flowKey =
                new FlowKey(Long.valueOf(sourceCfg.getName()), flowBuilder.getNode());
        flowBuilder.setKey(flowKey);
        return flowBuilder.build();
    }

    public static FlowConfig toFlowConfig(final Flow sourceCfg) {
        try {
            final FlowConfig flowConfig = new FlowConfig();
            flowConfig.setName(String.valueOf(sourceCfg.getId()));
            flowConfig.setNode(NodeMapping.toADNode(sourceCfg.getNode()));
            return flowConfig;
        } catch (Exception e) {
            String errMsg = MessageFormat.format("Convert from Flow {} to FlowConfig fail", sourceCfg);
            LOG.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    public static FlowConfig toFlowConfig(final InstanceIdentifier<? extends Object> identifier) {
        try {
            PathArgument pathArg = FlowConfigMapping.getSecondPathArgumentFromPath(identifier);
            if (pathArg != null) {
                final FlowConfig flowConfig = new FlowConfig();
                FlowKey key = ((IdentifiableItem<Flow, FlowKey>) pathArg).getKey();
                flowConfig.setName(String.valueOf(key.getId()));
                flowConfig.setNode(NodeMapping.toADNode(key.getNode()));
                return flowConfig;
            }
            return null;
        } catch (Exception e) {
            String errMsg = MessageFormat.format("Convert from InstanceIdentifier {} to FlowConfig fail", identifier);
            LOG.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    public static boolean isFlowPath(final InstanceIdentifier<? extends Object> path) {
        PathArgument pathArg = FlowConfigMapping.getSecondPathArgumentFromPath(path);
        if (pathArg == null) {
            return false;
        }
        if (pathArg instanceof IdentifiableItem<?,?>) {
            final Identifiable<?> key = ((IdentifiableItem<?, ? extends Identifiable<?>>) pathArg).getKey();
            if ((key instanceof FlowKey)) {
                return true;
            }
        }
        return false;
    }

    private static PathArgument getSecondPathArgumentFromPath(final InstanceIdentifier<? extends Object> path) {
        if (path != null && path.getPathArguments() != null) {
            Iterator<PathArgument> iterator = path.getPathArguments().iterator();
            int deep = 0;
            while (iterator.hasNext()) {
                PathArgument pathArg = iterator.next();
                if (deep == FlowConfigMapping.FLOW_KEY_IDENTIFIER_DEEP) {
                    return pathArg;
                }
                deep++;
            }
        }
        return null;
    }
}
