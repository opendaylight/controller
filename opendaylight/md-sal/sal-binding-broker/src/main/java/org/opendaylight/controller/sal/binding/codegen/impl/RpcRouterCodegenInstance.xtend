package org.opendaylight.controller.sal.binding.codegen.impl

import org.opendaylight.yangtools.yang.binding.RpcService
import org.opendaylight.controller.sal.binding.spi.RpcRouter
import org.opendaylight.yangtools.yang.binding.BaseIdentity
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import static extension org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper.*
import java.util.Set
import java.util.HashMap
import org.opendaylight.controller.sal.binding.spi.RpcRoutingTable
import org.opendaylight.yangtools.yang.binding.DataObject
import static org.opendaylight.controller.sal.binding.codegen.impl.XtendHelper.*

class RpcRouterCodegenInstance<T extends RpcService> implements RpcRouter<T> {

    @Property
    val T invocationProxy

    @Property
    val Class<T> rpcServiceType

    @Property
    val Set<Class<? extends BaseIdentity>> contexts

    val routingTables = new HashMap<Class<? extends BaseIdentity>, RpcRoutingTableImpl<? extends BaseIdentity, ?>>;

    @Property
    var T defaultService

    new(Class<T> type, T routerImpl, Set<Class<? extends BaseIdentity>> contexts) {
        _rpcServiceType = type
        _invocationProxy = routerImpl
        _contexts = contexts

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
}
