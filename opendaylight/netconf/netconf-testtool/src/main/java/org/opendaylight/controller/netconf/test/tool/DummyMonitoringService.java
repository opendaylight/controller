/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Sessions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SessionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema.Location;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema.Location.Enumeration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;

public class DummyMonitoringService implements NetconfMonitoringService {

    private static final Sessions EMPTY_SESSIONS = new SessionsBuilder().setSession(Collections.<Session>emptyList()).build();
    private static final Function<Capability, Uri> CAPABILITY_URI_FUNCTION = new Function<Capability, Uri>() {
        @Nullable
        @Override
        public Uri apply(Capability capability) {
            return new Uri(capability.getCapabilityUri());
        }
    };

    private static final Function<Capability, Schema> CAPABILITY_SCHEMA_FUNCTION = new Function<Capability, Schema>() {
        @Nullable
        @Override
        public Schema apply(@Nullable Capability capability) {
            return new SchemaBuilder()
                    .setIdentifier(capability.getModuleName().get())
                    .setNamespace(new Uri(capability.getModuleNamespace().get()))
                    .setFormat(Yang.class)
                    .setVersion(capability.getRevision().get())
                    .setLocation(Collections.singletonList(new Location(Enumeration.NETCONF)))
                    .setKey(new SchemaKey(Yang.class, capability.getModuleName().get(), capability.getRevision().get())).build();
        }
    };

    private final Capabilities capabilities;
    private final ArrayListMultimap<String, Capability> capabilityMultiMap;
    private final Schemas schemas;

    public DummyMonitoringService(Set<Capability> capabilities) {

        this.capabilities = new CapabilitiesBuilder().setCapability(
                Lists.newArrayList(Collections2.transform(capabilities, CAPABILITY_URI_FUNCTION))).build();

        this.capabilityMultiMap = ArrayListMultimap.create();
        for (Capability cap : capabilities) {
            capabilityMultiMap.put(cap.getModuleName().get(), cap);
        }

        this.schemas = new SchemasBuilder().setSchema(Lists.newArrayList(Collections2.transform(capabilities, CAPABILITY_SCHEMA_FUNCTION))).build();
    }

    @Override
    public Sessions getSessions() {
        return EMPTY_SESSIONS;
    }

    @Override
    public Schemas getSchemas() {
        return schemas;
    }

    @Override
    public String getSchemaForCapability(String moduleName, Optional<String> revision) {

        for (Capability capability : capabilityMultiMap.get(moduleName)) {
            if (capability.getRevision().get().equals(revision.get())) {
                return capability.getCapabilitySchema().get();
            }
        }
        throw new IllegalArgumentException("Module with name: " + moduleName + " and revision: " + revision + " does not exist");
    }

    @Override
    public Capabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public AutoCloseable registerListener(MonitoringListener listener) {
        return null;
    }

    @Override
    public void onCapabilitiesAdded(Set<Capability> addedCaps) {

    }

    @Override
    public void onCapabilitiesRemoved(Set<Capability> removedCaps) {

    }

    @Override
    public void onSessionUp(NetconfManagementSession session) {

    }

    @Override
    public void onSessionDown(NetconfManagementSession session) {

    }
}
