/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.util.internal.ConcurrentSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.CapabilitiesBuilder;
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

public class NetconfMonitoringServiceImpl implements NetconfMonitoringService, AutoCloseable {

    private static final Schema.Location NETCONF_LOCATION = new Schema.Location(Schema.Location.Enumeration.NETCONF);
    private static final List<Schema.Location> NETCONF_LOCATIONS = ImmutableList.of(NETCONF_LOCATION);
    private static final Logger LOG = LoggerFactory.getLogger(NetconfMonitoringServiceImpl.class);
    private static final Function<NetconfManagementSession, Session> SESSION_FUNCTION = new Function<NetconfManagementSession, Session>() {
        @Override
        public Session apply(@Nonnull final NetconfManagementSession input) {
            return input.toManagementSession();
        }
    };
    private static final Function<Capability, Uri> CAPABILITY_TO_URI = new Function<Capability, Uri>() {
        @Override
        public Uri apply(final Capability input) {
            return new Uri(input.getCapabilityUri());
        }
    };

    private final Set<NetconfManagementSession> sessions = new ConcurrentSet<>();
    private final NetconfOperationServiceFactory netconfOperationProvider;
    private final Map<Uri, Capability> capabilities = new ConcurrentHashMap<>();

    private final Set<MonitoringListener> listeners = Sets.newHashSet();

    public NetconfMonitoringServiceImpl(final NetconfOperationServiceFactory netconfOperationProvider) {
        this.netconfOperationProvider = netconfOperationProvider;
        netconfOperationProvider.registerCapabilityListener(this);
    }

    @Override
    public synchronized void onSessionUp(final NetconfManagementSession session) {
        LOG.debug("Session {} up", session);
        Preconditions.checkState(!sessions.contains(session), "Session %s was already added", session);
        sessions.add(session);
        notifyListeners();
    }

    @Override
    public synchronized void onSessionDown(final NetconfManagementSession session) {
        LOG.debug("Session {} down", session);
        Preconditions.checkState(sessions.contains(session), "Session %s not present", session);
        sessions.remove(session);
        notifyListeners();
    }

    @Override
    public synchronized Sessions getSessions() {
        return new SessionsBuilder().setSession(ImmutableList.copyOf(Collections2.transform(sessions, SESSION_FUNCTION))).build();
    }

    @Override
    public synchronized Schemas getSchemas() {
        try {
            return transformSchemas(netconfOperationProvider.getCapabilities());
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("Exception while closing", e);
        }
    }

    @Override
    public synchronized String getSchemaForCapability(final String moduleName, final Optional<String> revision) {

        // FIXME not effective at all

        Map<String, Map<String, String>> mappedModulesToRevisionToSchema = Maps.newHashMap();

        final Collection<Capability> caps = capabilities.values();

        for (Capability cap : caps) {
            if (!cap.getModuleName().isPresent()
                    || !cap.getRevision().isPresent()
                    || !cap.getCapabilitySchema().isPresent()){
                continue;
            }

            final String currentModuleName = cap.getModuleName().get();
            Map<String, String> revisionMap = mappedModulesToRevisionToSchema.get(currentModuleName);
            if (revisionMap == null) {
                revisionMap = Maps.newHashMap();
                mappedModulesToRevisionToSchema.put(currentModuleName, revisionMap);
            }

            String currentRevision = cap.getRevision().get();
            revisionMap.put(currentRevision, cap.getCapabilitySchema().get());
        }

        Map<String, String> revisionMapRequest = mappedModulesToRevisionToSchema.get(moduleName);
        Preconditions.checkState(revisionMapRequest != null, "Capability for module %s not present, " + ""
                + "available modules : %s", moduleName, Collections2.transform(caps, CAPABILITY_TO_URI));

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
    public synchronized Capabilities getCapabilities() {
        return new CapabilitiesBuilder().setCapability(Lists.newArrayList(capabilities.keySet())).build();
    }

    @Override
    public synchronized AutoCloseable registerListener(final MonitoringListener listener) {
        listeners.add(listener);
        listener.onStateChanged(getCurrentNetconfState());
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                listeners.remove(listener);
            }
        };
    }

    private NetconfState getCurrentNetconfState() {
        return new NetconfStateBuilder()
                .setCapabilities(getCapabilities())
                .setSchemas(getSchemas())
                .setSessions(getSessions())
                .build();
    }

    private static Schemas transformSchemas(final Set<Capability> caps) {
        final List<Schema> schemas = new ArrayList<>(caps.size());
        for (final Capability cap : caps) {
            if (cap.getCapabilitySchema().isPresent()) {
                final SchemaBuilder builder = new SchemaBuilder();
                Preconditions.checkState(cap.getModuleNamespace().isPresent());
                builder.setNamespace(new Uri(cap.getModuleNamespace().get()));

                Preconditions.checkState(cap.getRevision().isPresent());
                final String version = cap.getRevision().get();
                builder.setVersion(version);

                Preconditions.checkState(cap.getModuleName().isPresent());
                final String identifier = cap.getModuleName().get();
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

        for (final String location : locations) {
            b.add(new Schema.Location(new Uri(location)));
        }

        return b.build();
    }

    @Override
    public synchronized void onCapabilitiesAdded(final Set<Capability> addedCaps) {
        // FIXME howto check for duplicates
        this.capabilities.putAll(Maps.uniqueIndex(addedCaps, CAPABILITY_TO_URI));
        notifyListeners();
    }

    private void notifyListeners() {
        for (final MonitoringListener listener : listeners) {
            listener.onStateChanged(getCurrentNetconfState());
        }
    }

    @Override
    public synchronized void onCapabilitiesRemoved(final Set<Capability> addedCaps) {
        for (final Capability addedCap : addedCaps) {
            capabilities.remove(addedCap.getCapabilityUri());
        }
        notifyListeners();
    }

    @Override
    public synchronized void close() throws Exception {
        listeners.clear();
        sessions.clear();
        capabilities.clear();
    }
}
