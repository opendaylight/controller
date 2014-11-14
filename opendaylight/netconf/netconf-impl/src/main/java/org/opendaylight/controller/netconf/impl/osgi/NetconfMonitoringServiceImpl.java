/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.util.internal.ConcurrentSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Sessions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SessionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfMonitoringServiceImpl implements NetconfMonitoringService, SessionMonitoringService {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMonitoringServiceImpl.class);

    private final Set<NetconfManagementSession> sessions = new ConcurrentSet<>();
    private final NetconfOperationProvider netconfOperationProvider;

    public NetconfMonitoringServiceImpl(NetconfOperationProvider netconfOperationProvider) {
        this.netconfOperationProvider = netconfOperationProvider;
    }

    @Override
    public void onSessionUp(NetconfManagementSession session) {
        LOG.debug("Session {} up", session);
        Preconditions.checkState(!sessions.contains(session), "Session %s was already added", session);
        sessions.add(session);
    }

    @Override
    public void onSessionDown(NetconfManagementSession session) {
        LOG.debug("Session {} down", session);
        Preconditions.checkState(sessions.contains(session), "Session %s not present", session);
        sessions.remove(session);
    }

    @Override
    public Sessions getSessions() {
        return new SessionsBuilder().setSession(transformSessions(sessions)).build();
    }

    @Override
    public Schemas getSchemas() {
        // capabilities should be split from operations (it will allow to move getSchema operation to monitoring module)
        try (NetconfOperationServiceSnapshot snapshot = netconfOperationProvider.openSnapshot("netconf-monitoring")) {
            return transformSchemas(snapshot.getServices());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Exception while closing", e);
        }
    }

    private Schemas transformSchemas(Set<NetconfOperationService> services) {
        Set<Capability> caps = Sets.newHashSet();

        List<Schema> schemas = Lists.newArrayList();


        for (NetconfOperationService netconfOperationService : services) {
            // TODO check for duplicates ? move capability merging to snapshot
            // Split capabilities from operations first and delete this duplicate code
            caps.addAll(netconfOperationService.getCapabilities());
        }

        for (Capability cap : caps) {
            SchemaBuilder builder = new SchemaBuilder();

            if (cap.getCapabilitySchema().isPresent() == false) {
                continue;
            }

            Preconditions.checkState(cap.getModuleNamespace().isPresent());
            builder.setNamespace(new Uri(cap.getModuleNamespace().get()));

            Preconditions.checkState(cap.getRevision().isPresent());
            String version = cap.getRevision().get();
            builder.setVersion(version);

            Preconditions.checkState(cap.getModuleName().isPresent());
            String identifier = cap.getModuleName().get();
            builder.setIdentifier(identifier);

            builder.setFormat(Yang.class);

            builder.setLocation(transformLocations(cap.getLocation().or(Collections.<String>emptyList())));

            builder.setKey(new SchemaKey(Yang.class, identifier, version));

            schemas.add(builder.build());
        }

        return new SchemasBuilder().setSchema(schemas).build();
    }

    private List<Schema.Location> transformLocations(List<String> locations) {
        List<Schema.Location> monitoringLocations = Lists.newArrayList();
        monitoringLocations.add(new Schema.Location(Schema.Location.Enumeration.NETCONF));

        for (String location : locations) {
            monitoringLocations.add(new Schema.Location(new Uri(location)));
        }

        return monitoringLocations;
    }

    private List<Session> transformSessions(Set<NetconfManagementSession> sessions) {
        return Lists.newArrayList(Collections2.transform(sessions, new Function<NetconfManagementSession, Session>() {
            @Override
            public Session apply(@Nonnull NetconfManagementSession input) {
                return input.toManagementSession();
            }
        }));
    }
}
