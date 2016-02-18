package org.opendaylight.controller.netconf.test.tool;

import io.netty.util.Timer;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.impl.CommitNotifier;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;

public class TesttoolNegotiationFactory extends NetconfServerSessionNegotiatorFactory {

    private final Map<SocketAddress, NetconfOperationService> cachedOperationServices = new HashMap<>();

    public TesttoolNegotiationFactory(final Timer timer, final NetconfOperationServiceFactory netconfOperationProvider,
                                      final SessionIdProvider idProvider, final long connectionTimeoutMillis,
                                      final CommitNotifier commitNotifier, final NetconfMonitoringService monitoringService) {
        super(timer, netconfOperationProvider, idProvider, connectionTimeoutMillis, commitNotifier, monitoringService);
    }

    public TesttoolNegotiationFactory(final Timer timer, final NetconfOperationServiceFactory netconfOperationProvider,
                                      final SessionIdProvider idProvider, final long connectionTimeoutMillis,
                                      final CommitNotifier commitNotifier, final NetconfMonitoringService monitoringService,
                                      final Set<String> baseCapabilities) {
        super(timer, netconfOperationProvider, idProvider, connectionTimeoutMillis, commitNotifier, monitoringService, baseCapabilities);
    }

    @Override
    protected NetconfOperationService getOperationServiceForAddress(final String netconfSessionIdForReporting, final SocketAddress socketAddress) {
        if (cachedOperationServices.containsKey(socketAddress)) {
            return cachedOperationServices.get(socketAddress);
        } else {
            final NetconfOperationService service = getOperationServiceFactory().createService(netconfSessionIdForReporting);
            cachedOperationServices.put(socketAddress, service);
            return service;
        }
    }
}
