/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.ConfigurationObject;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntryInstall;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManagerAware;
import org.opendaylight.controller.forwardingrulesmanager.PortGroup;
import org.opendaylight.controller.forwardingrulesmanager.PortGroupChangeListener;
import org.opendaylight.controller.forwardingrulesmanager.PortGroupConfig;
import org.opendaylight.controller.forwardingrulesmanager.PortGroupProvider;
import org.opendaylight.controller.forwardingrulesmanager.implementation.data.FlowEntryDistributionOrder;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.action.Enqueue;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.connection.ConnectionLocality;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.IContainerLocalListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerListener;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.switchmanager.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that manages forwarding rule installation and removal per container of
 * the network. It also maintains the central repository of all the forwarding
 * rules installed on the network nodes.
 */
public class ForwardingRulesManager implements
        IForwardingRulesManager,
        PortGroupChangeListener,
        IContainerLocalListener,
        ISwitchManagerAware,
        IConfigurationContainerAware,
        IInventoryListener,
        IObjectReader,
        ICacheUpdateAware<Object,Object>,
        IFlowProgrammerListener {

    private static final Logger log = LoggerFactory.getLogger(ForwardingRulesManager.class);
    private static final Logger logsync = LoggerFactory.getLogger("FRMsync");
    private static final String PORT_REMOVED = "Port removed";
    private static final String NODE_DOWN = "Node is Down";
    private static final String INVALID_FLOW_ENTRY = "Invalid FlowEntry";
    private static final String STATIC_FLOWS_FILE_NAME = "frm_staticflows.conf";
    private static final String PORT_GROUP_FILE_NAME = "portgroup.conf";
    private ConcurrentMap<Integer, FlowConfig> staticFlows;
    private ConcurrentMap<Integer, Integer> staticFlowsOrdinal;
    private ConcurrentMap<String, PortGroupConfig> portGroupConfigs;
    private ConcurrentMap<PortGroupConfig, Map<Node, PortGroup>> portGroupData;
    private ConcurrentMap<String, Object> TSPolicies;
    private IContainerManager containerManager;
    private IConfigurationContainerService configurationService;
    private boolean inContainerMode; // being used by global instance only
    protected boolean stopping;

    /*
     * Flow database. It's the software view of what was requested to install
     * and what is installed on the switch. It is indexed by the entry itself.
     * The entry's hashcode resumes the network node index, the flow's priority
     * and the flow's match. The value element is a class which contains the
     * flow entry pushed by the applications modules and the respective
     * container flow merged version. In absence of container flows, the two
     * flow entries are the same.
     */
    private ConcurrentMap<FlowEntry, FlowEntry> originalSwView;
    private ConcurrentMap<FlowEntryInstall, FlowEntryInstall> installedSwView;
    /*
     * Per node and per group indexing
     */
    private ConcurrentMap<Node, List<FlowEntryInstall>> nodeFlows;
    private ConcurrentMap<String, List<FlowEntryInstall>> groupFlows;

    /*
     * Inactive flow list. This is for the global instance of FRM It will
     * contain all the flow entries which were installed on the global container
     * when the first container is created.
     */
    private ConcurrentMap<FlowEntry, FlowEntry> inactiveFlows;

    private IContainer container;
    private Set<IForwardingRulesManagerAware> frmAware =
        Collections.synchronizedSet(new HashSet<IForwardingRulesManagerAware>());
    private PortGroupProvider portGroupProvider;
    private IFlowProgrammerService programmer;
    private IClusterContainerServices clusterContainerService = null;
    private ISwitchManager switchManager;
    private Thread frmEventHandler;
    protected BlockingQueue<FRMEvent> pendingEvents;

    // Distributes FRM programming in the cluster
    private IConnectionManager connectionManager;

    /*
     * Name clustered caches used to support FRM entry distribution these are by
     * necessity non-transactional as long as need to be able to synchronize
     * states also while a transaction is in progress
     */
    static final String WORK_ORDER_CACHE = "frm.workOrder";
    static final String WORK_STATUS_CACHE = "frm.workStatus";
    static final String ORIGINAL_SW_VIEW_CACHE = "frm.originalSwView";
    static final String INSTALLED_SW_VIEW_CACHE = "frm.installedSwView";

    /*
     * Data structure responsible for distributing the FlowEntryInstall requests
     * in the cluster. The key value is entry that is being either Installed or
     * Updated or Delete. The value field is the same of the key value in case
     * of Installation or Deletion, it's the new entry in case of Modification,
     * this because the clustering caches don't allow null values.
     *
     * The logic behind this data structure is that the controller that initiate
     * the request will place the order here, someone will pick it and then will
     * remove from this data structure because is being served.
     *
     * TODO: We need to have a way to cleanup this data structure if entries are
     * not picked by anyone, which is always a case can happen especially on
     * Node disconnect cases.
     */
    protected ConcurrentMap<FlowEntryDistributionOrder, FlowEntryInstall> workOrder;

    /*
     * Data structure responsible for retrieving the results of the workOrder
     * submitted to the cluster.
     *
     * The logic behind this data structure is that the controller that has
     * executed the order will then place the result in workStatus signaling
     * that there was a success or a failure.
     *
     * TODO: The workStatus entries need to have a lifetime associated in case
     * of requestor controller leaving the cluster.
     */
    protected ConcurrentMap<FlowEntryDistributionOrder, Status> workStatus;

    /*
     * Local Map used to hold the Future which a caller can use to monitor for
     * completion
     */
    private ConcurrentMap<FlowEntryDistributionOrder, FlowEntryDistributionOrderFutureTask> workMonitor =
            new ConcurrentHashMap<FlowEntryDistributionOrder, FlowEntryDistributionOrderFutureTask>();

    /*
     * Max pool size for the executor
     */
    private static final int maxPoolSize = 10;

    /**
     * @param e
     *            Entry being installed/updated/removed
     * @param u
     *            New entry will be placed after the update operation. Valid
     *            only for UpdateType.CHANGED, null for all the other cases
     * @param t
     *            Type of update
     * @return a Future object for monitoring the progress of the result, or
     *         null in case the processing should take place locally
     */
    private FlowEntryDistributionOrderFutureTask distributeWorkOrder(FlowEntryInstall e, FlowEntryInstall u,
            UpdateType t) {
        // A null entry it's an unexpected condition, anyway it's safe to keep
        // the handling local
        if (e == null) {
            return null;
        }

        Node n = e.getNode();
        if (connectionManager.getLocalityStatus(n) == ConnectionLocality.NOT_LOCAL) {
            // Create the work order and distribute it
            FlowEntryDistributionOrder fe =
                    new FlowEntryDistributionOrder(e, t, clusterContainerService.getMyAddress());
            // First create the monitor job
            FlowEntryDistributionOrderFutureTask ret = new FlowEntryDistributionOrderFutureTask(fe);
            logsync.trace("Node {} not local so sending fe {}", n, fe);
            workMonitor.put(fe, ret);
            if (t.equals(UpdateType.CHANGED)) {
                // Then distribute the work
                workOrder.put(fe, u);
            } else {
                // Then distribute the work
                workOrder.put(fe, e);
            }
            logsync.trace("WorkOrder requested");
            // Now create an Handle to monitor the execution of the operation
            return ret;
        }

        logsync.trace("Node {} could be local. so processing Entry:{} UpdateType:{}", n, e, t);
        return null;
    }

    /**
     * Checks if the FlowEntry targets are valid for this container
     *
     * @param flowEntry
     *            The flow entry to test
     * @return a Status object representing the result of the validation
     */
    private Status validateEntry(FlowEntry flowEntry) {
        // Node presence check
        Node node = flowEntry.getNode();
        if (!switchManager.getNodes().contains(node)) {
            return new Status(StatusCode.BADREQUEST, String.format("Node %s is not present in this container", node));
        }

        // Ports and actions validation check
        Flow flow = flowEntry.getFlow();
        Match match = flow.getMatch();
        if (match.isPresent(MatchType.IN_PORT)) {
            NodeConnector inputPort = (NodeConnector)match.getField(MatchType.IN_PORT).getValue();
            if (!switchManager.getNodeConnectors(node).contains(inputPort)) {
                String msg = String.format("Ingress port %s is not present on this container", inputPort);
                return new Status(StatusCode.BADREQUEST, msg);
            }
        }
        for (Action action : flow.getActions()) {
            if (action instanceof Flood && !GlobalConstants.DEFAULT.toString().equals(getContainerName())) {
                return new Status(StatusCode.BADREQUEST, String.format("Flood is only allowed in default container"));
            }
            if (action instanceof FloodAll && !GlobalConstants.DEFAULT.toString().equals(getContainerName())) {
                return new Status(StatusCode.BADREQUEST, String.format("FloodAll is only allowed in default container"));
            }
            if (action instanceof Output) {
                Output out = (Output)action;
                NodeConnector outputPort = out.getPort();
                if (!switchManager.getNodeConnectors(node).contains(outputPort)) {
                    String msg = String.format("Output port %s is not present on this container", outputPort);
                    return new Status(StatusCode.BADREQUEST, msg);
                }
            }
            if (action instanceof Enqueue) {
                Enqueue out = (Enqueue)action;
                NodeConnector outputPort = out.getPort();
                if (!switchManager.getNodeConnectors(node).contains(outputPort)) {
                    String msg = String.format("Enqueue port %s is not present on this container", outputPort);
                    return new Status(StatusCode.BADREQUEST, msg);
                }
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Adds a flow entry onto the network node It runs various validity checks
     * and derive the final container flows merged entries that will be
     * attempted to be installed
     *
     * @param flowEntry
     *            the original flow entry application requested to add
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status addEntry(FlowEntry flowEntry, boolean async) {

        // Sanity Check
        if (flowEntry == null || flowEntry.getNode() == null || flowEntry.getFlow() == null) {
            String logMsg = INVALID_FLOW_ENTRY + ": {}";
            log.warn(logMsg, flowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, INVALID_FLOW_ENTRY);
        }

        // Operational check: input, output and queue ports presence check and
        // action validation for this container
        Status status = validateEntry(flowEntry);
        if (!status.isSuccess()) {
            String msg = String.format("%s: %s", INVALID_FLOW_ENTRY, status.getDescription());
            log.warn("{}: {}", msg, flowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        /*
         * Redundant Check: Check if the request is a redundant one from the
         * same application the flowEntry is equal to an existing one. Given we
         * do not have an application signature in the requested FlowEntry yet,
         * we are here detecting the above condition by comparing the flow
         * names, if set. If they are equal to the installed flow, most likely
         * this is a redundant installation request from the same application
         * and we can silently return success
         *
         * TODO: in future a sort of application reference list mechanism will
         * be added to the FlowEntry so that exact flow can be used by different
         * applications.
         */
        FlowEntry present = this.originalSwView.get(flowEntry);
        if (present != null) {
            boolean sameFlow = present.getFlow().equals(flowEntry.getFlow());
            boolean sameApp = present.getFlowName() != null && present.getFlowName().equals(flowEntry.getFlowName());
            if (sameFlow && sameApp) {
                log.trace("Skipping redundant request for flow {} on node {}", flowEntry.getFlowName(),
                        flowEntry.getNode());
                return new Status(StatusCode.SUCCESS, "Entry is already present");
            }
        }

        /*
         * Derive the container flow merged entries to install In presence of N
         * container flows, we may end up with N different entries to install...
         */
        List<FlowEntryInstall> toInstallList = deriveInstallEntries(flowEntry.clone(), container.getContainerFlows());

        // Container Flow conflict Check
        if (toInstallList.isEmpty()) {
            String msg = "Flow Entry conflicts with all Container Flows";
            String logMsg = msg + ": {}";
            log.warn(logMsg, flowEntry);
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Derive the list of entries good to be installed
        List<FlowEntryInstall> toInstallSafe = new ArrayList<FlowEntryInstall>();
        for (FlowEntryInstall entry : toInstallList) {
            // Conflict Check: Verify new entry would not overwrite existing
            // ones
            if (this.installedSwView.containsKey(entry)) {
                log.warn("Operation Rejected: A flow with same match and priority exists on the target node");
                log.trace("Aborting to install {}", entry);
                continue;
            }
            toInstallSafe.add(entry);
        }

        // Declare failure if all the container flow merged entries clash with
        // existing entries
        if (toInstallSafe.size() == 0) {
            String msg = "A flow with same match and priority exists on the target node";
            String logMsg = msg + ": {}";
            log.warn(logMsg, flowEntry);
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Try to install an entry at the time
        Status error = new Status(null, null);
        Status succeded = null;
        boolean oneSucceded = false;
        for (FlowEntryInstall installEntry : toInstallSafe) {

            // Install and update database
            Status ret = addEntryInternal(installEntry, async);

            if (ret.isSuccess()) {
                oneSucceded = true;
                /*
                 * The first successful status response will be returned For the
                 * asynchronous call, we can discard the container flow
                 * complication for now and assume we will always deal with one
                 * flow only per request
                 */
                succeded = ret;
            } else {
                error = ret;
                log.trace("Failed to install the entry: {}. The failure is: {}", installEntry, ret.getDescription());
            }
        }

        return (oneSucceded) ? succeded : error;
    }

    /**
     * Given a flow entry and the list of container flows, it returns the list
     * of container flow merged flow entries good to be installed on this
     * container. If the list of container flows is null or empty, the install
     * entry list will contain only one entry, the original flow entry. If the
     * flow entry is congruent with all the N container flows, then the output
     * install entry list will contain N entries. If the output list is empty,
     * it means the passed flow entry conflicts with all the container flows.
     *
     * @param cFlowList
     *            The list of container flows
     * @return the list of container flow merged entries good to be installed on
     *         this container
     */
    private List<FlowEntryInstall> deriveInstallEntries(FlowEntry request, List<ContainerFlow> cFlowList) {
        List<FlowEntryInstall> toInstallList = new ArrayList<FlowEntryInstall>(1);

        if (container.getContainerFlows() == null || container.getContainerFlows().isEmpty()) {
            // No container flows => entry good to be installed unchanged
            toInstallList.add(new FlowEntryInstall(request.clone(), null));
        } else {
            // Create the list of entries to be installed. If the flow entry is
            // not congruent with any container flow, no install entries will be
            // created
            for (ContainerFlow cFlow : container.getContainerFlows()) {
                if (cFlow.allowsFlow(request.getFlow())) {
                    toInstallList.add(new FlowEntryInstall(request.clone(), cFlow));
                }
            }
        }
        return toInstallList;
    }

    /**
     * Modify a flow entry with a new one It runs various validity check and
     * derive the final container flows merged flow entries to work with
     *
     * @param currentFlowEntry
     * @param newFlowEntry
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status modifyEntry(FlowEntry currentFlowEntry, FlowEntry newFlowEntry, boolean async) {
        Status retExt;

        // Sanity checks
        if (currentFlowEntry == null || currentFlowEntry.getNode() == null || newFlowEntry == null
                || newFlowEntry.getNode() == null || newFlowEntry.getFlow() == null) {
            String msg = "Modify: " + INVALID_FLOW_ENTRY;
            String logMsg = msg + ": {} or {}";
            log.warn(logMsg, currentFlowEntry, newFlowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }
        if (!currentFlowEntry.getNode().equals(newFlowEntry.getNode())
                || !currentFlowEntry.getFlowName().equals(newFlowEntry.getFlowName())) {
            String msg = "Modify: Incompatible Flow Entries";
            String logMsg = msg + ": {} and {}";
            log.warn(logMsg, currentFlowEntry, newFlowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        // Equality Check
        if (currentFlowEntry.getFlow().equals(newFlowEntry.getFlow())) {
            String msg = "Modify skipped as flows are the same";
            String logMsg = msg + ": {} and {}";
            log.debug(logMsg, currentFlowEntry, newFlowEntry);
            return new Status(StatusCode.SUCCESS, msg);
        }

        // Operational check: input, output and queue ports presence check and
        // action validation for this container
        Status status = validateEntry(newFlowEntry);
        if (!status.isSuccess()) {
            String msg = String.format("Modify: %s: %s", INVALID_FLOW_ENTRY, status.getDescription());
            log.warn("{}: {}", msg, newFlowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        /*
         * Conflict Check: Verify the new entry would not conflict with an
         * existing one. This is a loose check on the previous original flow
         * entry requests. No check on the container flow merged flow entries
         * (if any) yet
         */
        FlowEntry sameMatchOriginalEntry = originalSwView.get(newFlowEntry);
        if (sameMatchOriginalEntry != null && !sameMatchOriginalEntry.equals(currentFlowEntry)) {
            String msg = "Operation Rejected: Another flow with same match and priority exists on the target node";
            String logMsg = msg + ": {}";
            log.warn(logMsg, currentFlowEntry);
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Derive the installed and toInstall entries
        List<FlowEntryInstall> installedList = deriveInstallEntries(currentFlowEntry.clone(),
                container.getContainerFlows());
        List<FlowEntryInstall> toInstallList = deriveInstallEntries(newFlowEntry.clone(), container.getContainerFlows());

        if (toInstallList.isEmpty()) {
            String msg = "Modify Operation Rejected: The new entry conflicts with all the container flows";
            String logMsg = msg + ": {}";
            log.warn(logMsg, newFlowEntry);
            log.warn(msg);
            return new Status(StatusCode.CONFLICT, msg);
        }

        /*
         * If the two list sizes differ, it means the new flow entry does not
         * satisfy the same number of container flows the current entry does.
         * This is only possible when the new entry and current entry have
         * different match. In this scenario the modification would ultimately
         * be handled as a remove and add operations in the protocol plugin.
         *
         * Also, if any of the new flow entries would clash with an existing
         * one, we cannot proceed with the modify operation, because it would
         * fail for some entries and leave stale entries on the network node.
         * Modify path can be taken only if it can be performed completely, for
         * all entries.
         *
         * So, for the above two cases, to simplify, let's decouple the modify
         * in: 1) remove current entries 2) install new entries
         */
        Status succeeded = null;
        boolean decouple = false;
        if (installedList.size() != toInstallList.size()) {
            log.trace("Modify: New flow entry does not satisfy the same "
                    + "number of container flows as the original entry does");
            decouple = true;
        }
        List<FlowEntryInstall> toInstallSafe = new ArrayList<FlowEntryInstall>();
        for (FlowEntryInstall installEntry : toInstallList) {
            /*
             * Conflict Check: Verify the new entry would not overwrite another
             * existing one
             */
            FlowEntryInstall sameMatchEntry = installedSwView.get(installEntry);
            if (sameMatchEntry != null && !sameMatchEntry.getOriginal().equals(currentFlowEntry)) {
                log.trace("Modify: new container flow merged flow entry clashes with existing flow");
                decouple = true;
            } else {
                toInstallSafe.add(installEntry);
            }
        }

        if (decouple) {
            // Remove current entries
            for (FlowEntryInstall currEntry : installedList) {
                this.removeEntryInternal(currEntry, async);
            }
            // Install new entries
            for (FlowEntryInstall newEntry : toInstallSafe) {
                succeeded = this.addEntryInternal(newEntry, async);
            }
        } else {
            /*
             * The two list have the same size and the entries to install do not
             * clash with any existing flow on the network node. We assume here
             * (and might be wrong) that the same container flows that were
             * satisfied by the current entries are the same that are satisfied
             * by the new entries. Let's take the risk for now.
             *
             * Note: modification has to be complete. If any entry modification
             * fails, we need to stop, restore the already modified entries, and
             * declare failure.
             */
            Status retModify = null;
            int i = 0;
            int size = toInstallList.size();
            while (i < size) {
                // Modify and update database
                retModify = modifyEntryInternal(installedList.get(i), toInstallList.get(i), async);
                if (retModify.isSuccess()) {
                    i++;
                } else {
                    break;
                }
            }
            // Check if uncompleted modify
            if (i < size) {
                log.warn("Unable to perform a complete modify for all  the container flows merged entries");
                // Restore original entries
                int j = 0;
                while (j < i) {
                    log.info("Attempting to restore initial entries");
                    retExt = modifyEntryInternal(toInstallList.get(i), installedList.get(i), async);
                    if (retExt.isSuccess()) {
                        j++;
                    } else {
                        break;
                    }
                }
                // Fatal error, recovery failed
                if (j < i) {
                    String msg = "Flow recovery failed ! Unrecoverable Error";
                    log.error(msg);
                    return new Status(StatusCode.INTERNALERROR, msg);
                }
            }
            succeeded = retModify;
        }
        /*
         * The first successful status response will be returned. For the
         * asynchronous call, we can discard the container flow complication for
         * now and assume we will always deal with one flow only per request
         */
        return succeeded;
    }

    /**
     * This is the function that modifies the final container flows merged
     * entries on the network node and update the database. It expects that all
     * the validity checks are passed.
     * This function is supposed to be called only on the controller on which
     * the IFRM call is executed.
     *
     * @param currentEntries
     * @param newEntries
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status modifyEntryInternal(FlowEntryInstall currentEntries, FlowEntryInstall newEntries, boolean async) {
        Status status = new Status(StatusCode.UNDEFINED);
        FlowEntryDistributionOrderFutureTask futureStatus =
                distributeWorkOrder(currentEntries, newEntries, UpdateType.CHANGED);
        if (futureStatus != null) {
            try {
                status = futureStatus.get();
                if (status.getCode()
                        .equals(StatusCode.TIMEOUT)) {
                    // A timeout happened, lets cleanup the workMonitor
                    workMonitor.remove(futureStatus.getOrder());
                }
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
        } else {
            // Modify the flow on the network node
            status = modifyEntryInHw(currentEntries, newEntries, async);
        }

        if (!status.isSuccess()) {
            log.trace("{} SDN Plugin failed to program the flow: {}. The failure is: {}",
                    (futureStatus != null) ? "Remote" : "Local", newEntries.getInstall(), status.getDescription());
            return status;
        }

        log.trace("Modified {} => {}", currentEntries.getInstall(), newEntries.getInstall());

        // Update DB
        newEntries.setRequestId(status.getRequestId());
        updateSwViews(currentEntries, false);
        updateSwViews(newEntries, true);

        return status;
    }

    private Status modifyEntryInHw(FlowEntryInstall currentEntries, FlowEntryInstall newEntries, boolean async) {
        return async ? programmer.modifyFlowAsync(currentEntries.getNode(), currentEntries.getInstall().getFlow(),
                newEntries.getInstall().getFlow()) : programmer.modifyFlow(currentEntries.getNode(), currentEntries
                .getInstall().getFlow(), newEntries.getInstall().getFlow());
    }

    /**
     * Remove a flow entry. If the entry is not present in the software view
     * (entry or node not present), it return successfully
     *
     * @param flowEntry
     *            the flow entry to remove
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status removeEntry(FlowEntry flowEntry, boolean async) {
        Status error = new Status(null, null);

        // Sanity Check
        if (flowEntry == null || flowEntry.getNode() == null || flowEntry.getFlow() == null) {
            String logMsg = INVALID_FLOW_ENTRY + ": {}";
            log.warn(logMsg, flowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, INVALID_FLOW_ENTRY);
        }

        // Derive the container flows merged installed entries
        List<FlowEntryInstall> installedList = deriveInstallEntries(flowEntry.clone(), container.getContainerFlows());

        Status succeeded = null;
        boolean atLeastOneRemoved = false;
        for (FlowEntryInstall entry : installedList) {
            if (!installedSwView.containsKey(entry)) {
                String logMsg = "Removal skipped (not present in software view) for flow entry: {}";
                log.debug(logMsg, flowEntry);
                if (installedList.size() == 1) {
                    // If we had only one entry to remove, we are done
                    return new Status(StatusCode.SUCCESS);
                } else {
                    continue;
                }
            }

            // Remove and update DB
            Status ret = removeEntryInternal(entry, async);

            if (!ret.isSuccess()) {
                error = ret;
                log.trace("Failed to remove the entry: {}. The failure is: {}", entry.getInstall(), ret.getDescription());
                if (installedList.size() == 1) {
                    // If we had only one entry to remove, this is fatal failure
                    return error;
                }
            } else {
                succeeded = ret;
                atLeastOneRemoved = true;
            }
        }

        /*
         * No worries if full removal failed. Consistency checker will take care
         * of removing the stale entries later, or adjusting the software
         * database if not in sync with hardware
         */
        return (atLeastOneRemoved) ? succeeded : error;
    }

    /**
     * This is the function that removes the final container flows merged entry
     * from the network node and update the database. It expects that all the
     * validity checks are passed
     * This function is supposed to be called only on the controller on which
     * the IFRM call is executed.
     *
     * @param entry
     *            the flow entry to remove
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status removeEntryInternal(FlowEntryInstall entry, boolean async) {
        Status status = new Status(StatusCode.UNDEFINED);
        FlowEntryDistributionOrderFutureTask futureStatus = distributeWorkOrder(entry, null, UpdateType.REMOVED);
        if (futureStatus != null) {
            try {
                status = futureStatus.get();
                if (status.getCode().equals(StatusCode.TIMEOUT)) {
                    // A timeout happened, lets cleanup the workMonitor
                    workMonitor.remove(futureStatus.getOrder());
                }
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
        } else {
            // Mark the entry to be deleted (for CC just in case we fail)
            entry.toBeDeleted();

            // Remove from node
            status = removeEntryInHw(entry, async);
        }

        if (!status.isSuccess()) {
            log.trace("{} SDN Plugin failed to remove the flow: {}. The failure is: {}",
                    (futureStatus != null) ? "Remote" : "Local", entry.getInstall(), status.getDescription());
            return status;
        }

        log.trace("Removed  {}", entry.getInstall());

        // Update DB
        updateSwViews(entry, false);

        return status;
    }

    private Status removeEntryInHw(FlowEntryInstall entry, boolean async) {
        return async ? programmer.removeFlowAsync(entry.getNode(), entry.getInstall().getFlow()) : programmer
                .removeFlow(entry.getNode(), entry.getInstall().getFlow());
    }

    /**
     * This is the function that installs the final container flow merged entry
     * on the network node and updates the database. It expects that all the
     * validity and conflict checks are passed. That means it does not check
     * whether this flow would conflict or overwrite an existing one.
     * This function is supposed to be called only on the controller on which
     * the IFRM call is executed.
     *
     * @param entry
     *            the flow entry to install
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status addEntryInternal(FlowEntryInstall entry, boolean async) {
        Status status = new Status(StatusCode.UNDEFINED);
        FlowEntryDistributionOrderFutureTask futureStatus = distributeWorkOrder(entry, null, UpdateType.ADDED);
        if (futureStatus != null) {
            try {
                status = futureStatus.get();
                if (status.getCode().equals(StatusCode.TIMEOUT)) {
                    // A timeout happened, lets cleanup the workMonitor
                    workMonitor.remove(futureStatus.getOrder());
                }
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
        } else {
            status = addEntryInHw(entry, async);
        }

        if (!status.isSuccess()) {
            log.trace("{} SDN Plugin failed to program the flow: {}. The failure is: {}",
                    (futureStatus != null) ? "Remote" : "Local", entry.getInstall(), status.getDescription());
            return status;
        }

        log.trace("Added    {}", entry.getInstall());

        // Update DB
        entry.setRequestId(status.getRequestId());
        updateSwViews(entry, true);

        return status;
    }

    private Status addEntryInHw(FlowEntryInstall entry, boolean async) {
        // Install the flow on the network node
        return async ? programmer.addFlowAsync(entry.getNode(), entry.getInstall().getFlow()) : programmer.addFlow(
                entry.getNode(), entry.getInstall().getFlow());
    }

    /**
     * Returns true if the flow conflicts with all the container's flows. This
     * means that if the function returns true, the passed flow entry is
     * congruent with at least one container flow, hence it is good to be
     * installed on this container.
     *
     * @param flowEntry
     * @return true if flow conflicts with all the container flows, false
     *         otherwise
     */
    private boolean entryConflictsWithContainerFlows(FlowEntry flowEntry) {
        List<ContainerFlow> cFlowList = container.getContainerFlows();

        // Validity check and avoid unnecessary computation
        // Also takes care of default container where no container flows are
        // present
        if (cFlowList == null || cFlowList.isEmpty()) {
            return false;
        }

        for (ContainerFlow cFlow : cFlowList) {
            if (cFlow.allowsFlow(flowEntry.getFlow())) {
                // Entry is allowed by at least one container flow: good to go
                return false;
            }
        }
        return true;
    }

    private ConcurrentMap.Entry<Integer, FlowConfig> getStaticFlowEntry(String name, Node node) {
        for (ConcurrentMap.Entry<Integer, FlowConfig> flowEntry : staticFlows.entrySet()) {
            FlowConfig flowConfig = flowEntry.getValue();
            if (flowConfig.isByNameAndNodeIdEqual(name, node)) {
                return flowEntry;
            }
        }
        return null;
    }

    private void updateIndexDatabase(FlowEntryInstall entry, boolean add) {
        // Update node indexed flow database
        updateNodeFlowsDB(entry, add);

        // Update group indexed flow database
        updateGroupFlowsDB(entry, add);
    }

    /*
     * Update the node mapped flows database
     */
    private void updateSwViews(FlowEntryInstall flowEntries, boolean add) {
        if (add) {
            originalSwView.put(flowEntries.getOriginal(), flowEntries.getOriginal());
            installedSwView.put(flowEntries, flowEntries);
        } else {
            originalSwView.remove(flowEntries.getOriginal());
            installedSwView.remove(flowEntries);
        }
    }

    /*
     * Update the node mapped flows database
     */
    private void updateNodeFlowsDB(FlowEntryInstall flowEntries, boolean add) {
        Node node = flowEntries.getNode();

        List<FlowEntryInstall> nodeIndeces = this.nodeFlows.get(node);
        if (nodeIndeces == null) {
            if (!add) {
                return;
            } else {
                nodeIndeces = new ArrayList<FlowEntryInstall>();
            }
        }

        if (add) {
            // there may be an already existing entry.
            // remove it before adding the new one.
            // This is necessary since we have observed that in some cases
            // Infinispan does aggregation for operations (eg:- remove and then put a different value)
            // related to the same key within the same transaction.
            // Need this defensive code as the new FlowEntryInstall may be different
            // than the old one even though the equals method returns true. This is because
            // the equals method does not take into account the action list.
            if(nodeIndeces.contains(flowEntries)) {
                nodeIndeces.remove(flowEntries);
            }
            nodeIndeces.add(flowEntries);
        } else {
            nodeIndeces.remove(flowEntries);
        }

        // Update cache across cluster
        if (nodeIndeces.isEmpty()) {
            this.nodeFlows.remove(node);
        } else {
            this.nodeFlows.put(node, nodeIndeces);
        }
    }

    /*
     * Update the group name mapped flows database
     */
    private void updateGroupFlowsDB(FlowEntryInstall flowEntries, boolean add) {
        String groupName = flowEntries.getGroupName();

        // Flow may not be part of a group
        if (groupName == null) {
            return;
        }

        List<FlowEntryInstall> indices = this.groupFlows.get(groupName);
        if (indices == null) {
            if (!add) {
                return;
            } else {
                indices = new ArrayList<FlowEntryInstall>();
            }
        }

        if (add) {
            // same comments in the similar code section in
            // updateNodeFlowsDB method apply here too
            if(indices.contains(flowEntries)) {
                indices.remove(flowEntries);
            }
            indices.add(flowEntries);
        } else {
            indices.remove(flowEntries);
        }

        // Update cache across cluster
        if (indices.isEmpty()) {
            this.groupFlows.remove(groupName);
        } else {
            this.groupFlows.put(groupName, indices);
        }
    }

    /**
     * Remove a flow entry that has been added previously First checks if the
     * entry is effectively present in the local database
     */
    @SuppressWarnings("unused")
    private Status removeEntry(Node node, String flowName) {
        FlowEntryInstall target = null;

        // Find in database
        for (FlowEntryInstall entry : installedSwView.values()) {
            if (entry.equalsByNodeAndName(node, flowName)) {
                target = entry;
                break;
            }
        }

        // If it is not there, stop any further processing
        if (target == null) {
            return new Status(StatusCode.SUCCESS, "Entry is not present");
        }

        // Remove from node
        Status status = programmer.removeFlow(target.getNode(), target.getInstall().getFlow());

        // Update DB
        if (status.isSuccess()) {
            updateSwViews(target, false);
        } else {
            // log the error
            log.trace("SDN Plugin failed to remove the flow: {}. The failure is: {}", target.getInstall(),
                    status.getDescription());
        }

        return status;
    }

    @Override
    public Status installFlowEntry(FlowEntry flowEntry) {
        Status status;
        if (isContainerModeAllowed(flowEntry)) {
            status = addEntry(flowEntry, false);
        } else {
            String msg = "Controller in container mode: Install Refused";
            String logMsg = msg + ": {}";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(logMsg, flowEntry);
        }
        return status;
    }

    @Override
    public Status installFlowEntryAsync(FlowEntry flowEntry) {
        Status status;
        if (isContainerModeAllowed(flowEntry)) {
            status = addEntry(flowEntry, true);
        } else {
            String msg = "Controller in container mode: Install Refused";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(msg);
        }
        return status;
    }

    @Override
    public Status uninstallFlowEntry(FlowEntry flowEntry) {
        Status status;
        if (isContainerModeAllowed(flowEntry)) {
            status = removeEntry(flowEntry, false);
        } else {
            String msg = "Controller in container mode: Uninstall Refused";
            String logMsg = msg + ": {}";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(logMsg, flowEntry);
        }
        return status;
    }

    @Override
    public Status uninstallFlowEntryAsync(FlowEntry flowEntry) {
        Status status;
        if (isContainerModeAllowed(flowEntry)) {
            status = removeEntry(flowEntry, true);
        } else {
            String msg = "Controller in container mode: Uninstall Refused";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(msg);
        }
        return status;
    }

    @Override
    public Status modifyFlowEntry(FlowEntry currentFlowEntry, FlowEntry newFlowEntry) {
        Status status = null;
        if (isContainerModeAllowed(currentFlowEntry)) {
            status = modifyEntry(currentFlowEntry, newFlowEntry, false);
        } else {
            String msg = "Controller in container mode: Modify Refused";
            String logMsg = msg + ": {}";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(logMsg, newFlowEntry);
        }
        return status;
    }

    @Override
    public Status modifyFlowEntryAsync(FlowEntry currentFlowEntry, FlowEntry newFlowEntry) {
        Status status = null;
        if (isContainerModeAllowed(currentFlowEntry)) {
            status = modifyEntry(currentFlowEntry, newFlowEntry, true);
        } else {
            String msg = "Controller in container mode: Modify Refused";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(msg);
        }
        return status;
    }

    /**
     * Returns whether the specified flow entry is allowed to be
     * installed/removed/modified based on the current container mode status.
     * This call always returns true in the container instance of forwarding
     * rules manager. It is meant for the global instance only (default
     * container) of forwarding rules manager. Idea is that for assuring
     * container isolation of traffic, flow installation in default container is
     * blocked when in container mode (containers are present). The only flows
     * that are allowed in container mode in the default container are the
     * proactive flows, the ones automatically installed on the network node
     * which forwarding mode has been configured to "proactive". These flows are
     * needed by controller to discover the nodes topology and to discover the
     * attached hosts for some SDN switches.
     *
     * @param flowEntry
     *            The flow entry to be installed/removed/modified
     * @return true if not in container mode or if flowEntry is internally
     *         generated
     */
    private boolean isContainerModeAllowed(FlowEntry flowEntry) {
        return (!inContainerMode) ? true : flowEntry.isInternal();
    }

    @Override
    public Status modifyOrAddFlowEntry(FlowEntry newFlowEntry) {
        /*
         * Run a check on the original entries to decide whether to go with a
         * add or modify method. A loose check means only check against the
         * original flow entry requests and not against the installed flow
         * entries which are the result of the original entry merged with the
         * container flow(s) (if any). The modifyFlowEntry method in presence of
         * conflicts with the Container flows (if any) would revert back to a
         * delete + add pattern
         */
        FlowEntry currentFlowEntry = originalSwView.get(newFlowEntry);

        if (currentFlowEntry != null) {
            return modifyFlowEntry(currentFlowEntry, newFlowEntry);
        } else {
            return installFlowEntry(newFlowEntry);
        }
    }

    @Override
    public Status modifyOrAddFlowEntryAsync(FlowEntry newFlowEntry) {
        /*
         * Run a check on the original entries to decide whether to go with a
         * add or modify method. A loose check means only check against the
         * original flow entry requests and not against the installed flow
         * entries which are the result of the original entry merged with the
         * container flow(s) (if any). The modifyFlowEntry method in presence of
         * conflicts with the Container flows (if any) would revert back to a
         * delete + add pattern
         */
        FlowEntry currentFlowEntry = originalSwView.get(newFlowEntry);

        if (currentFlowEntry != null) {
            return modifyFlowEntryAsync(currentFlowEntry, newFlowEntry);
        } else {
            return installFlowEntryAsync(newFlowEntry);
        }
    }

    @Override
    public Status uninstallFlowEntryGroup(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid group name");
        }
        if (groupName.equals(FlowConfig.INTERNALSTATICFLOWGROUP)) {
            return new Status(StatusCode.BADREQUEST, "Internal static flows group cannot be deleted through this api");
        }
        if (inContainerMode) {
            String msg = "Controller in container mode: Group Uninstall Refused";
            String logMsg = msg + ": {}";
            log.warn(logMsg, groupName);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }
        int toBeRemoved = 0;
        String error = "";
        if (groupFlows.containsKey(groupName)) {
            List<FlowEntryInstall> list = new ArrayList<FlowEntryInstall>(groupFlows.get(groupName));
            toBeRemoved = list.size();
            for (FlowEntryInstall entry : list) {
                // since this is the entry that was stored in groupFlows
                // it is already validated and merged
                // so can call removeEntryInternal directly
                Status status = this.removeEntryInternal(entry, false);
                if (status.isSuccess()) {
                    toBeRemoved -= 1;
                } else {
                    error = status.getDescription();
                }
            }
        }
        return (toBeRemoved == 0) ? new Status(StatusCode.SUCCESS) : new Status(StatusCode.INTERNALERROR,
                "Not all the flows were removed: " + error);
    }

    @Override
    public Status uninstallFlowEntryGroupAsync(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid group name");
        }
        if (groupName.equals(FlowConfig.INTERNALSTATICFLOWGROUP)) {
            return new Status(StatusCode.BADREQUEST, "Static flows group cannot be deleted through this api");
        }
        if (inContainerMode) {
            String msg = "Controller in container mode: Group Uninstall Refused";
            String logMsg = msg + ": {}";
            log.warn(logMsg, groupName);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }
        if (groupFlows.containsKey(groupName)) {
            List<FlowEntryInstall> list = new ArrayList<FlowEntryInstall>(groupFlows.get(groupName));
            for (FlowEntryInstall entry : list) {
                this.removeEntry(entry.getOriginal(), true);
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public boolean checkFlowEntryConflict(FlowEntry flowEntry) {
        return entryConflictsWithContainerFlows(flowEntry);
    }

    /**
     * Updates all installed flows because the container flow got updated This
     * is obtained in two phases on per node basis: 1) Uninstall of all flows 2)
     * Reinstall of all flows This is needed because a new container flows
     * merged flow may conflict with an existing old container flows merged flow
     * on the network node
     */
    protected void updateFlowsContainerFlow() {
        Set<FlowEntry> toReInstall = new HashSet<FlowEntry>();
        // First remove all installed entries
        for (ConcurrentMap.Entry<FlowEntryInstall, FlowEntryInstall> entry : installedSwView.entrySet()) {
            FlowEntryInstall current = entry.getValue();
            // Store the original entry
            toReInstall.add(current.getOriginal());
            // Remove the old couples. No validity checks to be run, use the
            // internal remove
            this.removeEntryInternal(current, false);
        }
        // Then reinstall the original entries
        for (FlowEntry entry : toReInstall) {
            // Reinstall the original flow entries, via the regular path: new
            // cFlow merge + validations
            this.installFlowEntry(entry);
        }
    }

    private void nonClusterObjectCreate() {
        originalSwView = new ConcurrentHashMap<FlowEntry, FlowEntry>();
        installedSwView = new ConcurrentHashMap<FlowEntryInstall, FlowEntryInstall>();
        TSPolicies = new ConcurrentHashMap<String, Object>();
        staticFlowsOrdinal = new ConcurrentHashMap<Integer, Integer>();
        portGroupConfigs = new ConcurrentHashMap<String, PortGroupConfig>();
        portGroupData = new ConcurrentHashMap<PortGroupConfig, Map<Node, PortGroup>>();
        staticFlows = new ConcurrentHashMap<Integer, FlowConfig>();
        inactiveFlows = new ConcurrentHashMap<FlowEntry, FlowEntry>();
    }

    @Override
    public void setTSPolicyData(String policyname, Object o, boolean add) {

        if (add) {
            /* Check if this policy already exists */
            if (!(TSPolicies.containsKey(policyname))) {
                TSPolicies.put(policyname, o);
            }
        } else {
            TSPolicies.remove(policyname);
        }
        if (frmAware != null) {
            synchronized (frmAware) {
                for (IForwardingRulesManagerAware frma : frmAware) {
                    try {
                        frma.policyUpdate(policyname, add);
                    } catch (Exception e) {
                        log.warn("Exception on callback", e);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Object> getTSPolicyData() {
        return TSPolicies;
    }

    @Override
    public Object getTSPolicyData(String policyName) {
        if (TSPolicies.containsKey(policyName)) {
            return TSPolicies.get(policyName);
        } else {
            return null;
        }
    }

    @Override
    public List<FlowEntry> getFlowEntriesForGroup(String policyName) {
        List<FlowEntry> list = new ArrayList<FlowEntry>();
        if (policyName != null && !policyName.trim().isEmpty()) {
            for (Map.Entry<FlowEntry, FlowEntry> entry : this.originalSwView.entrySet()) {
                if (policyName.equals(entry.getKey().getGroupName())) {
                    list.add(entry.getValue().clone());
                }
            }
        }
        return list;
    }

    @Override
    public List<FlowEntry> getInstalledFlowEntriesForGroup(String policyName) {
        List<FlowEntry> list = new ArrayList<FlowEntry>();
        if (policyName != null && !policyName.trim().isEmpty()) {
            for (Map.Entry<FlowEntryInstall, FlowEntryInstall> entry : this.installedSwView.entrySet()) {
                if (policyName.equals(entry.getKey().getGroupName())) {
                    list.add(entry.getValue().getInstall().clone());
                }
            }
        }
        return list;
    }

    @Override
    public void addOutputPort(Node node, String flowName, List<NodeConnector> portList) {

        for (FlowEntryInstall flow : this.nodeFlows.get(node)) {
            if (flow.getFlowName().equals(flowName)) {
                FlowEntry currentFlowEntry = flow.getOriginal();
                FlowEntry newFlowEntry = currentFlowEntry.clone();
                for (NodeConnector dstPort : portList) {
                    newFlowEntry.getFlow().addAction(new Output(dstPort));
                }
                Status error = modifyEntry(currentFlowEntry, newFlowEntry, false);
                if (error.isSuccess()) {
                    log.trace("Ports {} added to FlowEntry {}", portList, flowName);
                } else {
                    log.warn("Failed to add ports {} to Flow entry {}. The failure is: {}", portList,
                            currentFlowEntry.toString(), error.getDescription());
                }
                return;
            }
        }
        log.warn("Failed to add ports to Flow {} on Node {}: Entry Not Found", flowName, node);
    }

    @Override
    public void removeOutputPort(Node node, String flowName, List<NodeConnector> portList) {
        for (FlowEntryInstall index : this.nodeFlows.get(node)) {
            FlowEntryInstall flow = this.installedSwView.get(index);
            if (flow.getFlowName().equals(flowName)) {
                FlowEntry currentFlowEntry = flow.getOriginal();
                FlowEntry newFlowEntry = currentFlowEntry.clone();
                for (NodeConnector dstPort : portList) {
                    Action action = new Output(dstPort);
                    newFlowEntry.getFlow().removeAction(action);
                }
                Status status = modifyEntry(currentFlowEntry, newFlowEntry, false);
                if (status.isSuccess()) {
                    log.trace("Ports {} removed from FlowEntry {}", portList, flowName);
                } else {
                    log.warn("Failed to remove ports {} from Flow entry {}. The failure is: {}", portList,
                            currentFlowEntry.toString(), status.getDescription());
                }
                return;
            }
        }
        log.warn("Failed to remove ports from Flow {} on Node {}: Entry Not Found", flowName, node);
    }

    /*
     * This function assumes the target flow has only one output port
     */
    @Override
    public void replaceOutputPort(Node node, String flowName, NodeConnector outPort) {
        FlowEntry currentFlowEntry = null;
        FlowEntry newFlowEntry = null;

        // Find the flow
        for (FlowEntryInstall index : this.nodeFlows.get(node)) {
            FlowEntryInstall flow = this.installedSwView.get(index);
            if (flow.getFlowName().equals(flowName)) {
                currentFlowEntry = flow.getOriginal();
                break;
            }
        }
        if (currentFlowEntry == null) {
            log.warn("Failed to replace output port for flow {} on node {}: Entry Not Found", flowName, node);
            return;
        }

        // Create a flow copy with the new output port
        newFlowEntry = currentFlowEntry.clone();
        Action target = null;
        for (Action action : newFlowEntry.getFlow().getActions()) {
            if (action.getType() == ActionType.OUTPUT) {
                target = action;
                break;
            }
        }
        newFlowEntry.getFlow().removeAction(target);
        newFlowEntry.getFlow().addAction(new Output(outPort));

        // Modify on network node
        Status status = modifyEntry(currentFlowEntry, newFlowEntry, false);

        if (status.isSuccess()) {
            log.trace("Output port replaced with {} for flow {} on node {}", outPort, flowName, node);
        } else {
            log.warn("Failed to replace output port for flow {} on node {}. The failure is: {}", flowName, node,
                    status.getDescription());
        }
        return;
    }

    @Override
    public NodeConnector getOutputPort(Node node, String flowName) {
        for (FlowEntryInstall index : this.nodeFlows.get(node)) {
            FlowEntryInstall flow = this.installedSwView.get(index);
            if (flow.getFlowName().equals(flowName)) {
                for (Action action : flow.getOriginal().getFlow().getActions()) {
                    if (action.getType() == ActionType.OUTPUT) {
                        return ((Output) action).getPort();
                    }
                }
            }
        }
        return null;
    }

    private void cacheStartup() {
        allocateCaches();
        retrieveCaches();
    }

    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            log.warn("Un-initialized clusterContainerService, can't create cache");
            return;
        }

        log.debug("Allocating caches for Container {}", container.getName());

        try {
            clusterContainerService.createCache(ORIGINAL_SW_VIEW_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache(INSTALLED_SW_VIEW_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.inactiveFlows",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.staticFlows",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.staticFlowsOrdinal",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.portGroupConfigs",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.portGroupData",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.TSPolicies",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache(WORK_STATUS_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL, IClusterServices.cacheMode.ASYNC));

            clusterContainerService.createCache(WORK_ORDER_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL, IClusterServices.cacheMode.ASYNC));

        } catch (CacheConfigException cce) {
            log.error("CacheConfigException");
        } catch (CacheExistException cce) {
            log.error("CacheExistException");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        ConcurrentMap<?, ?> map;

        if (this.clusterContainerService == null) {
            log.warn("un-initialized clusterContainerService, can't retrieve cache");
            nonClusterObjectCreate();
            return;
        }

        log.debug("Retrieving Caches for Container {}", container.getName());

        map = clusterContainerService.getCache(ORIGINAL_SW_VIEW_CACHE);
        if (map != null) {
            originalSwView = (ConcurrentMap<FlowEntry, FlowEntry>) map;
        } else {
            log.error("Retrieval of frm.originalSwView cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache(INSTALLED_SW_VIEW_CACHE);
        if (map != null) {
            installedSwView = (ConcurrentMap<FlowEntryInstall, FlowEntryInstall>) map;
        } else {
            log.error("Retrieval of frm.installedSwView cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.inactiveFlows");
        if (map != null) {
            inactiveFlows = (ConcurrentMap<FlowEntry, FlowEntry>) map;
        } else {
            log.error("Retrieval of frm.inactiveFlows cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.staticFlows");
        if (map != null) {
            staticFlows = (ConcurrentMap<Integer, FlowConfig>) map;
        } else {
            log.error("Retrieval of frm.staticFlows cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.staticFlowsOrdinal");
        if (map != null) {
            staticFlowsOrdinal = (ConcurrentMap<Integer, Integer>) map;
        } else {
            log.error("Retrieval of frm.staticFlowsOrdinal cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.portGroupConfigs");
        if (map != null) {
            portGroupConfigs = (ConcurrentMap<String, PortGroupConfig>) map;
        } else {
            log.error("Retrieval of frm.portGroupConfigs cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.portGroupData");
        if (map != null) {
            portGroupData = (ConcurrentMap<PortGroupConfig, Map<Node, PortGroup>>) map;
        } else {
            log.error("Retrieval of frm.portGroupData allocation failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.TSPolicies");
        if (map != null) {
            TSPolicies = (ConcurrentMap<String, Object>) map;
        } else {
            log.error("Retrieval of frm.TSPolicies cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache(WORK_ORDER_CACHE);
        if (map != null) {
            workOrder = (ConcurrentMap<FlowEntryDistributionOrder, FlowEntryInstall>) map;
        } else {
            log.error("Retrieval of " + WORK_ORDER_CACHE + " cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache(WORK_STATUS_CACHE);
        if (map != null) {
            workStatus = (ConcurrentMap<FlowEntryDistributionOrder, Status>) map;
        } else {
            log.error("Retrieval of " + WORK_STATUS_CACHE + " cache failed for Container {}", container.getName());
        }
    }

    private boolean flowConfigExists(FlowConfig config) {
        // Flow name has to be unique on per node id basis
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            if (entry.getValue().isByNameAndNodeIdEqual(config)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Status addStaticFlow(FlowConfig config) {
        return addStaticFlow(config, false);
    }

    private Status addStaticFlow(FlowConfig config, boolean async) {
        // Configuration object validation
        Status status = config.validate();
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", config, status.getDescription());
            String error = "Invalid Configuration (" + status.getDescription() + ")";
            config.setStatus(error);
            return new Status(StatusCode.BADREQUEST, error);
        }
        return addStaticFlowInternal(config, async, false);
    }


    @Override
    public Status addStaticFlowAsync(FlowConfig config) {
        return addStaticFlow(config, true);
    }

    /**
     * Private method to add a static flow configuration which does not run any
     * validation on the passed FlowConfig object. If restore is set to true,
     * configuration is stored in configuration database regardless the
     * installation on the network node was successful. This is useful at boot
     * when static flows are present in startup configuration and are read
     * before the switches connects.
     *
     * @param config
     *            The static flow configuration
     * @param restore
     *            if true, the configuration is stored regardless the
     *            installation on the network node was successful
     * @return The status of this request
     */
    private Status addStaticFlowInternal(FlowConfig config, boolean async, boolean restore) {
        boolean multipleFlowPush = false;
        String error;
        Status status;
        config.setStatus(StatusCode.SUCCESS.toString());

        // Presence check
        if (flowConfigExists(config)) {
            error = "Entry with this name on specified switch already exists";
            log.warn("Entry with this name on specified switch already exists: {}", config);
            config.setStatus(error);
            return new Status(StatusCode.CONFLICT, error);
        }

        if ((config.getIngressPort() == null) && config.getPortGroup() != null) {
            for (String portGroupName : portGroupConfigs.keySet()) {
                if (portGroupName.equalsIgnoreCase(config.getPortGroup())) {
                    multipleFlowPush = true;
                    break;
                }
            }
            if (!multipleFlowPush) {
                log.warn("Invalid Configuration(Invalid PortGroup Name) for flow {}", config);
                error = "Invalid Configuration (Invalid PortGroup Name)";
                config.setStatus(error);
                return new Status(StatusCode.BADREQUEST, error);
            }
        }

        /*
         * If requested program the entry in hardware first before updating the
         * StaticFlow DB
         */
        if (!multipleFlowPush) {
            // Program hw
            if (config.installInHw()) {
                FlowEntry entry = config.getFlowEntry();
                status = async ? this.installFlowEntryAsync(entry) : this.installFlowEntry(entry);
                if (!status.isSuccess()) {
                    config.setStatus(status.getDescription());
                    if (!restore) {
                        return status;
                    }
                }
            }
        }

        /*
         * When the control reaches this point, either of the following
         * conditions is true 1. This is a single entry configuration (non
         * PortGroup) and the hardware installation is successful 2. This is a
         * multiple entry configuration (PortGroup) and hardware installation is
         * NOT done directly on this event. 3. The User prefers to retain the
         * configuration in Controller and skip hardware installation.
         *
         * Hence it is safe to update the StaticFlow DB at this point.
         *
         * Note : For the case of PortGrouping, it is essential to have this DB
         * populated before the PortGroupListeners can query for the DB
         * triggered using portGroupChanged event...
         */
        Integer ordinal = staticFlowsOrdinal.get(0);
        staticFlowsOrdinal.put(0, ++ordinal);
        staticFlows.put(ordinal, config);

        if (multipleFlowPush) {
            PortGroupConfig pgconfig = portGroupConfigs.get(config.getPortGroup());
            Map<Node, PortGroup> existingData = portGroupData.get(pgconfig);
            if (existingData != null) {
                portGroupChanged(pgconfig, existingData, true);
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    private void addStaticFlowsToSwitch(Node node) {
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            FlowConfig config = entry.getValue();
            if (config.isPortGroupEnabled()) {
                continue;
            }
            if (config.getNode().equals(node)) {
                if (config.installInHw() && !config.getStatus().equals(StatusCode.SUCCESS.toString())) {
                    Status status = this.installFlowEntryAsync(config.getFlowEntry());
                    config.setStatus(status.getDescription());
                }
            }
        }
        // Update cluster cache
        refreshClusterStaticFlowsStatus(node);
    }

    private void updateStaticFlowConfigsOnNodeDown(Node node) {
        log.trace("Updating Static Flow configs on node down: {}", node);

        List<Integer> toRemove = new ArrayList<Integer>();
        for (Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {

            FlowConfig config = entry.getValue();

            if (config.isPortGroupEnabled()) {
                continue;
            }

            if (config.installInHw() && config.getNode().equals(node)) {
                if (config.isInternalFlow()) {
                    // Take note of this controller generated static flow
                    toRemove.add(entry.getKey());
                } else {
                    config.setStatus(NODE_DOWN);
                }
            }
        }
        // Remove controller generated static flows for this node
        for (Integer index : toRemove) {
            staticFlows.remove(index);
        }
        // Update cluster cache
        refreshClusterStaticFlowsStatus(node);

    }

    private void updateStaticFlowConfigsOnContainerModeChange(UpdateType update) {
        log.trace("Updating Static Flow configs on container mode change: {}", update);

        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            FlowConfig config = entry.getValue();
            if (config.isPortGroupEnabled()) {
                continue;
            }
            if (config.installInHw() && !config.isInternalFlow()) {
                switch (update) {
                case ADDED:
                    config.setStatus("Removed from node because in container mode");
                    break;
                case REMOVED:
                    config.setStatus(StatusCode.SUCCESS.toString());
                    break;
                default:
                    break;
                }
            }
        }
        // Update cluster cache
        refreshClusterStaticFlowsStatus(null);
    }

    @Override
    public Status removeStaticFlow(FlowConfig config) {
        return removeStaticFlow(config, false);
    }

    @Override
    public Status removeStaticFlowAsync(FlowConfig config) {
        return removeStaticFlow(config, true);
    }

    private Status removeStaticFlow(FlowConfig config, boolean async) {
        /*
         * No config.isInternal() check as NB does not take this path and GUI
         * cannot issue a delete on an internal generated flow. We need this
         * path to be accessible when switch mode is changed from proactive to
         * reactive, so that we can remove the internal generated LLDP and ARP
         * punt flows
         */

        // Look for the target configuration entry
        Integer key = 0;
        FlowConfig target = null;
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            if (entry.getValue().isByNameAndNodeIdEqual(config)) {
                key = entry.getKey();
                target = entry.getValue();
                break;
            }
        }
        if (target == null) {
            return new Status(StatusCode.NOTFOUND, "Entry Not Present");
        }

        // Program the network node
        Status status = async ? this.uninstallFlowEntryAsync(config.getFlowEntry()) : this.uninstallFlowEntry(config
                .getFlowEntry());

        // Update configuration database if programming was successful
        if (status.isSuccess()) {
            staticFlows.remove(key);
        }

        return status;
    }

    @Override
    public Status removeStaticFlow(String name, Node node) {
       return removeStaticFlow(name, node, false);
    }

    @Override
    public Status removeStaticFlowAsync(String name, Node node) {
        return removeStaticFlow(name, node, true);
    }

    private Status removeStaticFlow(String name, Node node, boolean async) {
        // Look for the target configuration entry
        Integer key = 0;
        FlowConfig target = null;
        for (ConcurrentMap.Entry<Integer, FlowConfig> mapEntry : staticFlows.entrySet()) {
            if (mapEntry.getValue().isByNameAndNodeIdEqual(name, node)) {
                key = mapEntry.getKey();
                target = mapEntry.getValue();
                break;
            }
        }
        if (target == null) {
            return new Status(StatusCode.NOTFOUND, "Entry Not Present");
        }

        // Validity check for api3 entry point
        if (target.isInternalFlow()) {
            String msg = "Invalid operation: Controller generated flow cannot be deleted";
            String logMsg = msg + ": {}";
            log.warn(logMsg, name);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        if (target.isPortGroupEnabled()) {
            String msg = "Invalid operation: Port Group flows cannot be deleted through this API";
            String logMsg = msg + ": {}";
            log.warn(logMsg, name);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        // Program the network node
        Status status = this.removeEntry(target.getFlowEntry(), async);

        // Update configuration database if programming was successful
        if (status.isSuccess()) {
            staticFlows.remove(key);
        }

        return status;
    }

    @Override
    public Status modifyStaticFlow(FlowConfig newFlowConfig) {
        // Validity check for api3 entry point
        if (newFlowConfig.isInternalFlow()) {
            String msg = "Invalid operation: Controller generated flow cannot be modified";
            String logMsg = msg + ": {}";
            log.warn(logMsg, newFlowConfig);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        // Validity Check
        Status status = newFlowConfig.validate();
        if (!status.isSuccess()) {
            String msg = "Invalid Configuration (" + status.getDescription() + ")";
            newFlowConfig.setStatus(msg);
            log.warn("Invalid Configuration for flow {}. The failure is {}", newFlowConfig, status.getDescription());
            return new Status(StatusCode.BADREQUEST, msg);
        }

        FlowConfig oldFlowConfig = null;
        Integer index = null;
        for (ConcurrentMap.Entry<Integer, FlowConfig> mapEntry : staticFlows.entrySet()) {
            FlowConfig entry = mapEntry.getValue();
            if (entry.isByNameAndNodeIdEqual(newFlowConfig.getName(), newFlowConfig.getNode())) {
                oldFlowConfig = entry;
                index = mapEntry.getKey();
                break;
            }
        }

        if (oldFlowConfig == null) {
            String msg = "Attempt to modify a non existing static flow";
            String logMsg = msg + ": {}";
            log.warn(logMsg, newFlowConfig);
            return new Status(StatusCode.NOTFOUND, msg);
        }

        // Do not attempt to reinstall the flow, warn user
        if (newFlowConfig.equals(oldFlowConfig)) {
            String msg = "No modification detected";
            log.trace("Static flow modification skipped. New flow and old flow are the same: {}", newFlowConfig);
            return new Status(StatusCode.SUCCESS, msg);
        }

        // If flow is installed, program the network node
        status = new Status(StatusCode.SUCCESS, "Saved in config");
        if (oldFlowConfig.installInHw()) {
            status = this.modifyFlowEntry(oldFlowConfig.getFlowEntry(), newFlowConfig.getFlowEntry());
        }

        // Update configuration database if programming was successful
        if (status.isSuccess()) {
            newFlowConfig.setStatus(status.getDescription());
            staticFlows.put(index, newFlowConfig);
        }

        return status;
    }

    @Override
    public Status toggleStaticFlowStatus(String name, Node node) {
        return toggleStaticFlowStatus(getStaticFlow(name, node));
    }

    @Override
    public Status toggleStaticFlowStatus(FlowConfig config) {
        if (config == null) {
            String msg = "Invalid request: null flow config";
            log.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }
        // Validity check for api3 entry point
        if (config.isInternalFlow()) {
            String msg = "Invalid operation: Controller generated flow cannot be modified";
            String logMsg = msg + ": {}";
            log.warn(logMsg, config);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        // Find the config entry
        Integer key = 0;
        FlowConfig target = null;
        for (Map.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            FlowConfig conf = entry.getValue();
            if (conf.isByNameAndNodeIdEqual(config)) {
                key = entry.getKey();
                target = conf;
                break;
            }
        }
        if (target != null) {
            Status status = target.validate();
            if (!status.isSuccess()) {
                log.warn(status.getDescription());
                return status;
            }
            status = (target.installInHw()) ? this.uninstallFlowEntry(target.getFlowEntry()) : this
                                    .installFlowEntry(target.getFlowEntry());
            if (status.isSuccess()) {
                // Update Configuration database
                target.setStatus(StatusCode.SUCCESS.toString());
                target.toggleInstallation();
                staticFlows.put(key, target);
            }
            return status;
        }

        return new Status(StatusCode.NOTFOUND, "Unable to locate the entry. Failed to toggle status");
    }

    /**
     * Reinsert all static flows entries in the cache to force cache updates in
     * the cluster. This is useful when only some parameters were changed in the
     * entries, like the status.
     *
     * @param node
     *            The node for which the static flow configurations have to be
     *            refreshed. If null, all nodes static flows will be refreshed.
     */
    private void refreshClusterStaticFlowsStatus(Node node) {
        // Refresh cluster cache
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            if (node == null || entry.getValue().getNode().equals(node)) {
                staticFlows.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Uninstall all the non-internal Flow Entries present in the software view.
     * If requested, a copy of each original flow entry will be stored in the
     * inactive list so that it can be re-applied when needed (This is typically
     * the case when running in the default container and controller moved to
     * container mode) NOTE WELL: The routine as long as does a bulk change will
     * operate only on the entries for nodes locally attached so to avoid
     * redundant operations initiated by multiple nodes
     *
     * @param preserveFlowEntries
     *            if true, a copy of each original entry is stored in the
     *            inactive list
     */
    private void uninstallAllFlowEntries(boolean preserveFlowEntries) {
        log.trace("Uninstalling all non-internal flows");

        List<FlowEntryInstall> toRemove = new ArrayList<FlowEntryInstall>();

        // Store entries / create target list
        for (ConcurrentMap.Entry<FlowEntryInstall, FlowEntryInstall> mapEntry : installedSwView.entrySet()) {
            FlowEntryInstall flowEntries = mapEntry.getValue();
            // Skip internal generated static flows
            if (!flowEntries.isInternal()) {
                toRemove.add(flowEntries);
                // Store the original entries if requested
                if (preserveFlowEntries) {
                    inactiveFlows.put(flowEntries.getOriginal(), flowEntries.getOriginal());
                }
            }
        }

        // Now remove the entries
        for (FlowEntryInstall flowEntryHw : toRemove) {
            Node n = flowEntryHw.getNode();
            if (n != null && connectionManager.getLocalityStatus(n) == ConnectionLocality.LOCAL) {
                Status status = this.removeEntryInternal(flowEntryHw, false);
                if (!status.isSuccess()) {
                    log.trace("Failed to remove entry: {}. The failure is: {}", flowEntryHw, status.getDescription());
                }
            } else {
                log.debug("Not removing entry {} because not connected locally, the remote guy will do it's job",
                        flowEntryHw);
            }
        }
    }

    /**
     * Re-install all the Flow Entries present in the inactive list The inactive
     * list will be empty at the end of this call This function is called on the
     * default container instance of FRM only when the last container is deleted
     */
    private void reinstallAllFlowEntries() {
        log.trace("Reinstalling all inactive flows");

        for (FlowEntry flowEntry : this.inactiveFlows.keySet()) {
            this.addEntry(flowEntry, false);
        }

        // Empty inactive list in any case
        inactiveFlows.clear();
    }

    @Override
    public List<FlowConfig> getStaticFlows() {
        return new ArrayList<FlowConfig>(staticFlows.values());
    }

    @Override
    public FlowConfig getStaticFlow(String name, Node node) {
        ConcurrentMap.Entry<Integer, FlowConfig> entry = getStaticFlowEntry(name, node);
        if(entry != null) {
            return entry.getValue();
        }
        return null;
    }

    @Override
    public List<FlowConfig> getStaticFlows(Node node) {
        List<FlowConfig> list = new ArrayList<FlowConfig>();
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            if (entry.getValue().onNode(node)) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    @Override
    public List<String> getStaticFlowNamesForNode(Node node) {
        List<String> list = new ArrayList<String>();
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            if (entry.getValue().onNode(node)) {
                list.add(entry.getValue().getName());
            }
        }
        return list;
    }

    @Override
    public List<Node> getListNodeWithConfiguredFlows() {
        Set<Node> set = new HashSet<Node>();
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            set.add(entry.getValue().getNode());
        }
        return new ArrayList<Node>(set);
    }

    private void loadFlowConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, PORT_GROUP_FILE_NAME)) {
            addPortGroupConfig(((PortGroupConfig) conf).getName(), ((PortGroupConfig) conf).getMatchString(), true);
        }

        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, STATIC_FLOWS_FILE_NAME)) {
            addStaticFlowInternal((FlowConfig) conf, false, true);
        }
    }

    @Override
    public Object readObject(ObjectInputStream ois) throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }

    @Override
    public Status saveConfig() {
        return saveConfigInternal();
    }

    private Status saveConfigInternal() {
        List<ConfigurationObject> nonDynamicFlows = new ArrayList<ConfigurationObject>();

        for (Integer ordinal : staticFlows.keySet()) {
            FlowConfig config = staticFlows.get(ordinal);
            // Do not save dynamic and controller generated static flows
            if (config.isDynamic() || config.isInternalFlow()) {
                continue;
            }
            nonDynamicFlows.add(config);
        }

        configurationService.persistConfiguration(nonDynamicFlows, STATIC_FLOWS_FILE_NAME);
        configurationService.persistConfiguration(new ArrayList<ConfigurationObject>(portGroupConfigs.values()),
                PORT_GROUP_FILE_NAME);

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void subnetNotify(Subnet sub, boolean add) {
    }

    private boolean programInternalFlow(boolean proactive, FlowConfig fc) {
        boolean retVal = true; // program flows unless determined otherwise
        if(proactive) {
            // if the flow already exists do not program
            if(flowConfigExists(fc)) {
                retVal = false;
            }
        } else {
            // if the flow does not exist do not program
            if(!flowConfigExists(fc)) {
                retVal = false;
            }
        }
        return retVal;
    }

    /**
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.switchmanager.ISwitchManagerAware#modeChangeNotify(org.opendaylight.controller.sal.core.Node,
     *      boolean)
     *
     *      This method can be called from within the OSGi framework context,
     *      given the programming operation can take sometime, it not good
     *      pratice to have in it's context operations that can take time,
     *      hence moving off to a different thread for async processing.
     */
    private ExecutorService executor;
    @Override
    public void modeChangeNotify(final Node node, final boolean proactive) {
        Callable<Status> modeChangeCallable = new Callable<Status>() {
            @Override
            public Status call() throws Exception {
                List<FlowConfig> defaultConfigs = new ArrayList<FlowConfig>();

                List<String> puntAction = new ArrayList<String>();
                puntAction.add(ActionType.CONTROLLER.toString());

                FlowConfig allowARP = new FlowConfig();
                allowARP.setInstallInHw(true);
                allowARP.setName(FlowConfig.INTERNALSTATICFLOWBEGIN + "Punt ARP" + FlowConfig.INTERNALSTATICFLOWEND);
                allowARP.setPriority("1");
                allowARP.setNode(node);
                allowARP.setEtherType("0x" + Integer.toHexString(EtherTypes.ARP.intValue())
                        .toUpperCase());
                allowARP.setActions(puntAction);
                defaultConfigs.add(allowARP);

                FlowConfig allowLLDP = new FlowConfig();
                allowLLDP.setInstallInHw(true);
                allowLLDP.setName(FlowConfig.INTERNALSTATICFLOWBEGIN + "Punt LLDP" + FlowConfig.INTERNALSTATICFLOWEND);
                allowLLDP.setPriority("1");
                allowLLDP.setNode(node);
                allowLLDP.setEtherType("0x" + Integer.toHexString(EtherTypes.LLDP.intValue())
                        .toUpperCase());
                allowLLDP.setActions(puntAction);
                defaultConfigs.add(allowLLDP);

                List<String> dropAction = new ArrayList<String>();
                dropAction.add(ActionType.DROP.toString());

                FlowConfig dropAllConfig = new FlowConfig();
                dropAllConfig.setInstallInHw(true);
                dropAllConfig.setName(FlowConfig.INTERNALSTATICFLOWBEGIN + "Catch-All Drop"
                        + FlowConfig.INTERNALSTATICFLOWEND);
                dropAllConfig.setPriority("0");
                dropAllConfig.setNode(node);
                dropAllConfig.setActions(dropAction);
                defaultConfigs.add(dropAllConfig);

                log.trace("Forwarding mode for node {} set to {}", node, (proactive ? "proactive" : "reactive"));
                for (FlowConfig fc : defaultConfigs) {
                    // check if the frm really needs to act on the notification.
                    // this is to check against duplicate notifications
                    if(programInternalFlow(proactive, fc)) {
                        Status status = (proactive) ? addStaticFlowInternal(fc, false, false) : removeStaticFlow(fc);
                        if (status.isSuccess()) {
                            log.trace("{} Proactive Static flow: {}", (proactive ? "Installed" : "Removed"), fc.getName());
                        } else {
                            log.warn("Failed to {} Proactive Static flow: {}", (proactive ? "install" : "remove"),
                                    fc.getName());
                        }
                    } else {
                        log.debug("Got redundant install request for internal flow: {} on node: {}. Request not sent to FRM.", fc.getName(), node);
                    }
                }
                return new Status(StatusCode.SUCCESS);
            }
        };

        /*
         * Execute the work outside the caller context, this could be an
         * expensive operation and we don't want to block the caller for it.
         */
        this.executor.submit(modeChangeCallable);
    }

    /**
     * Remove from the databases all the flows installed on the node
     *
     * @param node
     */
    private void cleanDatabaseForNode(Node node) {
        log.trace("Cleaning Flow database for Node {}", node);
        if (nodeFlows.containsKey(node)) {
            List<FlowEntryInstall> toRemove = new ArrayList<FlowEntryInstall>(nodeFlows.get(node));

            for (FlowEntryInstall entry : toRemove) {
                updateSwViews(entry, false);
            }
        }
    }

    private boolean doesFlowContainNodeConnector(Flow flow, NodeConnector nc) {
        if (nc == null) {
            return false;
        }

        Match match = flow.getMatch();
        if (match.isPresent(MatchType.IN_PORT)) {
            NodeConnector matchPort = (NodeConnector) match.getField(MatchType.IN_PORT).getValue();
            if (matchPort.equals(nc)) {
                return true;
            }
        }
        List<Action> actionsList = flow.getActions();
        if (actionsList != null) {
            for (Action action : actionsList) {
                if (action instanceof Output) {
                    NodeConnector actionPort = ((Output) action).getPort();
                    if (actionPort.equals(nc)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void notifyNode(Node node, UpdateType type, Map<String, Property> propMap) {
        this.pendingEvents.offer(new NodeUpdateEvent(type, node));
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector, UpdateType type, Map<String, Property> propMap) {
        boolean updateStaticFlowCluster = false;

        switch (type) {
        case ADDED:
            break;
        case CHANGED:
            Config config = (propMap == null) ? null : (Config) propMap.get(Config.ConfigPropName);
            if (config != null) {
                switch (config.getValue()) {
                case Config.ADMIN_DOWN:
                    log.trace("Port {} is administratively down: uninstalling interested flows", nodeConnector);
                    updateStaticFlowCluster = removeFlowsOnNodeConnectorDown(nodeConnector);
                    break;
                case Config.ADMIN_UP:
                    log.trace("Port {} is administratively up: installing interested flows", nodeConnector);
                    updateStaticFlowCluster = installFlowsOnNodeConnectorUp(nodeConnector);
                    break;
                case Config.ADMIN_UNDEF:
                    break;
                default:
                }
            }
            break;
        case REMOVED:
            // This is the case where a switch port is removed from the SDN agent space
            log.trace("Port {} was removed from our control: uninstalling interested flows", nodeConnector);
            updateStaticFlowCluster = removeFlowsOnNodeConnectorDown(nodeConnector);
            break;
        default:

        }

        if (updateStaticFlowCluster) {
            refreshClusterStaticFlowsStatus(nodeConnector.getNode());
        }
    }

    /*
     * It goes through the static flows configuration, it identifies the ones
     * which have the specified node connector as input or output port and
     * install them on the network node if they are marked to be installed in
     * hardware and their status shows they were not installed yet
     */
    private boolean installFlowsOnNodeConnectorUp(NodeConnector nodeConnector) {
        boolean updated = false;
        List<FlowConfig> flowConfigForNode = getStaticFlows(nodeConnector.getNode());
        for (FlowConfig flowConfig : flowConfigForNode) {
            if (doesFlowContainNodeConnector(flowConfig.getFlow(), nodeConnector)) {
                if (flowConfig.installInHw() && !flowConfig.getStatus().equals(StatusCode.SUCCESS.toString())) {
                    Status status = this.installFlowEntry(flowConfig.getFlowEntry());
                    if (!status.isSuccess()) {
                        flowConfig.setStatus(status.getDescription());
                    } else {
                        flowConfig.setStatus(StatusCode.SUCCESS.toString());
                    }
                    updated = true;
                }
            }
        }
        return updated;
    }

    /*
     * Remove from the network node all the flows which have the specified node
     * connector as input or output port. If any of the flow entry is a static
     * flow, it updates the correspondent configuration.
     */
    private boolean removeFlowsOnNodeConnectorDown(NodeConnector nodeConnector) {
        boolean updated = false;
        List<FlowEntryInstall> nodeFlowEntries = nodeFlows.get(nodeConnector.getNode());
        if (nodeFlowEntries == null) {
            return updated;
        }
        for (FlowEntryInstall fei : new ArrayList<FlowEntryInstall>(nodeFlowEntries)) {
            if (doesFlowContainNodeConnector(fei.getInstall().getFlow(), nodeConnector)) {
                Status status = this.removeEntryInternal(fei, true);
                if (!status.isSuccess()) {
                    continue;
                }
                /*
                 * If the flow entry is a static flow, then update its
                 * configuration
                 */
                if (fei.getGroupName().equals(FlowConfig.STATICFLOWGROUP)) {
                    FlowConfig flowConfig = getStaticFlow(fei.getFlowName(), fei.getNode());
                    if (flowConfig != null) {
                        flowConfig.setStatus(PORT_REMOVED);
                        updated = true;
                    }
                }
            }
        }
        return updated;
    }

    private FlowConfig getDerivedFlowConfig(FlowConfig original, String configName, Short port) {
        FlowConfig derivedFlow = new FlowConfig(original);
        derivedFlow.setDynamic(true);
        derivedFlow.setPortGroup(null);
        derivedFlow.setName(original.getName() + "_" + configName + "_" + port);
        derivedFlow.setIngressPort(port + "");
        return derivedFlow;
    }

    private void addPortGroupFlows(PortGroupConfig config, Node node, PortGroup data) {
        for (FlowConfig staticFlow : staticFlows.values()) {
            if (staticFlow.getPortGroup() == null) {
                continue;
            }
            if ((staticFlow.getNode().equals(node)) && (staticFlow.getPortGroup().equals(config.getName()))) {
                for (Short port : data.getPorts()) {
                    FlowConfig derivedFlow = getDerivedFlowConfig(staticFlow, config.getName(), port);
                    addStaticFlowInternal(derivedFlow, false, false);
                }
            }
        }
    }

    private void removePortGroupFlows(PortGroupConfig config, Node node, PortGroup data) {
        for (FlowConfig staticFlow : staticFlows.values()) {
            if (staticFlow.getPortGroup() == null) {
                continue;
            }
            if (staticFlow.getNode().equals(node) && staticFlow.getPortGroup().equals(config.getName())) {
                for (Short port : data.getPorts()) {
                    FlowConfig derivedFlow = getDerivedFlowConfig(staticFlow, config.getName(), port);
                    removeStaticFlow(derivedFlow);
                }
            }
        }
    }

    @Override
    public void portGroupChanged(PortGroupConfig config, Map<Node, PortGroup> data, boolean add) {
        log.trace("PortGroup Changed for: {} Data: {}", config, portGroupData);
        Map<Node, PortGroup> existingData = portGroupData.get(config);
        if (existingData != null) {
            for (Map.Entry<Node, PortGroup> entry : data.entrySet()) {
                PortGroup existingPortGroup = existingData.get(entry.getKey());
                if (existingPortGroup == null) {
                    if (add) {
                        existingData.put(entry.getKey(), entry.getValue());
                        addPortGroupFlows(config, entry.getKey(), entry.getValue());
                    }
                } else {
                    if (add) {
                        existingPortGroup.getPorts().addAll(entry.getValue().getPorts());
                        addPortGroupFlows(config, entry.getKey(), entry.getValue());
                    } else {
                        existingPortGroup.getPorts().removeAll(entry.getValue().getPorts());
                        removePortGroupFlows(config, entry.getKey(), entry.getValue());
                    }
                }
            }
        } else {
            if (add) {
                portGroupData.put(config, data);
                for (Node swid : data.keySet()) {
                    addPortGroupFlows(config, swid, data.get(swid));
                }
            }
        }
    }

    @Override
    public boolean addPortGroupConfig(String name, String regex, boolean restore) {
        PortGroupConfig config = portGroupConfigs.get(name);
        if (config != null) {
            return false;
        }

        if ((portGroupProvider == null) && !restore) {
            return false;
        }
        if ((portGroupProvider != null) && (!portGroupProvider.isMatchCriteriaSupported(regex))) {
            return false;
        }

        config = new PortGroupConfig(name, regex);
        portGroupConfigs.put(name, config);
        if (portGroupProvider != null) {
            portGroupProvider.createPortGroupConfig(config);
        }
        return true;
    }

    @Override
    public boolean delPortGroupConfig(String name) {
        PortGroupConfig config = portGroupConfigs.get(name);
        if (config == null) {
            return false;
        }

        if (portGroupProvider != null) {
            portGroupProvider.deletePortGroupConfig(config);
        }
        portGroupConfigs.remove(name);
        return true;
    }

    @Override
    public Map<String, PortGroupConfig> getPortGroupConfigs() {
        return portGroupConfigs;
    }

    public boolean isPortGroupSupported() {
        if (portGroupProvider == null) {
            return false;
        }
        return true;
    }

    public void setIContainer(IContainer s) {
        this.container = s;
    }

    public void unsetIContainer(IContainer s) {
        if (this.container == s) {
            this.container = null;
        }
    }

    public void setConfigurationContainerService(IConfigurationContainerService service) {
        log.trace("Got configuration service set request {}", service);
        this.configurationService = service;
    }

    public void unsetConfigurationContainerService(IConfigurationContainerService service) {
        log.trace("Got configuration service UNset request");
        this.configurationService = null;
    }

    @Override
    public PortGroupProvider getPortGroupProvider() {
        return portGroupProvider;
    }

    public void unsetPortGroupProvider(PortGroupProvider portGroupProvider) {
        this.portGroupProvider = null;
    }

    public void setPortGroupProvider(PortGroupProvider portGroupProvider) {
        this.portGroupProvider = portGroupProvider;
        portGroupProvider.registerPortGroupChange(this);
        for (PortGroupConfig config : portGroupConfigs.values()) {
            portGroupProvider.createPortGroupConfig(config);
        }
    }

    public void setFrmAware(IForwardingRulesManagerAware obj) {
        this.frmAware.add(obj);
    }

    public void unsetFrmAware(IForwardingRulesManagerAware obj) {
        this.frmAware.remove(obj);
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    private String getContainerName() {
        if (container == null) {
            return GlobalConstants.DEFAULT.toString();
        }
        return container.getName();
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {

        inContainerMode = false;

        if (portGroupProvider != null) {
            portGroupProvider.registerPortGroupChange(this);
        }

        nodeFlows = new ConcurrentHashMap<Node, List<FlowEntryInstall>>();
        groupFlows = new ConcurrentHashMap<String, List<FlowEntryInstall>>();

        cacheStartup();

        /*
         * If we are not the first cluster node to come up, do not initialize
         * the static flow entries ordinal
         */
        if (staticFlowsOrdinal.size() == 0) {
            staticFlowsOrdinal.put(0, Integer.valueOf(0));
        }

        pendingEvents = new LinkedBlockingQueue<FRMEvent>();

        // Initialize the event handler thread
        frmEventHandler = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stopping) {
                    try {
                        final FRMEvent event = pendingEvents.take();
                        if (event == null) {
                            log.warn("Dequeued null event");
                            continue;
                        }
                        log.trace("Dequeued {} event", event.getClass().getSimpleName());
                        if (event instanceof NodeUpdateEvent) {
                            NodeUpdateEvent update = (NodeUpdateEvent) event;
                            Node node = update.getNode();
                            switch (update.getUpdateType()) {
                            case ADDED:
                                addStaticFlowsToSwitch(node);
                                break;
                            case REMOVED:
                                cleanDatabaseForNode(node);
                                updateStaticFlowConfigsOnNodeDown(node);
                                break;
                            default:
                            }
                        } else if (event instanceof ErrorReportedEvent) {
                            ErrorReportedEvent errEvent = (ErrorReportedEvent) event;
                            processErrorEvent(errEvent);
                        } else if (event instanceof WorkOrderEvent) {
                            /*
                             * Take care of handling the remote Work request
                             */
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    WorkOrderEvent work = (WorkOrderEvent) event;
                                    FlowEntryDistributionOrder fe = work.getFe();
                                    if (fe != null) {
                                        logsync.trace("Executing the workOrder {}", fe);
                                        Status gotStatus = null;
                                        FlowEntryInstall feiCurrent = fe.getEntry();
                                        FlowEntryInstall feiNew = workOrder.get(fe);
                                        switch (fe.getUpType()) {
                                        case ADDED:
                                            gotStatus = addEntryInHw(feiCurrent, false);
                                            break;
                                        case CHANGED:
                                            gotStatus = modifyEntryInHw(feiCurrent, feiNew, false);
                                            break;
                                        case REMOVED:
                                            gotStatus = removeEntryInHw(feiCurrent, false);
                                            break;
                                        }
                                        // Remove the Order
                                        workOrder.remove(fe);
                                        logsync.trace(
                                                "The workOrder has been executed and now the status is being returned {}", fe);
                                        // Place the status
                                        workStatus.put(fe, gotStatus);
                                    } else {
                                        log.warn("Not expected null WorkOrder", work);
                                    }
                                }
                            };
                            if(executor != null) {
                                executor.execute(r);
                            }
                        } else if (event instanceof WorkStatusCleanup) {
                            /*
                             * Take care of handling the remote Work request
                             */
                            WorkStatusCleanup work = (WorkStatusCleanup) event;
                            FlowEntryDistributionOrder fe = work.getFe();
                            if (fe != null) {
                                logsync.trace("The workStatus {} is being removed", fe);
                                workStatus.remove(fe);
                            } else {
                                log.warn("Not expected null WorkStatus", work);
                            }
                        }  else if (event instanceof ContainerFlowChangeEvent) {
                            /*
                             * Whether it is an addition or removal, we have to
                             * recompute the merged flows entries taking into
                             * account all the current container flows because
                             * flow merging is not an injective function
                             */
                            updateFlowsContainerFlow();
                        } else if (event instanceof UpdateIndexDBs) {
                            UpdateIndexDBs update = (UpdateIndexDBs)event;
                            updateIndexDatabase(update.getFei(), update.isAddition());
                        } else {
                            log.warn("Dequeued unknown event {}", event.getClass().getSimpleName());
                        }
                    } catch (InterruptedException e) {
                        // clear pending events
                        pendingEvents.clear();
                    }
                }
            }
        }, "FRM EventHandler Collector");
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        // Interrupt the thread
        frmEventHandler.interrupt();
        // Clear the pendingEvents queue
        pendingEvents.clear();
        frmAware.clear();
        workMonitor.clear();
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        /*
         * If running in default container, need to know if controller is in
         * container mode
         */
        if (GlobalConstants.DEFAULT.toString().equals(this.getContainerName())) {
            inContainerMode = containerManager.inContainerMode();
        }

        // Initialize graceful stop flag
        stopping = false;

        // Allocate the executor service
        this.executor = Executors.newFixedThreadPool(maxPoolSize);

        // Start event handler thread
        frmEventHandler.start();

        // replay the installedSwView data structure to populate
        // node flows and group flows
        for (FlowEntryInstall fei : installedSwView.values()) {
            pendingEvents.offer(new UpdateIndexDBs(fei, true));
        }

        /*
         * Read startup and build database if we are the coordinator
         */
        loadFlowConfiguration();
    }

    /**
     * Function called by the dependency manager before Container is Stopped and Destroyed.
     */
    public void containerStop() {
        uninstallAllFlowEntries(false);
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     */
    void stop() {
        stopping = true;
        // Shutdown executor
        this.executor.shutdownNow();
        // Now walk all the workMonitor and wake up the one sleeping because
        // destruction is happening
        for (FlowEntryDistributionOrder fe : workMonitor.keySet()) {
            FlowEntryDistributionOrderFutureTask task = workMonitor.get(fe);
            task.cancel(true);
        }
    }

    public void setFlowProgrammerService(IFlowProgrammerService service) {
        this.programmer = service;
    }

    public void unsetFlowProgrammerService(IFlowProgrammerService service) {
        if (this.programmer == service) {
            this.programmer = null;
        }
    }

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
            this.switchManager = null;
        }
    }

    @Override
    public void tagUpdated(String containerName, Node n, short oldTag, short newTag, UpdateType t) {
        if (!container.getName().equals(containerName)) {
            return;
        }
    }

    @Override
    public void containerFlowUpdated(String containerName, ContainerFlow previous, ContainerFlow current, UpdateType t) {
        if (!container.getName().equals(containerName)) {
            return;
        }
        log.trace("Container {}: Updating installed flows because of container flow change: {} {}",
                container.getName(), t, current);
        ContainerFlowChangeEvent ev = new ContainerFlowChangeEvent(previous, current, t);
        pendingEvents.offer(ev);
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector nc, UpdateType t) {
        if (!container.getName().equals(containerName)) {
            return;
        }

        boolean updateStaticFlowCluster = false;

        switch (t) {
        case REMOVED:
            log.trace("Port {} was removed from container: uninstalling interested flows", nc);
            updateStaticFlowCluster = removeFlowsOnNodeConnectorDown(nc);
            break;
        case ADDED:
            log.trace("Port {} was added to container: reinstall interested flows", nc);
            updateStaticFlowCluster = installFlowsOnNodeConnectorUp(nc);

            break;
        case CHANGED:
            break;
        default:
        }

        if (updateStaticFlowCluster) {
            refreshClusterStaticFlowsStatus(nc.getNode());
        }
    }

    @Override
    public void containerModeUpdated(UpdateType update) {
        // Only default container instance reacts on this event
        if (!container.getName().equals(GlobalConstants.DEFAULT.toString())) {
            return;
        }
        switch (update) {
        case ADDED:
            /*
             * Controller is moving to container mode. We are in the default
             * container context, we need to remove all our non-internal flows
             * to prevent any container isolation breakage. We also need to
             * preserve our flow so that they can be re-installed if we move
             * back to non container mode (no containers).
             */
            this.inContainerMode = true;
            this.uninstallAllFlowEntries(true);
            break;
        case REMOVED:
            this.inContainerMode = false;
            this.reinstallAllFlowEntries();
            break;
        default:
            break;
        }

        // Update our configuration DB
        updateStaticFlowConfigsOnContainerModeChange(update);
    }

    protected abstract class FRMEvent {

    }

    private class NodeUpdateEvent extends FRMEvent {
        private final Node node;
        private final UpdateType update;

        public NodeUpdateEvent(UpdateType update, Node node) {
            this.update = update;
            this.node = node;
        }

        public UpdateType getUpdateType() {
            return update;
        }

        public Node getNode() {
            return node;
        }
    }

    private class ErrorReportedEvent extends FRMEvent {
        private final long rid;
        private final Node node;
        private final Object error;

        public ErrorReportedEvent(long rid, Node node, Object error) {
            this.rid = rid;
            this.node = node;
            this.error = error;
        }

        public long getRequestId() {
            return rid;
        }

        public Object getError() {
            return error;
        }

        public Node getNode() {
            return node;
        }
    }

    private class WorkOrderEvent extends FRMEvent {
        private FlowEntryDistributionOrder fe;
        private FlowEntryInstall newEntry;

        /**
         * @param fe
         * @param newEntry
         */
        WorkOrderEvent(FlowEntryDistributionOrder fe, FlowEntryInstall newEntry) {
            this.fe = fe;
            this.newEntry = newEntry;
        }

        /**
         * @return the fe
         */
        public FlowEntryDistributionOrder getFe() {
            return fe;
        }

        /**
         * @return the newEntry
         */
        public FlowEntryInstall getNewEntry() {
            return newEntry;
        }
    }
    private class ContainerFlowChangeEvent extends FRMEvent {
        private final ContainerFlow previous;
        private final ContainerFlow current;
        private final UpdateType type;

        public ContainerFlowChangeEvent(ContainerFlow previous, ContainerFlow current, UpdateType type) {
            this.previous = previous;
            this.current = current;
            this.type = type;
        }

        public ContainerFlow getPrevious() {
            return this.previous;
        }

        public ContainerFlow getCurrent() {
            return this.current;
        }

        public UpdateType getType() {
            return this.type;
        }
    }


    private class WorkStatusCleanup extends FRMEvent {
        private FlowEntryDistributionOrder fe;

        /**
         * @param fe
         */
        WorkStatusCleanup(FlowEntryDistributionOrder fe) {
            this.fe = fe;
        }

        /**
         * @return the fe
         */
        public FlowEntryDistributionOrder getFe() {
            return fe;
        }
    }

    private class UpdateIndexDBs extends FRMEvent {
        private FlowEntryInstall fei;
        private boolean add;

        /**
         *
         * @param fei the flow entry which was installed/removed on the netwrok node
         * @param update
         */
        UpdateIndexDBs(FlowEntryInstall fei, boolean add) {
            this.fei = fei;
            this.add = add;
        }


        /**
         * @return the flowEntryInstall object which was added/removed
         * to/from the installed software view cache
         */
        public FlowEntryInstall getFei() {
            return fei;
        }

        /**
         *
         * @return whether this was an flow addition or removal
         */
        public boolean isAddition() {
            return add;
        }
    }

    @Override
    public Status saveConfiguration() {
        return saveConfig();
    }

    @Override
    public void flowRemoved(Node node, Flow flow) {
        log.trace("Received flow removed notification on {} for {}", node, flow);

        // For flow entry identification, only node, match and priority matter
        FlowEntryInstall test = new FlowEntryInstall(new FlowEntry("", "", flow, node), null);
        FlowEntryInstall installedEntry = this.installedSwView.get(test);
        if (installedEntry == null) {
            log.trace("Entry is not known to us");
            return;
        }

        // Update Static flow status
        Integer key = 0;
        FlowConfig target = null;
        for (Map.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            FlowConfig conf = entry.getValue();
            if (conf.isByNameAndNodeIdEqual(installedEntry.getFlowName(), node)) {
                key = entry.getKey();
                target = conf;
                break;
            }
        }
        if (target != null) {
            // Update Configuration database
            if (target.getHardTimeout() != null || target.getIdleTimeout() != null) {
                /*
                 * No need for checking if actual values: these strings were
                 * validated at configuration creation. Also, after a switch
                 * down scenario, no use to reinstall a timed flow. Mark it as
                 * "do not install". User can manually toggle it.
                 */
                target.toggleInstallation();
            }
            target.setStatus(StatusCode.GONE.toString());
            staticFlows.put(key, target);
        }

        // Update software views
        this.updateSwViews(installedEntry, false);
    }

    @Override
    public void flowErrorReported(Node node, long rid, Object err) {
        log.trace("Got error {} for message rid {} from node {}", new Object[] { err, rid, node });
        pendingEvents.offer(new ErrorReportedEvent(rid, node, err));
    }

    private void processErrorEvent(ErrorReportedEvent event) {
        Node node = event.getNode();
        long rid = event.getRequestId();
        Object error = event.getError();
        String errorString = (error == null) ? "Not provided" : error.toString();
        /*
         * If this was for a flow install, remove the corresponding entry from
         * the software view. If it was a Looking for the rid going through the
         * software database. TODO: A more efficient rid <-> FlowEntryInstall
         * mapping will have to be added in future
         */
        FlowEntryInstall target = null;
        List<FlowEntryInstall> flowEntryInstallList = nodeFlows.get(node);
        // flowEntryInstallList could be null.
        // so check for it.
        if(flowEntryInstallList != null) {
            for (FlowEntryInstall index : flowEntryInstallList) {
                FlowEntryInstall entry = installedSwView.get(index);
                if(entry != null) {
                    if (entry.getRequestId() == rid) {
                        target = entry;
                        break;
                    }
                }
            }
        }
        if (target != null) {
            // This was a flow install, update database
            this.updateSwViews(target, false);
            // also update the config
            if(FlowConfig.STATICFLOWGROUP.equals(target.getGroupName())) {
                ConcurrentMap.Entry<Integer, FlowConfig> staticFlowEntry = getStaticFlowEntry(target.getFlowName(),target.getNode());
                // staticFlowEntry should never be null.
                // the null check is just an extra defensive check.
                if(staticFlowEntry != null) {
                    // Modify status and update cluster cache
                    log.debug("Updating static flow configuration on async error event");
                    String status = String.format("Cannot be installed on node. reason: %s", errorString);
                    staticFlowEntry.getValue().setStatus(status);
                    refreshClusterStaticFlowsStatus(node);
                }
            }
        }

        // Notify listeners
        if (frmAware != null) {
            synchronized (frmAware) {
                for (IForwardingRulesManagerAware frma : frmAware) {
                    try {
                        frma.requestFailed(rid, errorString);
                    } catch (Exception e) {
                        log.warn("Failed to notify {}", frma);
                    }
                }
            }
        }
    }

    @Override
    public Status solicitStatusResponse(Node node, boolean blocking) {
        Status rv = new Status(StatusCode.INTERNALERROR);

        if (this.programmer != null) {
            if (blocking) {
                rv = programmer.syncSendBarrierMessage(node);
            } else {
                rv = programmer.asyncSendBarrierMessage(node);
            }
        }

        return rv;
    }

    public void unsetIConnectionManager(IConnectionManager s) {
        if (s == this.connectionManager) {
            this.connectionManager = null;
        }
    }

    public void setIConnectionManager(IConnectionManager s) {
        this.connectionManager = s;
    }

    public void unsetIContainerManager(IContainerManager s) {
        if (s == this.containerManager) {
            this.containerManager = null;
        }
    }

    public void setIContainerManager(IContainerManager s) {
        this.containerManager = s;
    }

    @Override
    public void entryCreated(Object key, String cacheName, boolean originLocal) {
        /*
         * Do nothing
         */
    }

    @Override
    public void entryUpdated(Object key, Object new_value, String cacheName, boolean originLocal) {
        /*
         * Streamline the updates for the per node and per group index databases
         */
        if (cacheName.equals(INSTALLED_SW_VIEW_CACHE)) {
            pendingEvents.offer(new UpdateIndexDBs((FlowEntryInstall)new_value, true));
        }

        if (originLocal) {
            /*
             * Local updates are of no interest
             */
            return;
        }
        if (cacheName.equals(WORK_ORDER_CACHE)) {
            logsync.trace("Got a WorkOrderCacheUpdate for {}", key);
            /*
             * This is the case of one workOrder becoming available, so we need
             * to dispatch the work to the appropriate handler
             */
            FlowEntryDistributionOrder fe = (FlowEntryDistributionOrder) key;
            FlowEntryInstall fei = fe.getEntry();
            if (fei == null) {
                return;
            }
            Node n = fei.getNode();
            if (connectionManager.getLocalityStatus(n) == ConnectionLocality.LOCAL) {
                logsync.trace("workOrder for fe {} processed locally", fe);
                // I'm the controller in charge for the request, queue it for
                // processing
                pendingEvents.offer(new WorkOrderEvent(fe, (FlowEntryInstall) new_value));
            }
        } else if (cacheName.equals(WORK_STATUS_CACHE)) {
            logsync.trace("Got a WorkStatusCacheUpdate for {}", key);
            /*
             * This is the case of one workOrder being completed and a status
             * returned
             */
            FlowEntryDistributionOrder fe = (FlowEntryDistributionOrder) key;
            /*
             * Check if the order was initiated by this controller in that case
             * we need to actually look at the status returned
             */
            if (fe.getRequestorController()
                    .equals(clusterContainerService.getMyAddress())) {
                FlowEntryDistributionOrderFutureTask fet = workMonitor.remove(fe);
                if (fet != null) {
                    logsync.trace("workStatus response is for us {}", fe);
                    // Signal we got the status
                    fet.gotStatus(fe, workStatus.get(fe));
                    pendingEvents.offer(new WorkStatusCleanup(fe));
                }
            }
        }
    }

    @Override
    public void entryDeleted(Object key, String cacheName, boolean originLocal) {
        /*
         * Streamline the updates for the per node and per group index databases
         */
        if (cacheName.equals(INSTALLED_SW_VIEW_CACHE)) {
            pendingEvents.offer(new UpdateIndexDBs((FlowEntryInstall)key, false));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FlowEntry> getFlowEntriesForNode(Node node) {
        List<FlowEntry> list = new ArrayList<FlowEntry>();
        if (node != null) {
            for (Map.Entry<FlowEntry, FlowEntry> entry : this.originalSwView.entrySet()) {
                if (node.equals(entry.getKey().getNode())) {
                    list.add(entry.getValue().clone());
                }
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FlowEntry> getInstalledFlowEntriesForNode(Node node) {
        List<FlowEntry> list = new ArrayList<FlowEntry>();
        if (node != null) {
            List<FlowEntryInstall> flowEntryInstallList = this.nodeFlows.get(node);
            if(flowEntryInstallList != null) {
                for(FlowEntryInstall fi: flowEntryInstallList) {
                    list.add(fi.getInstall().clone());
                }
            }
        }
        return list;
    }

}
