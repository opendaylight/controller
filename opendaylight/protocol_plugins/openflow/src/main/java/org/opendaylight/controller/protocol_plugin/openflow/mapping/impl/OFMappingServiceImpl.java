/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.protocol_plugin.openflow.mapping.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFTransformationService;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.SALToOFActionTransformer;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFActionToSalTransformer;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFTransformationContext;
import org.opendaylight.controller.sal.action.Action;
import org.openflow.protocol.action.OFAction;
import static org.opendaylight.controller.protocol_plugin.openflow.mapping.impl.SALActionMappings.*;

public class OFMappingServiceImpl implements OFTransformationService {

    private final MappingContextImpl mappingContext = new MappingContextImpl();

    public OFTransformationContext getMappingContext() {
        return this.mappingContext;
    }

    public OFMappingServiceImpl() {
        initBaseMappings();
    }

    private void initBaseMappings() {
        // Openflow to SAL mapping
        addOFActionMapper(OFActionMappings.DataLayerDestination);
        addOFActionMapper(OFActionMappings.DataLayerSource);
        addOFActionMapper(OFActionMappings.NetworkLayerDestination);
        addOFActionMapper(OFActionMappings.NetworkLayerSource);
        addOFActionMapper(OFActionMappings.NetworkTypeOfService);
        addOFActionMapper(OFActionMappings.Output);
        addOFActionMapper(OFActionMappings.StripVirtualLan);
        addOFActionMapper(OFActionMappings.TransportLayerDestination);
        addOFActionMapper(OFActionMappings.TransportLayerSource);
        addOFActionMapper(OFActionMappings.VirtualLanIdentifier);
        addOFActionMapper(OFActionMappings.VirtualLanPriorityCodePoint);
    }

    public void addSALActionTransformer(SALToOFActionTransformer<? extends Action> transformer) {
        mappingContext.salMapping.addTransformer(transformer);
    }

    @Override
    public void addOFActionMapper(OFActionToSalTransformer<? extends OFAction> mapper) {
        Class<? extends OFAction> ofClass = mapper.getInputClass();
        mappingContext.ofMapping.put(ofClass, mapper);
    }

    private static class MappingContextImpl implements OFTransformationContext {
        private CompositeSALActionToOFTransformer salMapping = new CompositeSALActionToOFTransformer();
        private Map<Class<? extends OFAction>, OFActionToSalTransformer<?>> ofMapping = new ConcurrentHashMap<Class<? extends OFAction>, OFActionToSalTransformer<?>>();

        @Override
        public SALToOFActionTransformer<Action> getSalActionTransformer() {
            return salMapping;
        }

        @Override
        public <T extends OFAction> OFActionToSalTransformer<T> getOFActionTransformer(
                Class<T> ofType) {
            // FIXME add class cast validation
            return (OFActionToSalTransformer<T>) ofMapping.get(ofType);
        }
    }
}
