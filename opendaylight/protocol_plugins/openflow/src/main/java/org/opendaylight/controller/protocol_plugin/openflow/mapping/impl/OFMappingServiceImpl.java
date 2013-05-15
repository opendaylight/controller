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

import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFMappingService;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.SALActionMapper;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFActionMapper;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFMappingContext;
import org.opendaylight.controller.sal.action.Action;
import org.openflow.protocol.action.OFAction;
import static org.opendaylight.controller.protocol_plugin.openflow.mapping.impl.SALActionMappings.*;

public class OFMappingServiceImpl implements OFMappingService {

    private final MappingContextImpl mappingContext = new MappingContextImpl();

    public OFMappingContext getMappingContext() {
        return this.mappingContext;
    }

    public OFMappingServiceImpl() {
        initBaseMappings();
    }

    private void initBaseMappings() {

        // SAL to Openflow Mapping
        addSALActionMapper(CONTROLLER);
        addSALActionMapper(FLOOD);
        addSALActionMapper(FLOOD_ALL);
        addSALActionMapper(HW_PATH);
        addSALActionMapper(LOOPBACK);
        addSALActionMapper(OUTPUT);
        addSALActionMapper(POP_VLAN);
        addSALActionMapper(SET_DL_DST);
        addSALActionMapper(SET_DL_SRC);
        addSALActionMapper(SET_NW_DST);
        addSALActionMapper(SET_NW_SRC);
        addSALActionMapper(SET_NW_TOS);
        addSALActionMapper(SET_TP_DST);
        addSALActionMapper(SET_TP_SRC);
        addSALActionMapper(SET_VLAN_ID);
        addSALActionMapper(SET_VLAN_PCP);
        addSALActionMapper(SW_PATH);

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

    public void addSALActionMapper(SALActionMapper<? extends Action> mapper) {

        Class<? extends Action> salClass = mapper.getSalClass();
        // FIXME: Add check if mapping is already registered
        mappingContext.salMapping.put(salClass, mapper);
    }

    @Override
    public void addOFActionMapper(OFActionMapper<? extends OFAction> mapper) {
        Class<? extends OFAction> ofClass = mapper.getOfClass();
        mappingContext.ofMapping.put(ofClass, mapper);
    }

    private static class MappingContextImpl implements OFMappingContext {

        private Map<Class<? extends Action>, SALActionMapper<?>> salMapping = new ConcurrentHashMap<Class<? extends Action>, SALActionMapper<?>>();
        private Map<Class<? extends OFAction>, OFActionMapper<?>> ofMapping = new ConcurrentHashMap<Class<? extends OFAction>, OFActionMapper<?>>();

        @Override
        public <T extends Action> SALActionMapper<T> getMapperForSalAction(
                Class<T> salType) {
            // FIXME add class cast validation
            return (SALActionMapper<T>) salMapping.get(salType);
        }

        @Override
        public <T extends OFAction> OFActionMapper<T> getMapperForOFAction(
                Class<T> ofType) {
            // FIXME add class cast validation
            return (OFActionMapper<T>) ofMapping.get(ofType);
        }
    }
}
