package org.opendaylight.controller.sal.binding.codegen.impl

import org.opendaylight.yangtools.yang.binding.RpcService
import org.opendaylight.controller.sal.binding.spi.RpcRouter
import org.opendaylight.yangtools.yang.binding.BaseIdentity
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import static extension org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper.*
import java.util.Set
import java.util.HashMap
import org.opendaylight.controller.sal.binding.spi.RpcRoutingTable
import org.opendaylight.yangtools.yang.binding.DataContainer
import org.opendaylight.yangtools.yang.binding.RpcImplementation

class RpcRouterCodegenInstance<T extends RpcService> implements RpcRouter<T> {

    @Property
    val T invocationProxy

    @Property
    val RpcImplementation invokerDelegate;

    @Property
    val Class<T> serviceType

    @Property
    val Set<Class<? extends BaseIdentity>> contexts

    @Property
    val Set<Class<? extends DataContainer>> supportedInputs;

    val routingTables = new HashMap<Class<? extends BaseIdentity>, RpcRoutingTableImpl<? extends BaseIdentity, ? extends RpcService>>;

    @Property
    var T defaultService

    new(Class<T> type, T routerImpl, Set<Class<? extends BaseIdentity>> contexts,
        Set<Class<? extends DataContainer>> inputs) {
        _serviceType = type
        _invocationProxy = routerImpl
        _invokerDelegate = routerImpl as RpcImplementation
        _contexts = contexts
        _supportedInputs = inputs;

        for (ctx : contexts) {
            val table = XtendHelper.createRoutingTable(ctx)
            invocationProxy.setRoutingTable(ctx, table.routes);
            routingTables.put(ctx, table);
        }
    }

    override <C extends BaseIdentity> getRoutingTable(Class<C> table) {
        routingTables.get(table) as RpcRoutingTable<C,T>
    }

    override getService(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {
        val table = getRoutingTable(context);
        return table.getRoute(path);
    }

    override <T extends DataContainer> invoke(Class<T> type, T input) {
        return invokerDelegate.invoke(type, input);
    }

}
