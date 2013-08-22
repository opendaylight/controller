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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
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
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.IContainerListener;
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
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.switchmanager.Subnet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
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
        IContainerListener,
        ISwitchManagerAware,
        IConfigurationContainerAware,
        IInventoryListener,
        IObjectReader,
        ICacheUpdateAware,
        CommandProvider,
        IFlowProgrammerListener {
    private static final String NODEDOWN = "Node is Down";
    private static final String SUCCESS = StatusCode.SUCCESS.toString();
    private static final Logger log = LoggerFactory.getLogger(ForwardingRulesManager.class);
    private static final String PORTREMOVED = "Port removed";
    private static final Logger logsync = LoggerFactory.getLogger("FRMsync");
    private String frmFileName;
    private String portGroupFileName;
    private ConcurrentMap<Integer, FlowConfig> staticFlows;
    private ConcurrentMap<Integer, Integer> staticFlowsOrdinal;
    private ConcurrentMap<String, PortGroupConfig> portGroupConfigs;
    private ConcurrentMap<PortGroupConfig, Map<Node, PortGroup>> portGroupData;
    private ConcurrentMap<String, Object> TSPolicies;
    private boolean inContainerMode; // being used by global instance only
    private boolean stopping;

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
    static final String WORKORDERCACHE = "frm.workOrder";
    static final String WORKSTATUSCACHE = "frm.workStatus";

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
    private ConcurrentMap<FlowEntryDistributionOrder, FlowEntryInstall> workOrder;

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
    private ConcurrentMap<FlowEntryDistributionOrder, Status> workStatus;

    /*
     * Local Map used to hold the Future which a caller can use to monitor for
     * completion
     */
    private ConcurrentMap<FlowEntryDistributionOrder, FlowEntryDistributionOrderFutureTask> workMonitor =
            new ConcurrentHashMap<FlowEntryDistributionOrder, FlowEntryDistributionOrderFutureTask>();

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
    private Future<Status> distributeWorkOrder(FlowEntryInstall e, FlowEntryInstall u, UpdateType t) {
        // A null entry it's an unexpected condition, anyway it's safe to keep
        // the handling local
        if (e == null) {
            return null;
        }

        Node n = e.getNode();
        if (!connectionManager.isLocal(n)) {
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

        logsync.trace("LOCAL Node {} so processing Entry:{} UpdateType:{}", n, e, t);

        return null;
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
        if (flowEntry == null || flowEntry.getNode() == null) {
            String msg = "Invalid FlowEntry";
            String logMsg = msg + ": {}";
            log.warn(logMsg, flowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
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
            Status ret = addEntriesInternal(installEntry, async);

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
                log.warn("Failed to install the entry: {}. The failure is: {}", installEntry, ret.getDescription());
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
                || newFlowEntry.getNode() == null) {
            String msg = "Modify: Invalid FlowEntry";
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
            log.info("Modify: New flow entry does not satisfy the same "
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
                log.info("Modify: new container flow merged flow entry clashes with existing flow");
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
                succeeded = this.addEntriesInternal(newEntry, async);
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
     * the validity checks are passed
     *
     * @param currentEntries
     * @param newEntries
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status modifyEntryInternal(FlowEntryInstall currentEntries, FlowEntryInstall newEntries, boolean async) {
        Future<Status> futureStatus = distributeWorkOrder(currentEntries, newEntries, UpdateType.CHANGED);
        if (futureStatus != null) {
            Status retStatus = new Status(StatusCode.UNDEFINED);
            try {
                retStatus = futureStatus.get();
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
            return retStatus;
        } else {
            // Modify the flow on the network node
            Status status = async ? programmer.modifyFlowAsync(currentEntries.getNode(), currentEntries.getInstall()
                    .getFlow(), newEntries.getInstall()
                    .getFlow()) : programmer.modifyFlow(currentEntries.getNode(), currentEntries.getInstall()
                    .getFlow(), newEntries.getInstall()
                    .getFlow());

            if (!status.isSuccess()) {
                log.warn("SDN Plugin failed to program the flow: {}. The failure is: {}", newEntries.getInstall(),
                        status.getDescription());
                return status;
            }

            log.trace("Modified {} => {}", currentEntries.getInstall(), newEntries.getInstall());

            // Update DB
            newEntries.setRequestId(status.getRequestId());
            updateLocalDatabase(currentEntries, false);
            updateLocalDatabase(newEntries, true);

            return status;
        }
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
        if (flowEntry == null || flowEntry.getNode() == null) {
            String msg = "Invalid FlowEntry";
            String logMsg = msg + ": {}";
            log.warn(logMsg, flowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
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
                log.warn("Failed to remove the entry: {}. The failure is: {}", entry.getInstall(), ret.getDescription());
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
     *
     * @param entry
     *            the flow entry to remove
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status removeEntryInternal(FlowEntryInstall entry, boolean async) {
        Future<Status> futureStatus = distributeWorkOrder(entry, null, UpdateType.REMOVED);
        if (futureStatus != null) {
            Status retStatus = new Status(StatusCode.UNDEFINED);
            try {
                retStatus = futureStatus.get();
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
            return retStatus;
        } else {
            // Mark the entry to be deleted (for CC just in case we fail)
            entry.toBeDeleted();

            // Remove from node
            Status status = async ? programmer.removeFlowAsync(entry.getNode(), entry.getInstall()
                    .getFlow()) : programmer.removeFlow(entry.getNode(), entry.getInstall()
                    .getFlow());

            if (!status.isSuccess()) {
                log.warn("SDN Plugin failed to program the flow: {}. The failure is: {}", entry.getInstall(),
                        status.getDescription());
                return status;
            }
            log.trace("Removed  {}", entry.getInstall());

            // Update DB
            updateLocalDatabase(entry, false);

            return status;
        }
    }

    /**
     * This is the function that installs the final container flow merged entry
     * on the network node and updates the database. It expects that all the
     * validity and conflict checks are passed. That means it does not check
     * whether this flow would conflict or overwrite an existing one.
     *
     * @param entry
     *            the flow entry to install
     * @param async
     *            the flag indicating if this is a asynchronous request
     * @return the status of this request. In case of asynchronous call, it will
     *         contain the unique id assigned to this request
     */
    private Status addEntriesInternal(FlowEntryInstall entry, boolean async) {
        Future<Status> futureStatus = distributeWorkOrder(entry, null, UpdateType.ADDED);
        if (futureStatus != null) {
            Status retStatus = new Status(StatusCode.UNDEFINED);
            try {
                retStatus = futureStatus.get();
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
            return retStatus;
        } else {
            // Install the flow on the network node
            Status status = async ? programmer.addFlowAsync(entry.getNode(), entry.getInstall()
                    .getFlow()) : programmer.addFlow(entry.getNode(), entry.getInstall()
                    .getFlow());

            if (!status.isSuccess()) {
                log.warn("SDN Plugin failed to program the flow: {}. The failure is: {}", entry.getInstall(),
                        status.getDescription());
                return status;
            }

            log.trace("Added    {}", entry.getInstall());

            // Update DB
            entry.setRequestId(status.getRequestId());
            updateLocalDatabase(entry, true);

            return status;
        }
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

    private void updateLocalDatabase(FlowEntryInstall entry, boolean add) {
        // Update the software view
        updateSwViewes(entry, add);

        // Update node indexed flow database
        updateNodeFlowsDB(entry, add);

        // Update group indexed flow database
        updateGroupFlowsDB(entry, add);
    }

    /*
     * Update the node mapped flows database
     */
    private void updateSwViewes(FlowEntryInstall flowEntries, boolean add) {
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
            updateLocalDatabase(target, false);
        } else {
            // log the error
            log.warn("SDN Plugin failed to remove the flow: {}. The failure is: {}", target.getInstall(),
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
                Status status = this.removeEntry(entry.getOriginal(), false);
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
    private void updateFlowsContainerFlow() {
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
        nodeFlows = new ConcurrentHashMap<Node, List<FlowEntryInstall>>();
        groupFlows = new ConcurrentHashMap<String, List<FlowEntryInstall>>();
        TSPolicies = new ConcurrentHashMap<String, Object>();
        staticFlowsOrdinal = new ConcurrentHashMap<Integer, Integer>();
        portGroupConfigs = new ConcurrentHashMap<String, PortGroupConfig>();
        portGroupData = new ConcurrentHashMap<PortGroupConfig, Map<Node, PortGroup>>();
        staticFlows = new ConcurrentHashMap<Integer, FlowConfig>();
        inactiveFlows = new ConcurrentHashMap<FlowEntry, FlowEntry>();
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this, null);
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
                    list.add(entry.getKey().clone());
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
                    list.add(entry.getKey().getInstall().clone());
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
                    log.info("Ports {} added to FlowEntry {}", portList, flowName);
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
                    log.info("Ports {} removed from FlowEntry {}", portList, flowName);
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
            log.info("Output port replaced with {} for flow {} on node {}", outPort, flowName, node);
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

    @SuppressWarnings("deprecation")
    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            log.warn("Un-initialized clusterContainerService, can't create cache");
            return;
        }

        log.debug("Allocating caches for Container {}", container.getName());

        try {
            clusterContainerService.createCache("frm.originalSwView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.installedSwView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.inactiveFlows",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.nodeFlows",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.groupFlows",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.staticFlows",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.flowsSaveEvent",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.staticFlowsOrdinal",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.portGroupConfigs",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.portGroupData",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache("frm.TSPolicies",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterContainerService.createCache(WORKSTATUSCACHE,
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache(WORKORDERCACHE,
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

        } catch (CacheConfigException cce) {
            log.error("CacheConfigException");
        } catch (CacheExistException cce) {
            log.error("CacheExistException");
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCaches() {
        ConcurrentMap<?, ?> map;

        if (this.clusterContainerService == null) {
            log.warn("un-initialized clusterContainerService, can't retrieve cache");
            nonClusterObjectCreate();
            return;
        }

        log.debug("Retrieving Caches for Container {}", container.getName());

        map = clusterContainerService.getCache("frm.originalSwView");
        if (map != null) {
            originalSwView = (ConcurrentMap<FlowEntry, FlowEntry>) map;
        } else {
            log.error("Retrieval of frm.originalSwView cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.installedSwView");
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

        map = clusterContainerService.getCache("frm.nodeFlows");
        if (map != null) {
            nodeFlows = (ConcurrentMap<Node, List<FlowEntryInstall>>) map;
        } else {
            log.error("Retrieval of cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache("frm.groupFlows");
        if (map != null) {
            groupFlows = (ConcurrentMap<String, List<FlowEntryInstall>>) map;
        } else {
            log.error("Retrieval of frm.groupFlows cache failed for Container {}", container.getName());
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

        map = clusterContainerService.getCache(WORKORDERCACHE);
        if (map != null) {
            workOrder = (ConcurrentMap<FlowEntryDistributionOrder, FlowEntryInstall>) map;
        } else {
            log.error("Retrieval of " + WORKORDERCACHE + " cache failed for Container {}", container.getName());
        }

        map = clusterContainerService.getCache(WORKSTATUSCACHE);
        if (map != null) {
            workStatus = (ConcurrentMap<FlowEntryDistributionOrder, Status>) map;
        } else {
            log.error("Retrieval of " + WORKSTATUSCACHE + " cache failed for Container {}", container.getName());
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
        // Configuration object validation
        Status status = config.validate(container);
        if (!status.isSuccess()) {
            log.warn("Invalid Configuration for flow {}. The failure is {}", config, status.getDescription());
            String error = "Invalid Configuration (" + status.getDescription() + ")";
            config.setStatus(error);
            return new Status(StatusCode.BADREQUEST, error);
        }
        return addStaticFlowInternal(config, false);
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
    private Status addStaticFlowInternal(FlowConfig config, boolean restore) {
        boolean multipleFlowPush = false;
        String error;
        Status status;
        config.setStatus(SUCCESS);

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
                status = this.installFlowEntry(entry);
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
                if (config.installInHw() && !config.getStatus().equals(SUCCESS)) {
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
                    config.setStatus(NODEDOWN);
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
                    config.setStatus(SUCCESS);
                    break;
                default:
                }
            }
        }
        // Update cluster cache
        refreshClusterStaticFlowsStatus(null);
    }

    @Override
    public Status removeStaticFlow(FlowConfig config) {
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
        Status status = this.uninstallFlowEntry(config.getFlowEntry());

        // Update configuration database if programming was successful
        if (status.isSuccess()) {
            staticFlows.remove(key);
        }

        return status;
    }

    @Override
    public Status removeStaticFlow(String name, Node node) {
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
        Status status = this.removeEntry(target.getFlowEntry(), false);

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
        Status status = newFlowConfig.validate(container);
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
            log.info("Static flow modification skipped. New flow and old flow are the same: {}", newFlowConfig);
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
            // Program the network node
            Status status = (target.installInHw()) ? this.uninstallFlowEntry(target.getFlowEntry()) : this
                    .installFlowEntry(target.getFlowEntry());
            if (status.isSuccess()) {
                // Update Configuration database
                target.setStatus(SUCCESS);
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
     * A copy of each entry is stored in the inactive list so that it can be
     * re-applied when needed. This function is called on the global instance of
     * FRM only, when the first container is created
     */
    private void uninstallAllFlowEntries() {
        log.info("Uninstalling all non-internal flows");

        // Store entries / create target list
        for (ConcurrentMap.Entry<FlowEntryInstall, FlowEntryInstall> mapEntry : installedSwView.entrySet()) {
            FlowEntryInstall flowEntries = mapEntry.getValue();
            // Skip internal generated static flows
            if (!flowEntries.isInternal()) {
                inactiveFlows.put(flowEntries.getOriginal(), flowEntries.getOriginal());
            }
        }

        // Now remove the entries
        for (FlowEntry flowEntry : inactiveFlows.keySet()) {
            Status status = this.removeEntry(flowEntry, false);
            if (!status.isSuccess()) {
                log.warn("Failed to remove entry: {}. The failure is: {}", flowEntry, status.getDescription());
            }
        }
    }

    /**
     * Re-install all the Flow Entries present in the inactive list The inactive
     * list will be empty at the end of this call This function is called on the
     * default container instance of FRM only when the last container is deleted
     */
    private void reinstallAllFlowEntries() {
        log.info("Reinstalling all inactive flows");

        for (FlowEntry flowEntry : this.inactiveFlows.keySet()) {
            this.addEntry(flowEntry, false);
        }

        // Empty inactive list in any case
        inactiveFlows.clear();
    }

    @Override
    public List<FlowConfig> getStaticFlows() {
        return getStaticFlowsOrderedList(staticFlows, staticFlowsOrdinal.get(0).intValue());
    }

    // TODO: need to come out with a better algorithm for maintaining the order
    // of the configuration entries
    // with actual one, index associated to deleted entries cannot be reused and
    // map grows...
    private List<FlowConfig> getStaticFlowsOrderedList(ConcurrentMap<Integer, FlowConfig> flowMap, int maxKey) {
        List<FlowConfig> orderedList = new ArrayList<FlowConfig>();
        for (int i = 0; i <= maxKey; i++) {
            FlowConfig entry = flowMap.get(i);
            if (entry != null) {
                orderedList.add(entry);
            }
        }
        return orderedList;
    }

    @Override
    public FlowConfig getStaticFlow(String name, Node node) {
        for (ConcurrentMap.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            if (entry.getValue().isByNameAndNodeIdEqual(name, node)) {
                return entry.getValue();
            }
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

    @SuppressWarnings("unchecked")
    private void loadFlowConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<Integer, FlowConfig> confList = (ConcurrentMap<Integer, FlowConfig>) objReader.read(this,
                frmFileName);

        ConcurrentMap<String, PortGroupConfig> pgConfig = (ConcurrentMap<String, PortGroupConfig>) objReader.read(this,
                portGroupFileName);

        if (pgConfig != null) {
            for (ConcurrentMap.Entry<String, PortGroupConfig> entry : pgConfig.entrySet()) {
                addPortGroupConfig(entry.getKey(), entry.getValue().getMatchString(), true);
            }
        }

        if (confList == null) {
            return;
        }

        int maxKey = 0;
        for (Integer key : confList.keySet()) {
            if (key.intValue() > maxKey) {
                maxKey = key.intValue();
            }
        }

        for (FlowConfig conf : getStaticFlowsOrderedList(confList, maxKey)) {
            addStaticFlowInternal(conf, true);
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
        ObjectWriter objWriter = new ObjectWriter();
        ConcurrentMap<Integer, FlowConfig> nonDynamicFlows = new ConcurrentHashMap<Integer, FlowConfig>();
        for (Integer ordinal : staticFlows.keySet()) {
            FlowConfig config = staticFlows.get(ordinal);
            // Do not save dynamic and controller generated static flows
            if (config.isDynamic() || config.isInternalFlow()) {
                continue;
            }
            nonDynamicFlows.put(ordinal, config);
        }
        objWriter.write(nonDynamicFlows, frmFileName);
        objWriter.write(new ConcurrentHashMap<String, PortGroupConfig>(portGroupConfigs), portGroupFileName);
        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public void subnetNotify(Subnet sub, boolean add) {
    }

    private void installImplicitARPReplyPunt(Node node) {

        if (node == null) {
            return;
        }

        List<String> puntAction = new ArrayList<String>();
        puntAction.add(ActionType.CONTROLLER.toString());

        FlowConfig allowARP = new FlowConfig();
        allowARP.setInstallInHw(true);
        allowARP.setName(FlowConfig.INTERNALSTATICFLOWBEGIN + "Punt ARP Reply" + FlowConfig.INTERNALSTATICFLOWEND);
        allowARP.setPriority("500");
        allowARP.setNode(node);
        allowARP.setEtherType("0x" + Integer.toHexString(EtherTypes.ARP.intValue()).toUpperCase());
        allowARP.setDstMac(HexEncode.bytesToHexString(switchManager.getControllerMAC()));
        allowARP.setActions(puntAction);
        addStaticFlowInternal(allowARP, true); // skip validation on internal static flow name
    }

    @Override
    public void modeChangeNotify(Node node, boolean proactive) {
        List<FlowConfig> defaultConfigs = new ArrayList<FlowConfig>();

        List<String> puntAction = new ArrayList<String>();
        puntAction.add(ActionType.CONTROLLER.toString());

        FlowConfig allowARP = new FlowConfig();
        allowARP.setInstallInHw(true);
        allowARP.setName(FlowConfig.INTERNALSTATICFLOWBEGIN + "Punt ARP" + FlowConfig.INTERNALSTATICFLOWEND);
        allowARP.setPriority("1");
        allowARP.setNode(node);
        allowARP.setEtherType("0x" + Integer.toHexString(EtherTypes.ARP.intValue()).toUpperCase());
        allowARP.setActions(puntAction);
        defaultConfigs.add(allowARP);

        FlowConfig allowLLDP = new FlowConfig();
        allowLLDP.setInstallInHw(true);
        allowLLDP.setName(FlowConfig.INTERNALSTATICFLOWBEGIN + "Punt LLDP" + FlowConfig.INTERNALSTATICFLOWEND);
        allowLLDP.setPriority("1");
        allowLLDP.setNode(node);
        allowLLDP.setEtherType("0x" + Integer.toHexString(EtherTypes.LLDP.intValue()).toUpperCase());
        allowLLDP.setActions(puntAction);
        defaultConfigs.add(allowLLDP);

        List<String> dropAction = new ArrayList<String>();
        dropAction.add(ActionType.DROP.toString());

        FlowConfig dropAllConfig = new FlowConfig();
        dropAllConfig.setInstallInHw(true);
        dropAllConfig.setName(FlowConfig.INTERNALSTATICFLOWBEGIN + "Catch-All Drop" + FlowConfig.INTERNALSTATICFLOWEND);
        dropAllConfig.setPriority("0");
        dropAllConfig.setNode(node);
        dropAllConfig.setActions(dropAction);
        defaultConfigs.add(dropAllConfig);

        log.info("Forwarding mode for node {} set to {}", node, (proactive ? "proactive" : "reactive"));
        for (FlowConfig fc : defaultConfigs) {
            Status status = (proactive) ? addStaticFlowInternal(fc, false) : removeStaticFlow(fc);
            if (status.isSuccess()) {
                log.info("{} Proactive Static flow: {}", (proactive ? "Installed" : "Removed"), fc.getName());
            } else {
                log.warn("Failed to {} Proactive Static flow: {}", (proactive ? "install" : "remove"), fc.getName());
            }
        }
    }

    /**
     * Remove from the databases all the flows installed on the node
     *
     * @param node
     */
    private void cleanDatabaseForNode(Node node) {
        log.info("Cleaning Flow database for Node {}", node);
        if (nodeFlows.containsKey(node)) {
            List<FlowEntryInstall> toRemove = new ArrayList<FlowEntryInstall>(nodeFlows.get(node));

            for (FlowEntryInstall entry : toRemove) {
                updateLocalDatabase(entry, false);
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
                    addStaticFlowInternal(derivedFlow, false);
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
        log.info("PortGroup Changed for: {} Data: {}", config, portGroupData);
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

    private void usePortGroupConfig(String name) {
        PortGroupConfig config = portGroupConfigs.get(name);
        if (config == null) {
            return;
        }
        if (portGroupProvider != null) {
            Map<Node, PortGroup> data = portGroupProvider.getPortGroupData(config);
            portGroupData.put(config, data);
        }
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
        frmFileName = GlobalConstants.STARTUPHOME.toString() + "frm_staticflows_" + this.getContainerName() + ".conf";
        portGroupFileName = GlobalConstants.STARTUPHOME.toString() + "portgroup_" + this.getContainerName() + ".conf";

        inContainerMode = false;

        if (portGroupProvider != null) {
            portGroupProvider.registerPortGroupChange(this);
        }

        cacheStartup();

        registerWithOSGIConsole();

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
                        FRMEvent event = pendingEvents.take();
                        if (event == null) {
                            log.warn("Dequeued null event");
                            continue;
                        }
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
                            WorkOrderEvent work = (WorkOrderEvent) event;
                            FlowEntryDistributionOrder fe = work.getFe();
                            if (fe != null) {
                                logsync.trace("Executing the workOrder {}", fe);
                                Status gotStatus = null;
                                FlowEntryInstall feiCurrent = fe.getEntry();
                                FlowEntryInstall feiNew = workOrder.get(fe.getEntry());
                                switch (fe.getUpType()) {
                                case ADDED:
                                    /*
                                     * TODO: Not still sure how to handle the
                                     * sync entries
                                     */
                                    gotStatus = addEntriesInternal(feiCurrent, true);
                                    break;
                                case CHANGED:
                                    gotStatus = modifyEntryInternal(feiCurrent, feiNew, true);
                                    break;
                                case REMOVED:
                                    gotStatus = removeEntryInternal(feiCurrent, true);
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
                        } else {
                            log.warn("Dequeued unknown event {}", event.getClass()
                                    .getSimpleName());
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
        // Initialize graceful stop flag
        stopping = false;

        // Start event handler thread
        frmEventHandler.start();

        /*
         * Read startup and build database if we have not already gotten the
         * configurations synced from another node
         */
        if (staticFlows.isEmpty()) {
            loadFlowConfiguration();
        }
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     */
    void stop() {
        stopping = true;
        uninstallAllFlowEntries();
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
        /*
         * Whether it is an addition or removal, we have to recompute the merged
         * flows entries taking into account all the current container flows
         * because flow merging is not an injective function
         */
        updateFlowsContainerFlow();
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector nc, UpdateType t) {
        if (!container.getName().equals(containerName)) {
            return;
        }

        boolean updateStaticFlowCluster = false;

        switch (t) {
        case REMOVED:

            List<FlowEntryInstall> nodeFlowEntries = nodeFlows.get(nc.getNode());
            if (nodeFlowEntries == null) {
                return;
            }
            for (FlowEntryInstall fei : new ArrayList<FlowEntryInstall>(nodeFlowEntries)) {
                if (doesFlowContainNodeConnector(fei.getInstall().getFlow(), nc)) {
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
                            flowConfig.setStatus(PORTREMOVED);
                            updateStaticFlowCluster = true;
                        }
                    }
                }
            }
            if (updateStaticFlowCluster) {
                refreshClusterStaticFlowsStatus(nc.getNode());
            }
            break;
        case ADDED:
            List<FlowConfig> flowConfigForNode = getStaticFlows(nc.getNode());
            for (FlowConfig flowConfig : flowConfigForNode) {
                if (doesFlowContainNodeConnector(flowConfig.getFlow(), nc)) {
                    if (flowConfig.installInHw()) {
                        Status status = this.installFlowEntry(flowConfig.getFlowEntry());
                        if (!status.isSuccess()) {
                            flowConfig.setStatus(status.getDescription());
                        } else {
                            flowConfig.setStatus(SUCCESS);
                        }
                        updateStaticFlowCluster = true;
                    }
                }
            }
            if (updateStaticFlowCluster) {
                refreshClusterStaticFlowsStatus(nc.getNode());
            }
            break;
        case CHANGED:
            break;
        default:
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
            this.inContainerMode = true;
            this.uninstallAllFlowEntries();
            break;
        case REMOVED:
            this.inContainerMode = false;
            this.reinstallAllFlowEntries();
            break;
        default:
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

    /*
     * OSGI COMMANDS
     */
    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---FRM Matrix Application---\n");
        help.append("\t printMatrixData        - Prints the Matrix Configs\n");
        help.append("\t addMatrixConfig <name> <regex>\n");
        help.append("\t delMatrixConfig <name>\n");
        help.append("\t useMatrixConfig <name>\n");
        return help.toString();
    }

    public void _printMatrixData(CommandInterpreter ci) {
        ci.println("Configs : ");
        ci.println("---------");
        ci.println(portGroupConfigs);

        ci.println("Data : ");
        ci.println("------");
        ci.println(portGroupData);
    }

    public void _addMatrixConfig(CommandInterpreter ci) {
        String name = ci.nextArgument();
        String regex = ci.nextArgument();
        addPortGroupConfig(name, regex, false);
    }

    public void _delMatrixConfig(CommandInterpreter ci) {
        String name = ci.nextArgument();
        delPortGroupConfig(name);
    }

    public void _useMatrixConfig(CommandInterpreter ci) {
        String name = ci.nextArgument();
        usePortGroupConfig(name);
    }

    public void _arpPunt(CommandInterpreter ci) {
        String switchId = ci.nextArgument();
        long swid = HexEncode.stringToLong(switchId);
        Node node = NodeCreator.createOFNode(swid);
        installImplicitARPReplyPunt(node);
    }

    public void _frmaddflow(CommandInterpreter ci) throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = NodeCreator.createOFNode(Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            ci.print("Node id not a number");
            return;
        }
        ci.println(this.programmer.addFlow(node, getSampleFlow(node)));
    }

    public void _frmremoveflow(CommandInterpreter ci) throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = NodeCreator.createOFNode(Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            ci.print("Node id not a number");
            return;
        }
        ci.println(this.programmer.removeFlow(node, getSampleFlow(node)));
    }

    private Flow getSampleFlow(Node node) throws UnknownHostException {
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector((short) 24, node);
        NodeConnector oport = NodeConnectorCreator.createOFNodeConnector((short) 30, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d, (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress.getByName("172.28.30.50");
        InetAddress dstIP = InetAddress.getByName("171.71.9.52");
        InetAddress ipMask = InetAddress.getByName("255.255.255.0");
        InetAddress ipMask2 = InetAddress.getByName("255.0.0.0");
        short ethertype = EtherTypes.IPv4.shortValue();
        short vlan = (short) 27;
        byte vlanPr = 3;
        Byte tos = 4;
        byte proto = IPProtocols.TCP.byteValue();
        short src = (short) 55000;
        short dst = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match = new Match();
        match.setField(MatchType.IN_PORT, port);
        match.setField(MatchType.DL_SRC, srcMac);
        match.setField(MatchType.DL_DST, dstMac);
        match.setField(MatchType.DL_TYPE, ethertype);
        match.setField(MatchType.DL_VLAN, vlan);
        match.setField(MatchType.DL_VLAN_PR, vlanPr);
        match.setField(MatchType.NW_SRC, srcIP, ipMask);
        match.setField(MatchType.NW_DST, dstIP, ipMask2);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src);
        match.setField(MatchType.TP_DST, dst);

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(oport));
        actions.add(new PopVlan());
        actions.add(new Flood());
        actions.add(new Controller());
        return new Flow(match, actions);
    }

    @Override
    public Status saveConfiguration() {
        return saveConfig();
    }

    public void _frmNodeFlows(CommandInterpreter ci) {
        String nodeId = ci.nextArgument();
        Node node = Node.fromString(nodeId);
        if (node == null) {
            ci.println("frmNodeFlows <node> [verbose]");
            return;
        }
        boolean verbose = false;
        String verboseCheck = ci.nextArgument();
        if (verboseCheck != null) {
            verbose = verboseCheck.equals("true");
        }

        if (!nodeFlows.containsKey(node)) {
            return;
        }
        // Dump per node database
        for (FlowEntryInstall entry : nodeFlows.get(node)) {
            if (!verbose) {
                ci.println(node + " " + installedSwView.get(entry).getFlowName());
            } else {
                ci.println(node + " " + installedSwView.get(entry).toString());
            }
        }
    }

    public void _frmGroupFlows(CommandInterpreter ci) {
        String group = ci.nextArgument();
        if (group == null) {
            ci.println("frmGroupFlows <group> [verbose]");
            return;
        }
        boolean verbose = false;
        String verboseCheck = ci.nextArgument();
        if (verboseCheck != null) {
            verbose = verboseCheck.equalsIgnoreCase("true");
        }

        if (!groupFlows.containsKey(group)) {
            return;
        }
        // Dump per node database
        ci.println("Group " + group + ":\n");
        for (FlowEntryInstall flowEntry : groupFlows.get(group)) {
            if (!verbose) {
                ci.println(flowEntry.getNode() + " " + flowEntry.getFlowName());
            } else {
                ci.println(flowEntry.getNode() + " " + flowEntry.toString());
            }
        }
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
            target.toggleInstallation();
            target.setStatus(SUCCESS);
            staticFlows.put(key, target);
        }

        // Update software views
        this.updateLocalDatabase(installedEntry, false);
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
        for (FlowEntryInstall index : nodeFlows.get(node)) {
            FlowEntryInstall entry = installedSwView.get(index);
            if (entry.getRequestId() == rid) {
                target = entry;
                break;
            }
        }
        if (target != null) {
            // This was a flow install, update database
            this.updateLocalDatabase(target, false);
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

    @Override
    public void entryCreated(Object key, String cacheName, boolean originLocal) {
        /*
         * Do nothing
         */
    }

    @Override
    public void entryUpdated(Object key, Object new_value, String cacheName, boolean originLocal) {
        if (originLocal) {
            /*
             * Local updates are of no interest
             */
            return;
        }
        if (cacheName.equals(WORKORDERCACHE)) {
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
            if (connectionManager.isLocal(n)) {
                logsync.trace("workOrder for fe {} processed locally", fe);
                // I'm the controller in charge for the request, queue it for
                // processing
                pendingEvents.offer(new WorkOrderEvent(fe, (FlowEntryInstall) new_value));
            }
        } else if (cacheName.equals(WORKSTATUSCACHE)) {
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
                FlowEntryDistributionOrderFutureTask fet = workMonitor.get(fe);
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
         * Do nothing
         */
    }
}
