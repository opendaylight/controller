package org.opendaylight.controller.connectionmanager.scheme;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.SystemException;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractScheme {
    private static final Logger log = LoggerFactory.getLogger(AbstractScheme.class);
    protected IClusterGlobalServices clusterServices = null;
    /*
     * A more natural Map data-structure is to have a Key=Controller IP-address with value = a set of Nodes.
     * But, such a data-structure results in some complex event processing during the Cluster operations
     * to sync up the Connection states.
     *
     * A data-structure with Node as the key and set of controllers provides a good balance
     * between the needed functionality and simpler clustering implementation for Connection Manager.
     */
    protected ConcurrentMap <Node, Set<InetAddress>> nodeConnections;
    protected abstract boolean isConnectionAllowedInternal(Node node);
    private String name;

    protected AbstractScheme(IClusterGlobalServices clusterServices, ConnectionMgmtScheme type) {
        this.clusterServices = clusterServices;
        if (type != null) name = type.name();
        else name = "UNKNOWN";
        if (clusterServices != null) {
            allocateCaches();
            retrieveCaches();
        }
    }

    protected ConcurrentMap <InetAddress, Set<Node>> getControllerToNodesMap() {
        ConcurrentMap <InetAddress, Set<Node>> controllerNodesMap = new ConcurrentHashMap <InetAddress, Set<Node>>();
        for (Node node : nodeConnections.keySet()) {
            Set<InetAddress> controllers = nodeConnections.get(node);
            if (controllers == null) continue;
            for (InetAddress controller : controllers) {
                Set<Node> nodes = controllerNodesMap.get(controller);
                if (nodes == null) {
                    nodes = new HashSet<Node>();
                }
                nodes.add(node);
                controllerNodesMap.put(controller, nodes);
            }
        }
        return controllerNodesMap;
    }

    public boolean isConnectionAllowed (Node node) {
        if (clusterServices == null || nodeConnections == null) {
            return false;
        }

        return isConnectionAllowedInternal(node);
    }

    @SuppressWarnings("deprecation")
    public void handleClusterViewChanged() {
        List<InetAddress> controllers = clusterServices.getClusteredControllers();
        ConcurrentMap <InetAddress, Set<Node>> controllerNodesMap = getControllerToNodesMap();
        List<InetAddress> toRemove = new ArrayList<InetAddress>();
        for (InetAddress c : controllerNodesMap.keySet()) {
            if (!controllers.contains(c)) {
                toRemove.add(c);
            }
        }

        boolean retry = false;
        for (InetAddress c : toRemove) {
            log.debug("Removing Controller : {} from the Connections table", c);
            for (Iterator<Node> nodeIterator = nodeConnections.keySet().iterator();nodeIterator.hasNext();) {
                Node node = nodeIterator.next();
                Set <InetAddress> oldControllers = nodeConnections.get(node);
                Set <InetAddress> newControllers = new HashSet<InetAddress>(oldControllers);
                if (newControllers.remove(c)) {
                    try {
                        clusterServices.tbegin();
                        if (!nodeConnections.replace(node, oldControllers, newControllers)) {
                            log.debug("Replace Failed for {} ", node.toString());
                            retry = true;
                            clusterServices.trollback();
                            break;
                        } else {
                            clusterServices.tcommit();
                        }
                    } catch (Exception e) {
                        log.error("Exception in replacing nodeConnections ", e);
                        retry = false;
                        try {
                            clusterServices.trollback();
                        } catch (Exception e1) {}
                        break;
                    }
                }
            }
        }
        if (retry) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handleClusterViewChanged();
        }
    }

    public Set<Node> getNodes(InetAddress controller) {
        if (nodeConnections == null) return null;
        ConcurrentMap <InetAddress, Set<Node>> controllerNodesMap = getControllerToNodesMap();
        return controllerNodesMap.get(controller);
    }

    @SuppressWarnings("deprecation")
    public Set<Node> getNodes() {
        return getNodes(clusterServices.getMyAddress());
    }

    public Set<InetAddress> getControllers(Node node) {
        if (nodeConnections != null) return nodeConnections.get(node);
        return null;
    }

    public ConcurrentMap<Node, Set<InetAddress>> getNodeConnections() {
        return nodeConnections;
    }

    @SuppressWarnings("deprecation")
    public boolean isLocal(Node node) {
        if (nodeConnections == null) return false;
        InetAddress myController = clusterServices.getMyAddress();
        Set<InetAddress> controllers = nodeConnections.get(node);
        return (controllers != null && controllers.contains(myController));
    }

    @SuppressWarnings("deprecation")
    public Status removeNode (Node node) {
        return removeNodeFromController(node, clusterServices.getMyAddress());
    }

    protected Status removeNodeFromController (Node node, InetAddress controller) {
        if (node == null || controller == null) {
            return new Status(StatusCode.BADREQUEST);
        }

        if (clusterServices == null || nodeConnections == null) {
            return new Status(StatusCode.SUCCESS);
        }

        Set<InetAddress> oldControllers = nodeConnections.get(node);

        if (oldControllers != null && oldControllers.contains(controller)) {
            Set<InetAddress> newControllers = new HashSet<InetAddress>(oldControllers);
            if (newControllers.remove(controller)) {
                try {
                    clusterServices.tbegin();
                    if (newControllers.size() > 0) {
                        if (!nodeConnections.replace(node, oldControllers, newControllers)) {
                            clusterServices.trollback();
                            try {
                                Thread.sleep(100);
                            } catch ( InterruptedException e) {}
                            return removeNodeFromController(node, controller);
                        }
                    } else {
                        nodeConnections.remove(node);
                    }
                    clusterServices.tcommit();
                } catch (Exception e) {
                    log.error("Excepion in removing Controller from a Node", e);
                    try {
                        clusterServices.trollback();
                    } catch (Exception e1) {
                        log.error("Error Rolling back the node Connections Changes ", e);
                    }
                    return new Status(StatusCode.INTERNALERROR);
                }

            }
        }
        return new Status(StatusCode.SUCCESS);

    }

    /*
     * A few race-conditions were seen with the Clustered caches in putIfAbsent and replace
     * functions. Leaving a few debug logs behind to assist in debugging if strange things happen.
     */
    private Status putNodeToController (Node node, InetAddress controller) {
        if (clusterServices == null || nodeConnections == null) {
            return new Status(StatusCode.SUCCESS);
        }
        log.debug("Trying to Put {} to {}", controller.getHostAddress(), node.toString());

        Set <InetAddress> oldControllers = nodeConnections.get(node);
        Set <InetAddress> newControllers = null;
        if (oldControllers == null) {
            newControllers = new HashSet<InetAddress>();
        } else {
            if (oldControllers.size() > 0 && !isConnectionAllowed(node)) {
                /*
                 * In certain race conditions, the putIfAbsent fails to be atomic.
                 * This check is added to identify such cases and report an warning
                 * for debugging.
                 */
                log.warn("States Exists for {} : {}", node, oldControllers.toString());
            }
            newControllers = new HashSet<InetAddress>(oldControllers);
        }
        newControllers.add(controller);

        try {
            clusterServices.tbegin();
            if (nodeConnections.putIfAbsent(node, newControllers) != null) {
                log.debug("PutIfAbsent failed {} to {}", controller.getHostAddress(), node.toString());
                /*
                 * This check is needed again to take care of the case where some schemes
                 * would not allow nodes to be connected to multiple controllers.
                 * Hence, if putIfAbsent fails, that means, some other controller is competing
                 * with this controller to take hold of a Node.
                 */
                if (isConnectionAllowed(node)) {
                    if (!nodeConnections.replace(node, oldControllers, newControllers)) {
                        clusterServices.trollback();
                        try {
                            Thread.sleep(100);
                        } catch ( InterruptedException e) {}
                        log.debug("Replace failed... old={} with new={} for {} to {}", oldControllers.toString(), newControllers.toString(),
                                controller.getHostAddress(), node.toString());
                        return putNodeToController(node, controller);
                    } else {
                        log.debug("Replace successful old={} with new={} for {} to {}", oldControllers.toString(), newControllers.toString(),
                                controller.getHostAddress(), node.toString());
                    }
                } else {
                    clusterServices.trollback();
                    return new Status(StatusCode.CONFLICT);
                }
            } else {
                log.debug("Added {} to {}", controller.getHostAddress(), node.toString());
            }
            clusterServices.tcommit();
        } catch (Exception e) {
            log.error("Excepion in adding Controller to a Node", e);
            try {
                clusterServices.trollback();
            } catch (Exception e1) {
                log.error("Error Rolling back the node Connections Changes ", e);
            }
            return new Status(StatusCode.INTERNALERROR);
        }
        return new Status(StatusCode.SUCCESS);
    }

    public Status addNode (Node node, InetAddress controller) {
        if (node == null || controller == null) {
            return new Status(StatusCode.BADREQUEST);
        }
        if (isLocal(node)) return new Status(StatusCode.SUCCESS);
        if (isConnectionAllowed(node)) {
            return putNodeToController(node, controller);
        } else {
            return new Status(StatusCode.NOTALLOWED);
        }
    }

    @SuppressWarnings("deprecation")
    public Status addNode (Node node) {
        return addNode(node, clusterServices.getMyAddress());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCaches() {
        if (this.clusterServices == null) {
            log.error("un-initialized clusterServices, can't retrieve cache");
            return;
        }

        nodeConnections = (ConcurrentMap<Node, Set<InetAddress>>) clusterServices.getCache("connectionmanager."+name+".nodeconnections");

        if (nodeConnections == null) {
            log.error("\nFailed to get caches");
        }
    }

    @SuppressWarnings("deprecation")
    private void allocateCaches() {
        if (this.clusterServices == null) {
            log.error("un-initialized clusterServices, can't create cache");
            return;
        }

        try {
            clusterServices.createCache("connectionmanager."+name+".nodeconnections", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("\nCache already exists - destroy and recreate if needed");
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
