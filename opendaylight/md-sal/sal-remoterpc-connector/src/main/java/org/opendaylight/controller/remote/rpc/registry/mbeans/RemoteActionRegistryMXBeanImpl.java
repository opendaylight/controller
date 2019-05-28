package org.opendaylight.controller.remote.rpc.registry.mbeans;

import akka.actor.Address;
import akka.util.Timeout;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.remote.rpc.registry.ActionRoutingTable;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.*;

public class RemoteActionRegistryMXBeanImpl extends AbstractMXBean implements RemoteActionRegistryMXBean {

    @SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String LOCAL_CONSTANT = "local";

    private static final String ROUTE_CONSTANT = "route:";

    private static final String NAME_CONSTANT = " | name:";

    private final BucketStoreAccess actionRegistryAccess;
    private final Timeout timeout;

    public RemoteActionRegistryMXBeanImpl(final BucketStoreAccess actionRegistryAccess, final Timeout timeout) {
        super("RemoteActionRegistry", "RemoteActionBroker", null);
        this.actionRegistryAccess = actionRegistryAccess;
        this.timeout = timeout;
        registerMBean();
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    private ActionRoutingTable getLocalData() {
        try {
            return (ActionRoutingTable) Await.result((Future) actionRegistryAccess.getLocalData(), timeout.duration());
        } catch (Exception e) {
            throw new RuntimeException("getLocalData failed", e);
        }
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    private Map<Address, Bucket<ActionRoutingTable>> getRemoteBuckets() {
        try {
            return (Map<Address, Bucket<ActionRoutingTable>>) Await.result((Future)actionRegistryAccess.getRemoteBuckets(),
                    timeout.duration());
        } catch (Exception e) {
            throw new RuntimeException("getRemoteBuckets failed", e);
        }
    }

    @Override
    public Set<String> getLocalRegisteredAction() {
        ActionRoutingTable table = getLocalData();
        Set<String> routedAction = new HashSet<>(table.getActions().size());
        for (DOMActionInstance route : table.getActions()) {
            final YangInstanceIdentifier actionPath = YangInstanceIdentifier.create(
                    new YangInstanceIdentifier.NodeIdentifier(route.getType().getLastComponent()));
            if (!actionPath.isEmpty()) {
                routedAction.add(ROUTE_CONSTANT + actionPath + NAME_CONSTANT + route.getType());
            }
        }

        log.debug("Locally registered routed RPCs {}", routedAction);
        return routedAction;
    }

    @Override
    public Map<String, String> findActionByName(final String name) {
        ActionRoutingTable localTable = getLocalData();
        // Get all RPCs from local bucket
        Map<String, String> rpcMap = new HashMap<>(getActionMemberMapByName(localTable, name, LOCAL_CONSTANT));

        // Get all RPCs from remote bucket
        Map<Address, Bucket<ActionRoutingTable>> buckets = getRemoteBuckets();
        for (Map.Entry<Address, Bucket<ActionRoutingTable>> entry : buckets.entrySet()) {
            ActionRoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getActionMemberMapByName(table, name, entry.getKey().toString()));
        }

        log.debug("list of RPCs {} searched by name {}", rpcMap, name);
        return rpcMap;
    }

    @Override
    public Map<String, String> findActionByRoute(final String routeId) {
        ActionRoutingTable localTable = getLocalData();
        Map<String, String> rpcMap = new HashMap<>(getActionMemberMapByAction(localTable, routeId, LOCAL_CONSTANT));

        Map<Address, Bucket<ActionRoutingTable>> buckets = getRemoteBuckets();
        for (Map.Entry<Address, Bucket<ActionRoutingTable>> entry : buckets.entrySet()) {
            ActionRoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getActionMemberMapByAction(table, routeId, entry.getKey().toString()));
        }

        log.debug("list of RPCs {} searched by route {}", rpcMap, routeId);
        return rpcMap;
    }

    /**
     * Search if the routing table route String contains routeName.
     */
    private static Map<String,String> getActionMemberMapByAction(final ActionRoutingTable table, final String routeName,
                                                             final String address) {
        Collection<DOMActionInstance> routes = table.getActions();
        Map<String, String> rpcMap = new HashMap<>(routes.size());
        for (DOMActionInstance route : routes) {
            final YangInstanceIdentifier actionPath = YangInstanceIdentifier.create(
                    new YangInstanceIdentifier.NodeIdentifier(route.getType().getLastComponent()));
            if (!actionPath.isEmpty()) {
                String routeString = actionPath.toString();
                if (routeString.contains(routeName)) {
                    rpcMap.put(ROUTE_CONSTANT + routeString + NAME_CONSTANT + route.getType(), address);
                }
            }
        }
        return rpcMap;
    }

    /**
     * Search if the routing table route type contains name.
     */
    private static Map<String, String>  getActionMemberMapByName(final ActionRoutingTable table, final String name,
                                                              final String address) {
        Collection<DOMActionInstance> routes = table.getActions();
        Map<String, String> rpcMap = new HashMap<>(routes.size());
        for (DOMActionInstance route : routes) {
            final YangInstanceIdentifier actionPath = YangInstanceIdentifier.create(
                    new YangInstanceIdentifier.NodeIdentifier(route.getType().getLastComponent()));
            if (!actionPath.isEmpty()) {
                String type = route.getType().toString();
                if (type.contains(name)) {
                    rpcMap.put(ROUTE_CONSTANT + actionPath + NAME_CONSTANT + type, address);
                }
            }
        }
        return rpcMap;
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    public String getBucketVersions() {
        try {
            return Await.result((Future)actionRegistryAccess.getBucketVersions(), timeout.duration()).toString();
        } catch (Exception e) {
            throw new RuntimeException("getVersions failed", e);
        }
    }
}