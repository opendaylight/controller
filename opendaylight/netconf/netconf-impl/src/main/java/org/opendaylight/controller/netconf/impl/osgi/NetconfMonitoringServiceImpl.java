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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import io.netty.util.internal.ConcurrentSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
    private static final Schema.Location NETCONF_LOCATION = new Schema.Location(Schema.Location.Enumeration.NETCONF);
    private static final List<Schema.Location> NETCONF_LOCATIONS = ImmutableList.of(NETCONF_LOCATION);
    private static final Logger LOG = LoggerFactory.getLogger(NetconfMonitoringServiceImpl.class);
    private static final Function<NetconfManagementSession, Session> SESSION_FUNCTION = new Function<NetconfManagementSession, Session>() {
        @Override
        public Session apply(@Nonnull final NetconfManagementSession input) {
            return input.toManagementSession();
        }
    };

    private final Set<NetconfManagementSession> sessions = new ConcurrentSet<>();
    private final NetconfOperationProvider netconfOperationProvider;

    public NetconfMonitoringServiceImpl(final NetconfOperationProvider netconfOperationProvider) {
        this.netconfOperationProvider = netconfOperationProvider;
    }

    @Override
    public void onSessionUp(final NetconfManagementSession session) {
        LOG.debug("Session {} up", session);
        Preconditions.checkState(!sessions.contains(session), "Session %s was already added", session);
        sessions.add(session);
    }

    @Override
    public void onSessionDown(final NetconfManagementSession session) {
        LOG.debug("Session {} down", session);
        Preconditions.checkState(sessions.contains(session), "Session %s not present", session);
        sessions.remove(session);
    }

    @Override
    public Sessions getSessions() {
        return new SessionsBuilder().setSession(ImmutableList.copyOf(Collections2.transform(sessions, SESSION_FUNCTION))).build();
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

    private static Schemas transformSchemas(final Set<NetconfOperationService> services) {
        // FIXME: Capability implementations do not have hashcode/equals!
        final Set<Capability> caps = new HashSet<>();
        for (NetconfOperationService netconfOperationService : services) {
            // TODO check for duplicates ? move capability merging to snapshot
            // Split capabilities from operations first and delete this duplicate code
            caps.addAll(netconfOperationService.getCapabilities());
        }

        final List<Schema> schemas = new ArrayList<>(caps.size());
        for (Capability cap : caps) {
            if (cap.getCapabilitySchema().isPresent()) {
                SchemaBuilder builder = new SchemaBuilder();
                Preconditions.checkState(cap.getModuleNamespace().isPresent());
                builder.setNamespace(new Uri(cap.getModuleNamespace().get()));

                Preconditions.checkState(cap.getRevision().isPresent());
                String version = cap.getRevision().get();
                builder.setVersion(version);

                Preconditions.checkState(cap.getModuleName().isPresent());
                String identifier = cap.getModuleName().get();
                builder.setIdentifier(identifier);

                builder.setFormat(Yang.class);

                builder.setLocation(transformLocations(cap.getLocation()));

                builder.setKey(new SchemaKey(Yang.class, identifier, version));

                schemas.add(builder.build());
            }
        }

        return new SchemasBuilder().setSchema(schemas).build();
    }

    private static List<Schema.Location> transformLocations(final Collection<String> locations) {
        if (locations.isEmpty()) {
            return NETCONF_LOCATIONS;
        }

        final Builder<Schema.Location> b = ImmutableList.builder();
        b.add(NETCONF_LOCATION);

        for (String location : locations) {
            b.add(new Schema.Location(new Uri(location)));
        }

        return b.build();
    }
}
