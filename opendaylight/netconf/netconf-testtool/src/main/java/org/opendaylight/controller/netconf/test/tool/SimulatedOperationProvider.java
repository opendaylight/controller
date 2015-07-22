package org.opendaylight.controller.netconf.test.tool;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Set;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.test.tool.rpc.DataList;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedCommit;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedCreateSubscription;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedEditConfig;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedGet;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedGetConfig;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedLock;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedUnLock;

class SimulatedOperationProvider implements NetconfOperationServiceFactory {
    private final Set<Capability> caps;
    private final SimulatedOperationService simulatedOperationService;

    public SimulatedOperationProvider(final SessionIdProvider idProvider,
                                      final Set<Capability> caps,
                                      final Optional<File> notificationsFile) {
        this.caps = caps;
        simulatedOperationService = new SimulatedOperationService(idProvider.getCurrentSessionId(), notificationsFile);
    }

    @Override
    public Set<Capability> getCapabilities() {
        return caps;
    }

    @Override
    public AutoCloseable registerCapabilityListener(
            final CapabilityListener listener) {
        listener.onCapabilitiesAdded(caps);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            }
        };
    }

    @Override
    public NetconfOperationService createService(
            final String netconfSessionIdForReporting) {
        return simulatedOperationService;
    }

    static class SimulatedOperationService implements NetconfOperationService {
        private final long currentSessionId;
        private final Optional<File> notificationsFile;

        public SimulatedOperationService(final long currentSessionId, final Optional<File> notificationsFile) {
            this.currentSessionId = currentSessionId;
            this.notificationsFile = notificationsFile;
        }

        @Override
        public Set<NetconfOperation> getNetconfOperations() {
            final DataList storage = new DataList();
            final SimulatedGet sGet = new SimulatedGet(String.valueOf(currentSessionId), storage);
            final SimulatedEditConfig sEditConfig = new SimulatedEditConfig(String.valueOf(currentSessionId), storage);
            final SimulatedGetConfig sGetConfig = new SimulatedGetConfig(String.valueOf(currentSessionId), storage);
            final SimulatedCommit sCommit = new SimulatedCommit(String.valueOf(currentSessionId));
            final SimulatedLock sLock = new SimulatedLock(String.valueOf(currentSessionId));
            final SimulatedUnLock sUnlock = new SimulatedUnLock(String.valueOf(currentSessionId));
            final SimulatedCreateSubscription sCreateSubs = new SimulatedCreateSubscription(
                    String.valueOf(currentSessionId), notificationsFile);
            return Sets.<NetconfOperation>newHashSet(sGet, sGetConfig, sEditConfig, sCommit, sLock, sUnlock, sCreateSubs);
        }

        @Override
        public void close() {
        }

    }
}
