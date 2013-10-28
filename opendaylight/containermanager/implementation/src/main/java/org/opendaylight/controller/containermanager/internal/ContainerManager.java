
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationAware;
import org.opendaylight.controller.configuration.IConfigurationService;
import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.sal.authorization.AppRoleLevel;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.Resource;
import org.opendaylight.controller.sal.authorization.ResourceGroup;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerAware;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.IContainerLocalListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.appauth.authorization.Authorization;
import org.opendaylight.controller.containermanager.ContainerFlowChangeEvent;
import org.opendaylight.controller.containermanager.ContainerFlowConfig;
import org.opendaylight.controller.containermanager.NodeConnectorsChangeEvent;
import org.opendaylight.controller.containermanager.ContainerChangeEvent;
import org.opendaylight.controller.containermanager.ContainerConfig;
import org.opendaylight.controller.containermanager.ContainerData;

public class ContainerManager extends Authorization<String> implements IContainerManager, IObjectReader,
        CommandProvider, ICacheUpdateAware<String, Object>, IContainerInternal, IContainerAuthorization,
        IConfigurationAware {
    private static final Logger logger = LoggerFactory.getLogger(ContainerManager.class);
    private static String ROOT = GlobalConstants.STARTUPHOME.toString();
    private static String containersFileName = ROOT + "containers.conf";
    private static final String allContainersGroup = "allContainers";
    private IClusterGlobalServices clusterServices;
    /*
     * Collection containing the configuration objects. This is configuration
     * world: container names (also the map key) are maintained as they were
     * configured by user, same case
     */
    private ConcurrentMap<String, ContainerConfig> containerConfigs;
    private ConcurrentMap<String, ContainerData> containerData;
    private ConcurrentMap<NodeConnector, CopyOnWriteArrayList<String>> nodeConnectorToContainers;
    private ConcurrentMap<Node, Set<String>> nodeToContainers;
    private ConcurrentMap<String, Object> containerChangeEvents;
    private final Set<IContainerAware> iContainerAware = Collections.synchronizedSet(new HashSet<IContainerAware>());
    private final Set<IContainerListener> iContainerListener = Collections
            .synchronizedSet(new HashSet<IContainerListener>());
    private final Set<IContainerLocalListener> iContainerLocalListener = Collections
            .synchronizedSet(new HashSet<IContainerLocalListener>());

    void setIContainerListener(IContainerListener s) {
        if (this.iContainerListener != null) {
            this.iContainerListener.add(s);
            /*
             * At boot with startup, containers are created before listeners have
             * joined. Replaying here the first container creation notification for
             * the joining listener when containers are already present. Also
             * replaying all the node connectors and container flows additions
             * to the existing containers.
             */
            if (!this.containerData.isEmpty()) {
                s.containerModeUpdated(UpdateType.ADDED);
            }
            for (ConcurrentMap.Entry<NodeConnector, CopyOnWriteArrayList<String>> entry : nodeConnectorToContainers
                    .entrySet()) {
                NodeConnector port = entry.getKey();
                for (String container : entry.getValue()) {
                    s.nodeConnectorUpdated(container, port, UpdateType.ADDED);
                }
            }
            for (Map.Entry<String, ContainerData> container : containerData.entrySet()) {
                for (ContainerFlow cFlow : container.getValue().getContainerFlowSpecs()) {
                    s.containerFlowUpdated(container.getKey(), cFlow, cFlow, UpdateType.ADDED);
                }
            }
        }
    }

    void unsetIContainerListener(IContainerListener s) {
        if (this.iContainerListener != null) {
            this.iContainerListener.remove(s);
        }
    }

    void setIContainerLocalListener(IContainerLocalListener s) {
        if (this.iContainerLocalListener != null) {
            this.iContainerLocalListener.add(s);
        }
    }

    void unsetIContainerLocalListener(IContainerLocalListener s) {
        if (this.iContainerLocalListener != null) {
            this.iContainerLocalListener.remove(s);
        }
    }

    public void setIContainerAware(IContainerAware iContainerAware) {
        if (!this.iContainerAware.contains(iContainerAware)) {
            this.iContainerAware.add(iContainerAware);
            // Now call the container creation for all the known containers so far
            for (String container : getContainerNameList()) {
                iContainerAware.containerCreate(container.toLowerCase(Locale.ENGLISH));
            }
        }
    }

    public void unsetIContainerAware(IContainerAware iContainerAware) {
        this.iContainerAware.remove(iContainerAware);
        // There is no need to do cleanup of the component when
        // unregister because it will be taken care by the Container
        // component itself
    }

    public void setClusterServices(IClusterGlobalServices i) {
        this.clusterServices = i;
        logger.debug("IClusterServices set");
    }

    public void unsetClusterServices(IClusterGlobalServices i) {
        if (this.clusterServices == i) {
            this.clusterServices = null;
            logger.debug("IClusterServices Unset");
        }
    }

    private void allocateCaches() {
        logger.debug("Container Manager allocating caches");

        if (clusterServices == null) {
            logger.warn("un-initialized Cluster Services, can't allocate caches");
            return;
        }
        try {
            clusterServices.createCache("containermgr.containerConfigs", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterServices.createCache("containermgr.event.containerChange",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterServices.createCache("containermgr.containerData", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterServices.createCache("containermgr.nodeConnectorToContainers",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterServices.createCache("containermgr.nodeToContainers", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterServices.createCache("containermgr.containerGroups", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterServices.createCache("containermgr.containerAuthorizations",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterServices.createCache("containermgr.roles", EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            logger.error("Cache already exits - destroy and recreate if needed");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        logger.debug("Container Manager retrieving caches");

        if (clusterServices == null) {
            logger.warn("un-initialized Cluster Services, can't retrieve caches");
            return;
        }

        containerConfigs = (ConcurrentMap<String, ContainerConfig>) clusterServices.getCache("containermgr.containerConfigs");

        containerChangeEvents = (ConcurrentMap<String, Object>) clusterServices.getCache("containermgr.event.containerChange");

        containerData = (ConcurrentMap<String, ContainerData>) clusterServices.getCache("containermgr.containerData");

        nodeConnectorToContainers = (ConcurrentMap<NodeConnector, CopyOnWriteArrayList<String>>) clusterServices
                .getCache("containermgr.nodeConnectorToContainers");

        nodeToContainers = (ConcurrentMap<Node, Set<String>>) clusterServices.getCache("containermgr.nodeToContainers");

        resourceGroups = (ConcurrentMap<String, Set<String>>) clusterServices.getCache("containermgr.containerGroups");

        groupsAuthorizations = (ConcurrentMap<String, Set<ResourceGroup>>) clusterServices
                .getCache("containermgr.containerAuthorizations");

        roles = (ConcurrentMap<String, AppRoleLevel>) clusterServices.getCache("containermgr.roles");

        if (inContainerMode()) {
            for (Map.Entry<String, ContainerConfig> entry : containerConfigs.entrySet()) {
                // Notify global and local listeners about the mode change
                notifyContainerChangeInternal(entry.getValue(), UpdateType.ADDED, true);
            }
        }
    }

    @Override
    public void entryCreated(String containerName, String cacheName, boolean originLocal) {

    }

    @Override
    public void entryUpdated(String key, Object value, String cacheName, boolean originLocal) {
        /*
         * This is were container manager replays a configuration event that was
         * notified by its peer from a cluster node where the configuration
         * happened. Only the global listeners, the cluster unaware classes,
         * (mainly the shim classes in the sdn protocol plugins) need to receive
         * these notifications on this cluster node. The cluster aware classes,
         * like the functional modules which reacts on these events, must _not_
         * be notified to avoid parallel computation in the cluster.
         */
        if (!originLocal) {
            if (value instanceof NodeConnectorsChangeEvent) {
                NodeConnectorsChangeEvent event = (NodeConnectorsChangeEvent) value;
                List<NodeConnector> ncList = event.getNodeConnectors();
                notifyContainerEntryChangeInternal(key, ncList, event.getUpdateType(), false);
            } else if (value instanceof ContainerFlowChangeEvent) {
                ContainerFlowChangeEvent event = (ContainerFlowChangeEvent) value;
                notifyCFlowChangeInternal(key, event.getConfigList(), event.getUpdateType(), false);
            } else if (value instanceof ContainerChangeEvent) {
                ContainerChangeEvent event = (ContainerChangeEvent) value;
                notifyContainerChangeInternal(event.getConfig(), event.getUpdateType(), false);
            }
        }
    }

    @Override
    public void entryDeleted(String containerName, String cacheName, boolean originLocal) {
    }

    public ContainerManager() {
    }

    public void init() {

    }

    public void start() {
        // Get caches from cluster manager
        allocateCaches();
        retrieveCaches();

        // Allocates default groups and association to default roles
        createDefaultAuthorizationGroups();

        // Read startup configuration and create local database
        loadConfigurations();
    }

    public void destroy() {
        // Clear local states
        this.iContainerAware.clear();
        this.iContainerListener.clear();
        this.iContainerLocalListener.clear();
    }

    /**
     * Adds/Remove the list of flow specs to/from the specified container. This
     * function is supposed to be called after all the validation checks have
     * already been run on the proposed configuration.
     */
    private Status updateContainerFlow(String containerName, List<ContainerFlowConfig> confList, boolean delete) {
        ContainerData container = getContainerByName(containerName);
        if (container == null) {
            return new Status(StatusCode.GONE, "Container not present");
        }

        for (ContainerFlowConfig conf : confList) {
            // Validation was fine. Modify the database now.
            for (Match match : conf.getMatches()) {
                ContainerFlow cFlow = new ContainerFlow(match);
                if (delete) {
                    logger.trace("Removing Flow Spec {} from Container {}", conf.getName(), containerName);
                    container.deleteFlowSpec(cFlow);
                } else {
                    logger.trace("Adding Flow Spec {} to Container {}", conf.getName(), containerName);
                    container.addFlowSpec(cFlow);

                }
                // Update Database
                putContainerDataByName(containerName, container);
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Adds/Remove this container to/from the Container database, no updates are going
     * to be generated here other that the destroying and creation of the container.
     * This function is supposed to be called after all the validation checks
     * have already been run on the configuration object
     */
    private Status updateContainerDatabase(ContainerConfig containerConf, boolean delete) {
        /*
         * Back-end world here, container names are all stored in lower case
         */
        String containerName = containerConf.getContainerName();
        ContainerData container = getContainerByName(containerName);
        if (delete && container == null) {
            return new Status(StatusCode.NOTFOUND, "Container is not present");
        }
        if (!delete && container != null) {
            // A container with the same (lower case) name already exists
            return new Status(StatusCode.CONFLICT, "A container with the same name already exists");
        }
        if (delete) {
            logger.debug("Removing container {}", containerName);
            removeNodeToContainersMapping(container);
            removeNodeConnectorToContainersMapping(container);
            removeContainerDataByName(containerName);
        } else {
            logger.debug("Adding container {}", containerName);
            container = new ContainerData(containerConf);
            putContainerDataByName(containerName, container);

            // If flow specs are specified, add them
            if (containerConf.hasFlowSpecs()) {
                updateContainerFlow(containerName, containerConf.getContainerFlowConfigs(), delete);
            }

            // If ports are specified, add them
            if (!containerConf.getPortList().isEmpty()) {
                updateContainerEntryDatabase(containerName, containerConf.getPortList(), delete);
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    private void removeNodeConnectorToContainersMapping(ContainerData container) {
        Iterator<Entry<NodeConnector, CopyOnWriteArrayList<String>>> it = nodeConnectorToContainers.entrySet().iterator();
        String containerName = container.getContainerName();
        for (; it.hasNext();) {
            Entry<NodeConnector, CopyOnWriteArrayList<String>> entry = it.next();
            final NodeConnector nc = entry.getKey();
            final CopyOnWriteArrayList<String> slist = entry.getValue();
            for (final String sdata : slist) {
                if (sdata.equalsIgnoreCase(containerName)) {
                    logger.debug("Removing NodeConnector->Containers mapping, nodeConnector: {}", nc);
                    slist.remove(containerName);
                    if (slist.isEmpty()) {
                        nodeConnectorToContainers.remove(nc);
                    } else {
                        nodeConnectorToContainers.put(nc, slist);
                    }
                    break;
                }
            }
        }
    }

    private void removeNodeToContainersMapping(ContainerData container) {
        for (Entry<Node, Set<String>> entry : nodeToContainers.entrySet()) {
            Node node = entry.getKey();
            for (String sdata : entry.getValue()) {
                if (sdata.equals(container.getContainerName())) {
                    logger.debug("Removing Node->Containers mapping, node {} container {}", node, sdata);
                    Set<String> value = nodeToContainers.get(node);
                    value.remove(sdata);
                    nodeToContainers.put(node, value);
                    break;
                }
            }
        }
    }

    /**
     * Adds/Remove container data to/from the container. This function is supposed to be
     * called after all the validation checks have already been run on the
     * configuration object
     */
    private Status updateContainerEntryDatabase(String containerName, List<NodeConnector> nodeConnectors, boolean delete) {
        ContainerData container = getContainerByName(containerName);
        // Presence check
        if (container == null) {
            return new Status(StatusCode.NOTFOUND, "Container Not Found");
        }

        // Check changes in the portlist
        for (NodeConnector port : nodeConnectors) {
            Node node = port.getNode();
            if (delete) {
                container.removePortFromSwitch(port);
                putContainerDataByName(containerName, container);

                /* remove <sp> - container mapping */
                if (nodeConnectorToContainers.containsKey(port)) {
                    nodeConnectorToContainers.remove(port);
                }
                /*
                 * If no more ports in the switch, remove switch from container
                 * Generate switchRemoved Event
                 */
                if (container.portListEmpty(node)) {
                    logger.debug("Port List empty for switch {}", node);
                    putContainerDataByName(containerName, container);
                    // remove node->containers mapping
                    Set<String> slist = nodeToContainers.get(node);
                    if (slist != null) {
                        logger.debug("Removing container from switch-container list. node{}, container{}", node, containerName);
                        slist.remove(container.getContainerName());
                        nodeToContainers.put(node, slist);
                        if (slist.isEmpty()) {
                            logger.debug("Container list empty for switch {}. removing switch-container mapping", node);
                            nodeToContainers.remove(node);
                        }
                    }
                }
            } else {
                if (container.isSwitchInContainer(node) == false) {
                    Set<String> value = nodeToContainers.get(node);
                    // Add node->containers mapping
                    if (value == null) {
                        value = new HashSet<String>();
                        logger.debug("Creating new Container Set for switch {}", node);
                    }
                    value.add(container.getContainerName());
                    nodeToContainers.put(node, value);
                }
                container.addPortToSwitch(port);
                putContainerDataByName(containerName, container);

                // added nc->containers mapping
                CopyOnWriteArrayList<String> slist = nodeConnectorToContainers.get(port);
                if (slist == null) {
                    slist = new CopyOnWriteArrayList<String>();
                }
                slist.add(container.getContainerName());
                nodeConnectorToContainers.put(port, slist);
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status validateContainerFlowAddRemoval(String containerName, ContainerFlow cFlow, boolean delete) {
        /*
         * It used to be the comment below: ~~~~~~~~~~~~~~~~~~~~ If Link Sharing
         * at Host facing interfaces, then disallow last ContainerFlow removal
         * ~~~~~~~~~~~~~~~~~~~~ But the interface being host facing is a
         * condition that can change at runtime and so the final effect will be
         * unreliable. So now we will always allow the container flow removal,
         * if this is a link host facing and is shared by many that will cause
         * issues but that validation should be done not at the configuration
         * but in the UI/northbound side.
         */
        ContainerData container = this.getContainerByName(containerName);
        if (container == null) {
            String error = String.format("Cannot validate flow specs for container %s: (Container does not exist)", containerName);
            logger.warn(error);
            return new Status(StatusCode.BADREQUEST, error);
        }

        if (delete) {
            Set<NodeConnector> thisContainerPorts = container.getNodeConnectors();
            // Go through all the installed containers
            for (Map.Entry<String, ContainerData> entry : containerData.entrySet()) {
                if (containerName.equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                // Derive the common ports
                Set<NodeConnector> commonPorts = entry.getValue().getNodeConnectors();
                commonPorts.retainAll(thisContainerPorts);
                if (commonPorts.isEmpty()) {
                    continue;
                }

                // Check if this operation would remove the only flow spec
                // assigned to this container
                if (container.getFlowSpecCount() == 1) {
                    if (!container.hasStaticVlanAssigned()) {
                        // Ports are shared and static vlan is not present: this
                        // is a failure
                        // regardless the shared ports are host facing or
                        // interswitch ports
                        return new Status(StatusCode.BADREQUEST, "Container shares port with another container: "
                                + "The only one flow spec assigned to this container cannot be removed,"
                                + "because this container is not assigned any static vlan");
                    }

                    // Check on host facing port
                    ITopologyManager topologyManager = (ITopologyManager) ServiceHelper.getInstance(
                            ITopologyManager.class, GlobalConstants.DEFAULT.toString(), this);
                    if (topologyManager == null) {
                        return new Status(StatusCode.NOSERVICE,
                                "Cannot validate the request: Required service is not available");
                    }
                    for (NodeConnector nc : commonPorts) {
                        /*
                         * Shared link case : For internal port check if it has
                         * a vlan configured. If vlan is configured, allow the
                         * flowspec to be deleted If the port is host-facing, do
                         * not allow the flowspec to be deleted
                         */
                        if (!topologyManager.isInternal(nc)) {
                            return new Status(StatusCode.BADREQUEST, String.format(
                                    "Port %s is shared and is host facing port: "
                                            + "The only one flow spec assigned to this container cannot be removed", nc));
                        }
                    }
                }
            }
        } else {
            // Adding a new flow spec: need to check if other containers with common
            // ports do not have same flow spec
            Set<NodeConnector> thisContainerPorts = container.getNodeConnectors();
            List<ContainerFlow> proposed = new ArrayList<ContainerFlow>(container.getContainerFlowSpecs());
            proposed.add(cFlow);
            for (Map.Entry<String, ContainerData> entry : containerData.entrySet()) {
                if (containerName.equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                ContainerData otherContainer = entry.getValue();
                Set<NodeConnector> commonPorts = otherContainer.getNodeConnectors();
                commonPorts.retainAll(thisContainerPorts);

                if (!commonPorts.isEmpty()) {
                    Status status = checkCommonContainerFlow(otherContainer.getContainerFlowSpecs(), proposed);
                    if (!status.isSuccess()) {
                        return new Status(StatusCode.BADREQUEST, String.format(
                                "Container %s which shares ports with this container has overlapping flow spec: %s",
                                entry.getKey(), status.getDescription()));
                    }
                }
            }
        }

        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Checks if the passed list of node connectors can be safely applied to the
     * specified existing container in terms of port sharing with other containers.
     *
     * @param containerName
     *            the name of the existing container
     * @param portList
     *            the list of node connectors to be added to the container
     * @return the status object representing the result of the check
     */
    private Status validatePortSharing(String containerName, List<NodeConnector> portList) {
        ContainerData container = this.getContainerByName(containerName);
        if (container == null) {
            String error = String
                    .format("Cannot validate port sharing for container %s: (container does not exist)", containerName);
            logger.error(error);
            return new Status(StatusCode.BADREQUEST, error);
        }
        return validatePortSharingInternal(portList, container.getContainerFlowSpecs());
    }

    /**
     * Checks if the proposed container configuration is valid to be applied in
     * terms of port sharing with other containers.
     *
     * @param containerConf
     *            the container configuration object containing the list of node
     *            connectors
     * @return the status object representing the result of the check
     */
    private Status validatePortSharing(ContainerConfig containerConf) {
        return validatePortSharingInternal(containerConf.getPortList(), containerConf.getContainerFlowSpecs());
    }

    /*
     * If any port is shared with an existing container, need flowSpec to be
     * configured. If no flowSpec for this or other container, or if containers have any
     * overlapping flowspec in common, then let the caller know this
     * configuration has to be rejected.
     */
    private Status validatePortSharingInternal(List<NodeConnector> portList, List<ContainerFlow> flowSpecList) {
        for (NodeConnector port : portList) {
            List<String> slist = nodeConnectorToContainers.get(port);
            if (slist != null && !slist.isEmpty()) {
                for (String otherContainerName : slist) {
                    String msg = null;
                    ContainerData other = containerData.get(otherContainerName);
                    if (flowSpecList.isEmpty()) {
                        msg = String.format("Port %s is shared and flow spec is empty for this container", port);
                    } else if (other.isFlowSpecEmpty()) {
                        msg = String.format("Port %s is shared and flow spec is empty for the other container", port);
                    } else if (!checkCommonContainerFlow(flowSpecList, other.getContainerFlowSpecs()).isSuccess()) {
                        msg = String.format("Port %s is shared and other container has common flow spec", port);
                    }
                    if (msg != null) {
                        logger.debug(msg);
                        return new Status(StatusCode.BADREQUEST, msg);
                    }
                }
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Utility function to check if two lists of container flows share any same
     * or overlapping container flows.
     *
     * @param oneFlowList
     *            One of the two lists of container flows to test
     * @param twoFlowList
     *            One of the two lists of container flows to test
     * @return The status of the check. Either SUCCESS or CONFLICT. In case of
     *         conflict, the Status will contain the description for the failed
     *         check.
     */
    private Status checkCommonContainerFlow(List<ContainerFlow> oneFlowList, List<ContainerFlow> twoFlowList) {
        for (ContainerFlow oneFlow : oneFlowList) {
            for (ContainerFlow twoFlow : twoFlowList) {
                if (oneFlow.getMatch().intersetcs(twoFlow.getMatch())) {
                    return new Status(StatusCode.CONFLICT, String.format("Flow Specs overlap: %s %s",
                            oneFlow.getMatch(), twoFlow.getMatch()));
                }
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Return the ContainerData object for the passed container name. Given this is a
     * backend database, the lower case version of the passed name is used while
     * searching for the corresponding ContainerData object.
     *
     * @param name
     *            The container name in any case
     * @return The corresponding ContainerData object
     */
    private ContainerData getContainerByName(String name) {
        return containerData.get(name.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Add a ContainerData object for the given container name.
     *
     * @param name
     *            The container name in any case
     * @param sData
     *            The container data object
     */
    private void putContainerDataByName(String name, ContainerData sData) {
        containerData.put(name.toLowerCase(Locale.ENGLISH), sData);
    }

    /**
     * Removes the ContainerData object for the given container name.
     *
     * @param name
     *            The container name in any case
     */
    private void removeContainerDataByName(String name) {
        containerData.remove(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public List<ContainerConfig> getContainerConfigList() {
        return new ArrayList<ContainerConfig>(containerConfigs.values());
    }

    @Override
    public ContainerConfig getContainerConfig(String containerName) {
        ContainerConfig target = containerConfigs.get(containerName);
        return (target == null) ? null : new ContainerConfig(target);
    }

    @Override
    public List<String> getContainerNameList() {
        /*
         * Return container names as they were configured by user (case sensitive)
         * along with the default container
         */
        List<String> containerNameList = new ArrayList<String>();
        containerNameList.add(GlobalConstants.DEFAULT.toString());
        containerNameList.addAll(containerConfigs.keySet());
        return containerNameList;
    }

    @Override
    public Map<String, List<ContainerFlowConfig>> getContainerFlows() {
        Map<String, List<ContainerFlowConfig>> flowSpecConfig = new HashMap<String, List<ContainerFlowConfig>>();
        for (Map.Entry<String, ContainerConfig> entry : containerConfigs.entrySet()) {
            List<ContainerFlowConfig> set = entry.getValue().getContainerFlowConfigs();
            flowSpecConfig.put(entry.getKey(), set);
        }
        return flowSpecConfig;
    }

    private void loadConfigurations() {
        /*
         * Read containers, container flows and finally containers' entries from file
         * and program the database accordingly
         */
        if (containerConfigs.isEmpty()) {
            loadContainerConfig();
        }
    }

    private Status saveContainerConfig() {
        return saveContainerConfigLocal();
    }

    public Status saveContainerConfigLocal() {
        ObjectWriter objWriter = new ObjectWriter();

        Status status = objWriter.write(new ConcurrentHashMap<String, ContainerConfig>(containerConfigs), containersFileName);
        if (!status.isSuccess()) {
            return new Status(StatusCode.INTERNALERROR, "Failed to save container configurations: "
                    + status.getDescription());
        }
        return new Status(StatusCode.SUCCESS);
    }

    private void removeComponentsStartUpfiles(String containerName) {
        String startupLocation = String.format("./%s", GlobalConstants.STARTUPHOME.toString());
        String containerPrint = String.format("_%s.", containerName.toLowerCase(Locale.ENGLISH));

        File directory = new File(startupLocation);
        String[] fileList = directory.list();

        logger.trace("Deleting startup configuration files for container {}", containerName);
        if (fileList != null) {
            for (String fileName : fileList) {
                if (fileName.contains(containerPrint)) {
                    String fullPath = String.format("%s/%s", startupLocation, fileName);
                    File file = new File(fullPath);
                    boolean done = file.delete();
                    logger.trace("{} {}", (done ? "Deleted: " : "Failed to delete: "), fileName);
                }
            }
        }
    }

    /**
     * Create and initialize default all resource group and create association
     * with default well known users and profiles, if not already learnt from
     * another cluster node
     */
    private void createDefaultAuthorizationGroups() {
        allResourcesGroupName = ContainerManager.allContainersGroup;

        // Add the default container to the all containers group if needed
        String defaultContainer = GlobalConstants.DEFAULT.toString();
        Set<String> allContainers = (resourceGroups.containsKey(allResourcesGroupName)) ? resourceGroups
                .get(allResourcesGroupName) : new HashSet<String>();
        if (!allContainers.contains(defaultContainer)) {
            // Add Default container
            allContainers.add(defaultContainer);
            // Update cluster
            resourceGroups.put(allResourcesGroupName, allContainers);
        }

        // Add the controller well known roles, if not known already
        if (!roles.containsKey(UserLevel.SYSTEMADMIN.toString())) {
            roles.put(UserLevel.SYSTEMADMIN.toString(), AppRoleLevel.APPADMIN);
        }
        if (!roles.containsKey(UserLevel.NETWORKADMIN.toString())) {
            roles.put(UserLevel.NETWORKADMIN.toString(), AppRoleLevel.APPADMIN);
        }
        if (!roles.containsKey(UserLevel.NETWORKOPERATOR.toString())) {
            roles.put(UserLevel.NETWORKOPERATOR.toString(), AppRoleLevel.APPOPERATOR);
        }

        /*
         * Create and add the all containers user groups and associate them to the
         * default well known user roles, if not present already
         */
        if (!groupsAuthorizations.containsKey(UserLevel.NETWORKADMIN.toString())) {
            Set<ResourceGroup> writeProfile = new HashSet<ResourceGroup>(1);
            Set<ResourceGroup> readProfile = new HashSet<ResourceGroup>(1);
            writeProfile.add(new ResourceGroup(allResourcesGroupName, Privilege.WRITE));
            readProfile.add(new ResourceGroup(allResourcesGroupName, Privilege.READ));
            groupsAuthorizations.put(UserLevel.SYSTEMADMIN.toString(), writeProfile);
            groupsAuthorizations.put(UserLevel.NETWORKADMIN.toString(), writeProfile);
            groupsAuthorizations.put(UserLevel.NETWORKOPERATOR.toString(), readProfile);
        }
    }

    /**
     * Until manual configuration is not available, automatically maintain the
     * well known resource groups
     *
     * @param containerName
     * @param delete
     */
    private void updateResourceGroups(ContainerConfig containerConf, boolean delete) {
        // Container Roles and Container Resource Group
        String containerName = containerConf.getContainer();
        String groupName = containerConf.getContainerGroupName();
        String containerAdminRole = containerConf.getContainerAdminRole();
        String containerOperatorRole = containerConf.getContainerOperatorRole();
        Set<String> allContainerSet = resourceGroups.get(allResourcesGroupName);
        if (delete) {
            resourceGroups.remove(groupName);
            groupsAuthorizations.remove(containerAdminRole);
            groupsAuthorizations.remove(containerOperatorRole);
            roles.remove(containerAdminRole);
            roles.remove(containerOperatorRole);
            // Update the all container group
            allContainerSet.remove(containerName);
        } else {
            Set<String> resources = new HashSet<String>(1);
            resources.add(containerName);
            resourceGroups.put(groupName, resources);
            Set<ResourceGroup> adminGroups = new HashSet<ResourceGroup>(1);
            Set<ResourceGroup> operatorGroups = new HashSet<ResourceGroup>(1);
            adminGroups.add(new ResourceGroup(groupName, Privilege.WRITE));
            operatorGroups.add(new ResourceGroup(groupName, Privilege.READ));
            groupsAuthorizations.put(containerAdminRole, adminGroups);
            groupsAuthorizations.put(containerOperatorRole, operatorGroups);
            roles.put(containerAdminRole, AppRoleLevel.APPADMIN);
            roles.put(containerOperatorRole, AppRoleLevel.APPOPERATOR);
            // Update the all containers resource group
            allContainerSet.add(containerName);
        }
        // Update resource groups in cluster
        resourceGroups.put(allResourcesGroupName, allContainerSet);
    }

    /**
     * Notify ContainerAware listeners of the creation/deletion of the container
     *
     * @param containerName
     * @param delete
     *            true is container was removed, false otherwise
     */
    private void notifyContainerAwareListeners(String containerName, boolean delete) {
        // Back-end World: container name forced to lower case
        String name = containerName.toLowerCase(Locale.ENGLISH);

        synchronized (this.iContainerAware) {
            for (IContainerAware i : this.iContainerAware) {
                if (delete) {
                    i.containerDestroy(name);
                } else {
                    i.containerCreate(name);
                }
            }
        }
    }

    /**
     * Notify the ContainerListener listeners in case the container mode has
     * changed following a container configuration operation Note: this call
     * must happen after the configuration db has been updated
     *
     * @param lastActionDelete
     *            true if the last container configuration operation was a
     *            container delete operation
     * @param notifyLocal
     *            if true, the notification is also sent to the
     *            IContainerLocalListener classes besides the IContainerListener
     *            classes
     */
    private void notifyContainerModeChange(boolean lastActionDelete, boolean notifyLocal) {
        if (lastActionDelete == false && containerConfigs.size() == 1) {
            logger.info("First container Creation. Inform listeners");
            synchronized (this.iContainerListener) {
                for (IContainerListener i : this.iContainerListener) {
                    i.containerModeUpdated(UpdateType.ADDED);
                }
            }
            if (notifyLocal) {
                synchronized (this.iContainerLocalListener) {
                    for (IContainerLocalListener i : this.iContainerLocalListener) {
                        i.containerModeUpdated(UpdateType.ADDED);
                    }
                }
            }
        } else if (lastActionDelete == true && containerConfigs.isEmpty()) {
            logger.info("Last container Deletion. Inform listeners");
            synchronized (this.iContainerListener) {
                for (IContainerListener i : this.iContainerListener) {
                    i.containerModeUpdated(UpdateType.REMOVED);
                }
            }
            if (notifyLocal) {
                synchronized (this.iContainerLocalListener) {
                    for (IContainerLocalListener i : this.iContainerLocalListener) {
                        i.containerModeUpdated(UpdateType.REMOVED);
                    }
                }
            }
        }
    }

    private Status addRemoveContainerEntries(String containerName, List<String> nodeConnectorsString, boolean delete) {
        // Construct action message
        String action = String.format("Node connector(s) %s container %s: %s", delete ? "removal from" : "addition to",
                containerName, nodeConnectorsString);

        // Validity Check
        if (nodeConnectorsString == null || nodeConnectorsString.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Node connector list is null or empty");
        }

        // Presence check
        ContainerConfig entryConf = containerConfigs.get(containerName);
        if (entryConf == null) {
            String msg = String.format("Container not found: %s", containerName);
            String error = String.format("Failed to apply %s: (%s)", action, msg);
            logger.warn(error);
            return new Status(StatusCode.NOTFOUND, msg);
        }

        // Validation check
        Status status = ContainerConfig.validateNodeConnectors(nodeConnectorsString);
        if (!status.isSuccess()) {
            String error = String.format("Failed to apply %s: (%s)", action, status.getDescription());
            logger.warn(error);
            return status;
        }

        List<NodeConnector> nodeConnectors = ContainerConfig.nodeConnectorsFromString(nodeConnectorsString);

        // Port sharing check
        if (!delete) {
            /*
             * Check if the ports being added to this container already belong to
             * other containers. If so check whether the the appropriate flow specs
             * are configured on this container
             */
            status = validatePortSharing(containerName, nodeConnectors);
            if (!status.isSuccess()) {
                String error = String.format("Failed to apply %s: (%s)", action, status.getDescription());
                logger.warn(error);
                return status;
            }
        }

        // Update Database
        status = updateContainerEntryDatabase(containerName, nodeConnectors, delete);
        if (!status.isSuccess()) {
            String error = String.format("Failed to apply %s: (%s)", action, status.getDescription());
            logger.warn(error);
            return status;
        }

        // Update Configuration
        status = (delete) ? entryConf.removeNodeConnectors(nodeConnectorsString) : entryConf
                .addNodeConnectors(nodeConnectorsString);
        if (!status.isSuccess()) {
            String error = String.format("Failed to modify config for %s: (%s)", action, status.getDescription());
            logger.warn(error);
            // Revert backend changes
            Status statusRevert = updateContainerEntryDatabase(containerName, nodeConnectors, !delete);
            if (!statusRevert.isSuccess()) {
                // Unlikely
                logger.error("Failed to revert changes in database (CRITICAL)");
            }
            return status;
        }

        // Update cluster Configuration cache
        containerConfigs.put(containerName, entryConf);

        // Notify global and local listeners
        UpdateType update = (delete) ? UpdateType.REMOVED : UpdateType.ADDED;
        notifyContainerEntryChangeInternal(containerName, nodeConnectors, update, true);
        // Trigger cluster notification
        containerChangeEvents.put(containerName, new NodeConnectorsChangeEvent(nodeConnectors, update));

        return status;
    }

    private void notifyContainerChangeInternal(ContainerConfig conf, UpdateType update, boolean notifyLocal) {
        String containerName = conf.getContainerName();
        logger.trace("Notifying listeners on {} for container {}", update, containerName);
        // Back-end World: container name forced to lower case
        String container = containerName.toLowerCase(Locale.ENGLISH);
        boolean delete = (update == UpdateType.REMOVED);
        // Check if a container mode change notification is needed
        notifyContainerModeChange(delete, notifyLocal);
        // Notify listeners
        notifyContainerAwareListeners(container, delete);

        /*
         * This is a quick fix until configuration service becomes the
         * centralized configuration management place. Here container manager
         * will remove the startup files for all the bundles that are present in
         * the container being deleted. Do the cleanup here in Container manger
         * as do not want to put this temporary code in Configuration manager
         * yet which is ODL.
         */
        if (delete) {
            // TODO: remove when Config Mgr takes over
            removeComponentsStartUpfiles(containerName);
        }
    }

    private void notifyContainerEntryChangeInternal(String containerName, List<NodeConnector> ncList, UpdateType update, boolean notifyLocal) {
        logger.trace("Notifying listeners on {} for ports {} in container {}", update, ncList, containerName);
        // Back-end World: container name forced to lower case
        String container = containerName.toLowerCase(Locale.ENGLISH);
        for (NodeConnector nodeConnector : ncList) {
            // Now signal that the port has been added/removed
            synchronized (this.iContainerListener) {
                for (IContainerListener i : this.iContainerListener) {
                    i.nodeConnectorUpdated(container, nodeConnector, update);
                }
            }
            // Check if the Functional Modules need to be notified as well
            if (notifyLocal) {
                synchronized (this.iContainerLocalListener) {
                    for (IContainerLocalListener i : this.iContainerLocalListener) {
                        i.nodeConnectorUpdated(container, nodeConnector, update);
                    }
                }
            }
        }
    }

    private void notifyCFlowChangeInternal(String containerName, List<ContainerFlowConfig> confList, UpdateType update,
            boolean notifyLocal) {
        logger.trace("Notifying listeners on {} for flow specs {} in container {}", update, confList, containerName);
        // Back-end World: container name forced to lower case
        String container = containerName.toLowerCase(Locale.ENGLISH);

        for (ContainerFlowConfig conf : confList) {
            for (Match match : conf.getMatches()) {
                ContainerFlow cFlow = new ContainerFlow(match);
                synchronized (this.iContainerListener) {
                    for (IContainerListener i : this.iContainerListener) {
                        i.containerFlowUpdated(container, cFlow, cFlow, update);
                    }
                }
                // Check if the Functional Modules need to be notified as well
                if (notifyLocal) {
                    synchronized (this.iContainerLocalListener) {
                        for (IContainerLocalListener i : this.iContainerLocalListener) {
                            i.containerFlowUpdated(container, cFlow, cFlow, update);
                        }
                    }
                }
            }
        }
    }

    private Status addRemoveContainerFlow(String containerName, List<ContainerFlowConfig> cFlowConfList, boolean delete) {
        // Construct action message
        String action = String.format("Flow spec(s) %s container %s: %s", delete ? "removal from" : "addition to",
                containerName, cFlowConfList);

        // Presence check
        ContainerConfig containerConfig = this.containerConfigs.get(containerName);
        if (containerConfig == null) {
            String msg = String.format("Container not found: %s", containerName);
            String error = String.format("Failed to apply %s: (%s)", action, msg);
            logger.warn(error);
            return new Status(StatusCode.NOTFOUND, "Container not present");
        }

        // Validity check, check for overlaps on current container configuration
        Status status = containerConfig.validateContainerFlowModify(cFlowConfList, delete);
        if (!status.isSuccess()) {
            String msg = status.getDescription();
            String error = String.format("Failed to apply %s: (%s)", action, msg);
            logger.warn(error);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        // Validate the operation in terms to the port sharing with other containers
        for (ContainerFlowConfig conf : cFlowConfList) {
            for (Match match : conf.getMatches()) {
                ContainerFlow cFlow = new ContainerFlow(match);
                status = validateContainerFlowAddRemoval(containerName, cFlow, delete);
                if (!status.isSuccess()) {
                    String msg = "Validation failed: " + status.getDescription();
                    String error = String.format("Failed to apply %s: (%s)", action, msg);
                    logger.warn(error);
                    return new Status(StatusCode.BADREQUEST, msg);
                }
            }
        }

        // Update Database
        status = updateContainerFlow(containerName, cFlowConfList, delete);
        if (!status.isSuccess()) {
            String error = String.format("Failed to apply %s: (%s)", action, status.getDescription());
            logger.error(error);
            return status;
        }

        // Update Configuration
        status = (delete) ? containerConfig.removeContainerFlows(cFlowConfList) : containerConfig
                .addContainerFlows(cFlowConfList);
        if (!status.isSuccess()) {
            String error = String.format("Failed to modify config for %s: (%s)", action, status.getDescription());
            logger.error(error);
            // Revert backend changes
            Status statusRevert = updateContainerFlow(containerName, cFlowConfList, !delete);
            if (!statusRevert.isSuccess()) {
                // Unlikely
                logger.error("Failed to revert changes in database (CRITICAL)");
            }
            return status;
        }
        // Update cluster cache
        this.containerConfigs.put(containerName, containerConfig);

        // Notify global and local listeners
        UpdateType update = (delete) ? UpdateType.REMOVED : UpdateType.ADDED;
        notifyCFlowChangeInternal(containerName, cFlowConfList, update, true);
        // Trigger cluster notification
        containerChangeEvents.put(containerName, new ContainerFlowChangeEvent(cFlowConfList, update));

        return status;
    }

    private Status addRemoveContainer(ContainerConfig containerConf, boolean delete) {
        // Construct action message
        String action = String.format("Container %s", delete ? "removal" : "creation");

        // Valid configuration check
        Status status = null;
        String error = (containerConfigs == null) ? String.format("Invalid %s configuration: (null config object)", action)
                : (!(status = containerConf.validate()).isSuccess()) ? String.format("Invalid %s configuration: (%s)",
                        action, status.getDescription()) : null;
        if (error != null) {
            logger.warn(error);
            return new Status(StatusCode.BADREQUEST, error);
        }

        // Configuration presence check
        String containerName = containerConf.getContainerName();
        if (delete) {
            if (!containerConfigs.containsKey(containerName)) {
                String msg = String.format("%s Failed: (Container does not exist: %s)", action, containerName);
                logger.warn(msg);
                return new Status(StatusCode.NOTFOUND, msg);
            }
        } else {
            if (containerConfigs.containsKey(containerName)) {
                String msg = String.format("%s Failed: (Container already exist: %s)", action, containerName);
                logger.warn(msg);
                return new Status(StatusCode.CONFLICT, msg);
            }
        }

        /*
         * The proposed container configuration could be a complex one containing
         * both ports and flow spec. If so, check if it has shared ports with
         * other existing containers. If that is the case verify flow spec isolation
         * is in place. No need to check on flow spec validation first. This
         * would take care of both
         */
        if (!delete) {
            status = validatePortSharing(containerConf);
            if (!status.isSuccess()) {
                error = String.format("%s Failed: (%s)", action, status.getDescription());
                logger.error(error);
                return status;
            }
        }

        // Update Database
        status = updateContainerDatabase(containerConf, delete);

        // Abort and exit here if back-end database update failed
        if (!status.isSuccess()) {
            return status;
        }

        /*
         * Update Configuration: This will trigger the notifications on cache
         * update callback locally and on the other cluster nodes
         */
        if (delete) {
            this.containerConfigs.remove(containerName);
        } else {
            this.containerConfigs.put(containerName, containerConf);
        }

        // Automatically create and populate user and resource groups
        updateResourceGroups(containerConf, delete);

        // Notify global and local listeners
        UpdateType update = (delete) ? UpdateType.REMOVED : UpdateType.ADDED;
        notifyContainerChangeInternal(containerConf, update, true);

        // Trigger cluster notification
        containerChangeEvents.put(containerName, new ContainerChangeEvent(containerConf, update));

        if (update == UpdateType.ADDED) {
            if (containerConf.hasFlowSpecs()) {
                List<ContainerFlowConfig> specList = containerConf.getContainerFlowConfigs();
                // Notify global and local listeners about flow spec addition
                notifyCFlowChangeInternal(containerName, specList, update, true);

                // Trigger cluster notification
                containerChangeEvents.put(containerName, new ContainerFlowChangeEvent(specList, update));
            }

            if (containerConf.hasNodeConnectors()) {
                List<NodeConnector> ncList = containerConf.getPortList();
                // Notify global and local listeners about port(s) addition
                notifyContainerEntryChangeInternal(containerName, ncList, update, true);
                // Trigger cluster notification
                containerChangeEvents.put(containerName, new NodeConnectorsChangeEvent(ncList, update));
            }
        }

        if (delete) {
            clusterServices.removeContainerCaches(containerName);
        }
        return status;
    }

    @Override
    public Status addContainer(ContainerConfig containerConf) {
        return addRemoveContainer(containerConf, false);
    }

    @Override
    public Status removeContainer(ContainerConfig containerConf) {
        return addRemoveContainer(containerConf, true);
    }

    @Override
    public Status removeContainer(String containerName) {
        // Construct action message
        String action = String.format("Container removal: %s", containerName);

        ContainerConfig containerConf = containerConfigs.get(containerName);
        if (containerConf == null) {
            String msg = String.format("Container not found");
            String error = String.format("Failed to apply %s: (%s)", action, msg);
            logger.warn(error);
            return new Status(StatusCode.NOTFOUND, msg);
        }
        return addRemoveContainer(containerConf, true);
    }

    @Override
    public Status addContainerEntry(String containerName, List<String> nodeConnectors) {
        return addRemoveContainerEntries(containerName, nodeConnectors, false);
    }

    @Override
    public Status removeContainerEntry(String containerName, List<String> nodeConnectors) {
        return addRemoveContainerEntries(containerName, nodeConnectors, true);
    }

    @Override
    public Status addContainerFlows(String containerName, List<ContainerFlowConfig> fSpecConf) {
        return addRemoveContainerFlow(containerName, fSpecConf, false);
    }

    @Override
    public Status removeContainerFlows(String containerName, List<ContainerFlowConfig> fSpecConf) {
        return addRemoveContainerFlow(containerName, fSpecConf, true);
    }

    @Override
    public Status removeContainerFlows(String containerName, Set<String> names) {
        // Construct action message
        String action = String.format("Flow spec(s) removal from container %s: %s", containerName, names);

        // Presence check
        ContainerConfig sc = containerConfigs.get(containerName);
        if (sc == null) {
            String msg = String.format("Container not found: %s", containerName);
            String error = String.format("Failed to apply %s: (%s)", action, msg);
            logger.warn(error);
            return new Status(StatusCode.NOTFOUND, msg);
        }
        List<ContainerFlowConfig> list = sc.getContainerFlowConfigs(names);
        if (list.isEmpty() || list.size() != names.size()) {
            String msg = String.format("Cannot find all the specified flow specs");
            String error = String.format("Failed to apply %s: (%s)", action, msg);
            logger.warn(error);
            return new Status(StatusCode.BADREQUEST, msg);
        }
        return addRemoveContainerFlow(containerName, list, true);
    }

    @Override
    public List<ContainerFlowConfig> getContainerFlows(String containerName) {
        ContainerConfig sc = containerConfigs.get(containerName);
        return (sc == null) ? new ArrayList<ContainerFlowConfig>(0) : sc.getContainerFlowConfigs();
    }

    @Override
    public List<String> getContainerFlowNameList(String containerName) {
        ContainerConfig sc = containerConfigs.get(containerName);
        return (sc == null) ? new ArrayList<String>(0) : sc.getContainerFlowConfigsNames();
    }

    @Override
    public Object readObject(ObjectInputStream ois) throws FileNotFoundException, IOException, ClassNotFoundException {
        // Perform the class deserialization locally, from inside the package
        // where the class is defined
        return ois.readObject();
    }

    @SuppressWarnings("unchecked")
    private void loadContainerConfig() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, ContainerConfig> configMap = (ConcurrentMap<String, ContainerConfig>) objReader.read(this,
                containersFileName);

        if (configMap == null) {
            return;
        }

        for (Map.Entry<String, ContainerConfig> configEntry : configMap.entrySet()) {
            addContainer(configEntry.getValue());
        }
    }

    public void _psc(CommandInterpreter ci) {
        for (Map.Entry<String, ContainerConfig> entry : containerConfigs.entrySet()) {
            ContainerConfig sc = entry.getValue();
            ci.println(String.format("%s: %s", sc.getContainerName(), sc.toString()));
        }
        ci.println("Total number of containers: " + containerConfigs.entrySet().size());
    }

    public void _pfc(CommandInterpreter ci) {
        for (Map.Entry<String, ContainerConfig> entry : containerConfigs.entrySet()) {
            ContainerConfig sc = entry.getValue();
            ci.println(String.format("%s: %s", sc.getContainerName(), sc.getContainerFlowConfigs()));
        }
    }

    public void _psd(CommandInterpreter ci) {
        for (String containerName : containerData.keySet()) {
            ContainerData sd = containerData.get(containerName);
            for (Node sid : sd.getSwPorts().keySet()) {
                Set<NodeConnector> s = sd.getSwPorts().get(sid);
                ci.println("\t" + sid + " : " + s);
            }

            for (ContainerFlow s : sd.getContainerFlowSpecs()) {
                ci.println("\t" + s.toString());
            }
        }
    }

    public void _psp(CommandInterpreter ci) {
        for (NodeConnector sp : nodeConnectorToContainers.keySet()) {
            ci.println(nodeConnectorToContainers.get(sp));
        }
    }

    public void _psm(CommandInterpreter ci) {
        for (Node sp : nodeToContainers.keySet()) {
            ci.println(nodeToContainers.get(sp));
        }
    }

    public void _addContainer(CommandInterpreter ci) {
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.print("Container Name not specified");
            return;
        }
        String staticVlan = ci.nextArgument();
        ContainerConfig containerConfig = new ContainerConfig(containerName, staticVlan, null, null);
        ci.println(this.addRemoveContainer(containerConfig, false));
    }

    public void _createContainer(CommandInterpreter ci) {
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.print("Container Name not specified");
            return;
        }
        String staticVlan = ci.nextArgument();
        if (staticVlan == null) {
            ci.print("Static Vlan not specified");
            return;
        }
        List<String> ports = new ArrayList<String>();
        for (long l = 1L; l < 10L; l++) {
            ports.add(NodeConnectorCreator.createOFNodeConnector((short) 1, NodeCreator.createOFNode(l)).toString());
        }
        List<ContainerFlowConfig> cFlowList = new ArrayList<ContainerFlowConfig>();
        cFlowList.add(this.createSampleContainerFlowConfig("tcp", true));
        ContainerConfig containerConfig = new ContainerConfig(containerName, staticVlan, ports, cFlowList);
        ci.println(this.addRemoveContainer(containerConfig, false));
    }

    public void _removeContainer(CommandInterpreter ci) {
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.print("Container Name not specified");
            return;
        }
        ContainerConfig containerConfig = new ContainerConfig(containerName, "", null, null);
        ci.println(this.addRemoveContainer(containerConfig, true));
    }

    public void _addContainerEntry(CommandInterpreter ci) {
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.print("Container Name not specified");
            return;
        }
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node Id not specified");
            return;
        }
        String portId = ci.nextArgument();
        if (portId == null) {
            ci.print("Port not specified");
            return;
        }
        Node node = NodeCreator.createOFNode(Long.valueOf(nodeId));
        Short port = Short.valueOf(portId);
        NodeConnector nc = NodeConnectorCreator.createOFNodeConnector(port, node);
        List<String> portList = new ArrayList<String>(1);
        portList.add(nc.toString());
        ci.println(this.addRemoveContainerEntries(containerName, portList, false));
    }

    public void _removeContainerEntry(CommandInterpreter ci) {
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.print("Container Name not specified");
            return;
        }
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node Id not specified");
            return;
        }
        String portId = ci.nextArgument();
        if (portId == null) {
            ci.print("Port not specified");
            return;
        }
        Node node = NodeCreator.createOFNode(Long.valueOf(nodeId));
        Short port = Short.valueOf(portId);
        NodeConnector nc = NodeConnectorCreator.createOFNodeConnector(port, node);
        List<String> portList = new ArrayList<String>(1);
        portList.add(nc.toString());
        ci.println(this.addRemoveContainerEntries(containerName, portList, true));
    }

    private ContainerFlowConfig createSampleContainerFlowConfig(String cflowName, boolean boolUnidirectional) {
        ContainerFlowConfig cfg = new ContainerFlowConfig(cflowName, "9.9.1.0/24", "19.9.1.2", "TCP", "1234", "25");
        return cfg;
    }

    public void _addContainerFlow(CommandInterpreter ci) {
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.print("Container Name not specified");
            return;
        }
        String cflowName = ci.nextArgument();
        if (cflowName == null) {
            ci.print("cflowName not specified");
            return;
        }
        String unidirectional = ci.nextArgument();
        boolean boolUnidirectional = Boolean.parseBoolean(unidirectional);
        List<ContainerFlowConfig> list = new ArrayList<ContainerFlowConfig>();
        list.add(createSampleContainerFlowConfig(cflowName, boolUnidirectional));
        ci.println(this.addRemoveContainerFlow(containerName, list, false));
    }

    public void _removeContainerFlow(CommandInterpreter ci) {
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.print("Container Name not specified");
            return;
        }
        String cflowName = ci.nextArgument();
        if (cflowName == null) {
            ci.print("cflowName not specified");
            return;
        }
        Set<String> set = new HashSet<String>(1);
        set.add(cflowName);
        ci.println(this.removeContainerFlows(containerName, set));
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---ContainerManager Testing---\n");
        help.append("\tpsc        - Print ContainerConfigs\n");
        help.append("\tpfc        - Print FlowSpecConfigs\n");
        help.append("\tpsd        - Print ContainerData\n");
        help.append("\tpsp        - Print nodeConnectorToContainers\n");
        help.append("\tpsm        - Print nodeToContainers\n");
        help.append("\t addContainer <containerName> <staticVlan> \n");
        help.append("\t removeContainer <containerName> \n");
        help.append("\t addContainerEntry <containerName> <nodeId> <port> \n");
        help.append("\t removeContainerEntry <containerName> <nodeId> <port> \n");
        help.append("\t addContainerFlow <containerName> <cflowName> <unidirectional true/false>\n");
        help.append("\t removeContainerFlow <containerName> <cflowName> \n");
        return help.toString();
    }

    @Override
    public boolean doesContainerExist(String containerName) {
        // Test for default container
        if (GlobalConstants.DEFAULT.toString().equalsIgnoreCase(containerName)) {
            return true;
        }
        // Test for non-default one
        return (getContainerByName(containerName) != null);
    }

    @Override
    public ContainerData getContainerData(String containerName) {
        return (getContainerByName(containerName));
    }

    @Override
    public Status saveConfiguration() {
        return saveContainerConfig();
    }

    public void _containermgrGetRoles(CommandInterpreter ci) {
        ci.println("Configured roles for Container Mgr:");
        List<String> list = this.getRoles();
        for (String role : list) {
            ci.println(role + "\t" + roles.get(role));
        }
    }

    public void _containermgrGetAuthorizedGroups(CommandInterpreter ci) {
        String roleName = ci.nextArgument();
        if (roleName == null || roleName.trim().isEmpty()) {
            ci.println("Invalid argument");
            ci.println("mmGetAuthorizedGroups <role_name>");
            return;
        }
        ci.println("Resource Groups associated to role " + roleName + ":");
        List<ResourceGroup> list = this.getAuthorizedGroups(roleName);
        for (ResourceGroup group : list) {
            ci.println(group.toString());
        }
    }

    public void _containermgrGetAuthorizedResources(CommandInterpreter ci) {
        String roleName = ci.nextArgument();
        if (roleName == null || roleName.trim().isEmpty()) {
            ci.println("Invalid argument");
            ci.println("mmGetAuthorizedResources <role_name>");
            return;
        }
        ci.println("Resource associated to role " + roleName + ":");
        List<Resource> list = this.getAuthorizedResources(roleName);
        for (Resource resource : list) {
            ci.println(resource.toString());
        }
    }

    public void _containermgrGetResourcesForGroup(CommandInterpreter ci) {
        String groupName = ci.nextArgument();
        if (groupName == null || groupName.trim().isEmpty()) {
            ci.println("Invalid argument");
            ci.println("containermgrResourcesForGroup <group_name>");
            return;
        }
        ci.println("Group " + groupName + " contains the following resources:");
        List<Object> resources = this.getResources(groupName);
        for (Object resource : resources) {
            ci.println(resource.toString());
        }
    }

    public void _containermgrGetUserLevel(CommandInterpreter ci) {
        String userName = ci.nextArgument();
        if (userName == null || userName.trim().isEmpty()) {
            ci.println("Invalid argument");
            ci.println("containermgrGetUserLevel <user_name>");
            return;
        }
        ci.println("User " + userName + " has level: " + this.getUserLevel(userName));
    }

    public void _containermgrGetUserResources(CommandInterpreter ci) {
        String userName = ci.nextArgument();
        if (userName == null || userName.trim().isEmpty()) {
            ci.println("Invalid argument");
            ci.println("containermgrGetUserResources <user_name>");
            return;
        }
        ci.println("User " + userName + " owns the following resources: ");
        Set<Resource> resources = this.getAllResourcesforUser(userName);
        for (Resource resource : resources) {
            ci.println(resource.toString());
        }
    }

    /*
     * For scalability testing where as of now controller gui is unresponsive
     * providing here an osgi hook to trigger the save config so that DT do not
     * have to reaply the scalable configuration each time they restart the
     * controller
     */
    // TODO: remove when no longer needed
    public void _saveConfig(CommandInterpreter ci) {
        Status status = new Status(StatusCode.NOSERVICE, "Configuration service not reachable");

        IConfigurationService configService = (IConfigurationService) ServiceHelper.getGlobalInstance(
                IConfigurationService.class, this);
        if (configService != null) {
            status = configService.saveConfigurations();
        }
        ci.println(status.toString());
    }

    @Override
    public List<String> getContainerNames() {
        return getContainerNameList();
    }

    @Override
    public boolean hasNonDefaultContainer() {
        return !containerConfigs.keySet().isEmpty();
    }

    @Override
    public boolean inContainerMode() {
        return this.containerConfigs.size() > 0;
    }
}
