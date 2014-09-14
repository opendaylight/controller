package test.mock.util;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class ProviderContextMock implements BindingAwareBroker.ProviderContext {

    RpcProviderRegistry rpcProviderMock;
    NotificationProviderService notificationProviderService;
    DataBroker dataBroker;

    public ProviderContextMock(RpcProviderRegistry rpcProviderMock, DataBroker dataBroker,
                               NotificationProviderService notificationProviderServiceMock) {
        this.rpcProviderMock = rpcProviderMock;
        this.dataBroker = dataBroker;
        this.notificationProviderService = notificationProviderServiceMock;
    }

    @Override
    public void registerFunctionality(BindingAwareProvider.ProviderFunctionality functionality) {

    }

    @Override
    public void unregisterFunctionality(BindingAwareProvider.ProviderFunctionality functionality) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BindingAwareService> T getSALService(Class<T> service) {
        if (service.equals(DataBroker.class)) {
            return (T) dataBroker;
        }
        else if (service.equals(NotificationProviderService.class)) {
            return (T) notificationProviderService;
        }
        return null;
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

    @Override
    public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
        return rpcProviderMock.getRpcService(serviceInterface);
    }
}
