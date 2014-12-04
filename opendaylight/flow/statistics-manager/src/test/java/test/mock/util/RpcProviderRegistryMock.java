package test.mock.util;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class RpcProviderRegistryMock implements RpcProviderRegistry {

    OpendaylightFlowStatisticsServiceMock flowStatisticsServiceMock;
    OpendaylightFlowTableStatisticsServiceMock flowTableStatisticsServiceMock;
    OpendaylightGroupStatisticsServiceMock groupStatisticsServiceMock;
    OpendaylightMeterStatisticsServiceMock meterStatisticsServiceMock;
    OpendaylightPortStatisticsServiceMock portStatisticsServiceMock;
    OpendaylightQueueStatisticsServiceMock queueStatisticsServiceMock;

    public RpcProviderRegistryMock(NotificationProviderServiceHelper notificationProviderService) {
        this.flowStatisticsServiceMock = new OpendaylightFlowStatisticsServiceMock(notificationProviderService);
        this.flowTableStatisticsServiceMock = new OpendaylightFlowTableStatisticsServiceMock(notificationProviderService);
        this.groupStatisticsServiceMock = new OpendaylightGroupStatisticsServiceMock(notificationProviderService);
        this.meterStatisticsServiceMock = new OpendaylightMeterStatisticsServiceMock(notificationProviderService);
        this.portStatisticsServiceMock = new OpendaylightPortStatisticsServiceMock(notificationProviderService);
        this.queueStatisticsServiceMock = new OpendaylightQueueStatisticsServiceMock(notificationProviderService);
    }

    @Override
    public <T extends RpcService> BindingAwareBroker.RpcRegistration<T> addRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
        return null;
    }

    @Override
    public <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> serviceInterface, T implementation) throws IllegalStateException {
        return null;
    }

    @Override
    public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(L listener) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
        if (serviceInterface.equals(OpendaylightFlowStatisticsService.class)) {
            return (T)flowStatisticsServiceMock;
        } else if (serviceInterface.equals(OpendaylightFlowTableStatisticsService.class)) {
            return (T) flowTableStatisticsServiceMock;
        } else if (serviceInterface.equals(OpendaylightGroupStatisticsService.class)) {
            return (T) groupStatisticsServiceMock;
        } else if (serviceInterface.equals(OpendaylightMeterStatisticsService.class)) {
            return (T) meterStatisticsServiceMock;
        } else if (serviceInterface.equals(OpendaylightPortStatisticsService.class)) {
            return (T) portStatisticsServiceMock;
        } else if (serviceInterface.equals(OpendaylightQueueStatisticsService.class)) {
            return (T) queueStatisticsServiceMock;
        } else {
            return null;
        }
    }

    public OpendaylightFlowStatisticsServiceMock getFlowStatisticsServiceMock() {
        return flowStatisticsServiceMock;
    }

    public OpendaylightFlowTableStatisticsServiceMock getFlowTableStatisticsServiceMock() {
        return flowTableStatisticsServiceMock;
    }

    public OpendaylightGroupStatisticsServiceMock getGroupStatisticsServiceMock() {
        return groupStatisticsServiceMock;
    }

    public OpendaylightMeterStatisticsServiceMock getMeterStatisticsServiceMock() {
        return meterStatisticsServiceMock;
    }

    public OpendaylightPortStatisticsServiceMock getPortStatisticsServiceMock() {
        return portStatisticsServiceMock;
    }

    public OpendaylightQueueStatisticsServiceMock getQueueStatisticsServiceMock() {
        return queueStatisticsServiceMock;
    }
}
