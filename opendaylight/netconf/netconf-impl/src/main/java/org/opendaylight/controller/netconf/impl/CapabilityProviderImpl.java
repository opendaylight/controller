/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CapabilityProviderImpl implements CapabilityProvider {
    private final NetconfOperationServiceSnapshot netconfOperationServiceSnapshot;
    private final Set<String> capabilityURIs;

    private static final Logger logger = LoggerFactory.getLogger(DefaultCommitNotificationProducer.class);

    public CapabilityProviderImpl(NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
        this.netconfOperationServiceSnapshot = netconfOperationServiceSnapshot;
        Map<String, Capability> urisToCapabilitiesInternalMap = getCapabilitiesInternal(netconfOperationServiceSnapshot);
        List<String> capabilityURIs = new ArrayList<>(urisToCapabilitiesInternalMap.keySet());
        Collections.sort(capabilityURIs);
        this.capabilityURIs = Collections.unmodifiableSet(new TreeSet<>(capabilityURIs));
    }

    private static Map<String, Capability> getCapabilitiesInternal(
            NetconfOperationServiceSnapshot netconfOperationServiceSnapshot) {
        Map<String, Capability> capabilityMap = Maps.newHashMap();

        for (NetconfOperationService netconfOperationService : netconfOperationServiceSnapshot.getServices()) {
            final Set<Capability> caps = netconfOperationService.getCapabilities();

            for (Capability cap : caps) {

                if(capabilityMap.containsKey(cap.getCapabilityUri())) {
                    logger.debug("Duplicate capability {} from service {}", cap.getCapabilityUri(), netconfOperationService);
                }

                capabilityMap.put(cap.getCapabilityUri(), cap);
            }
        }

        return capabilityMap;
    }

    @Override
    public synchronized String getSchemaForCapability(String moduleName, Optional<String> revision) {

        Map<String, Map<String, String>> mappedModulesToRevisionToSchema = Maps.newHashMap();

        for (NetconfOperationService netconfOperationService : netconfOperationServiceSnapshot.getServices()) {
            final Set<Capability> caps = netconfOperationService.getCapabilities();

            for (Capability cap : caps) {
                if (cap.getModuleName().isPresent() == false)
                    continue;
                if (cap.getRevision().isPresent() == false)
                    continue;
                if (cap.getCapabilitySchema().isPresent() == false)
                    continue;

                final String currentModuleName = cap.getModuleName().get();
                Map<String, String> revisionMap = mappedModulesToRevisionToSchema.get(currentModuleName);
                if (revisionMap == null) {
                    revisionMap = Maps.newHashMap();
                    mappedModulesToRevisionToSchema.put(currentModuleName, revisionMap);
                }

                String currentRevision = cap.getRevision().get();
                revisionMap.put(currentRevision, cap.getCapabilitySchema().get());
            }
        }

        Map<String, String> revisionMapRequest = mappedModulesToRevisionToSchema.get(moduleName);
        Preconditions.checkState(revisionMapRequest != null, "Capability for module %s not present, " + ""
                + "available modules : %s", moduleName, capabilityURIs);

        if (revision.isPresent()) {
            String schema = revisionMapRequest.get(revision.get());

            Preconditions.checkState(schema != null,
                    "Capability for module %s:%s not present, available revisions for module: %s", moduleName,
                    revision.get(), revisionMapRequest.keySet());

            return schema;
        } else {
            Preconditions.checkState(revisionMapRequest.size() == 1,
                    "Expected 1 capability for module %s, available revisions : %s", moduleName,
                    revisionMapRequest.keySet());
            return revisionMapRequest.values().iterator().next();
        }
    }

    @Override
    public synchronized Set<String> getCapabilities() {
        return capabilityURIs;
    }

}
