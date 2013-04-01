
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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntryInstall;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManagerAware;
import org.opendaylight.controller.forwardingrulesmanager.PortGroup;
import org.opendaylight.controller.forwardingrulesmanager.PortGroupChangeListener;
import org.opendaylight.controller.forwardingrulesmanager.PortGroupConfig;
import org.opendaylight.controller.forwardingrulesmanager.PortGroupProvider;
import org.opendaylight.controller.hosttracker.IfIptoHost;
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
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.StatusCode;
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
public class ForwardingRulesManagerImpl implements IForwardingRulesManager,
        PortGroupChangeListener, IContainerListener, ISwitchManagerAware,
        IConfigurationContainerAware, IInventoryListener, IObjectReader,
        ICacheUpdateAware<Long, String>, CommandProvider {
    private static final String SAVE = "Save";
    private static final String NODEDOWN = "Node is Down";
    private static final Logger log = LoggerFactory
            .getLogger(ForwardingRulesManagerImpl.class);
    private Map<Long, String> flowsSaveEvent;
    private String frmFileName;
    private String portGroupFileName;
    private ConcurrentMap<Integer, FlowConfig> staticFlows;
    private ConcurrentMap<Integer, Integer> staticFlowsOrdinal;
    private ConcurrentMap<String, PortGroupConfig> portGroupConfigs;
    private ConcurrentMap<PortGroupConfig, Map<Node, PortGroup>> portGroupData;
    private ConcurrentMap<String, Object> TSPolicies;
    private boolean inContainerMode; // being used by default instance only
    /*
     * Flow database. It's the software view of what was installed on the
     * switch. It is indexed by node. For convenience a version indexed
     * by group name is also maintained. The core element is a class which
     * contains the flow entry pushed by the functional modules and the
     * respective container flow merged version. In absence of container
     * flows, the two flow entries are the same.
     */
    private ConcurrentMap<Node, Set<FlowEntryInstall>> nodeFlows;
    private ConcurrentMap<String, Set<FlowEntryInstall>> groupFlows;
    /*
     * Inactive flow list. This is for the global instance of FRM
     * It will contain all the flow entries which were installed on the
     * global container when the first container is created.
     */
    private List<FlowEntry> inactiveFlows;

    private IfIptoHost hostFinder;
    private IContainer container;
    private Set<IForwardingRulesManagerAware> frmAware;
    private PortGroupProvider portGroupProvider;
    private IFlowProgrammerService programmer;
    private IClusterContainerServices clusterContainerService = null;
    private ISwitchManager switchManager;

    /**
     * Adds a flow entry onto the network node
     * It runs various validity checks and derive the final container flows
     * merged entries that will be attempted to be installed
     *
     * @param flowEntry the original flow entry application requested to add
     * @return
     */
    private Status addEntry(FlowEntry flowEntry) {
        // Sanity Check
        if (flowEntry == null || flowEntry.getNode() == null) {
        	String msg = "Invalid FlowEntry";
            log.warn(msg + ": " + flowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        /*
         * Derive the container flow merged entries to install
         * In presence of N container flows, we may end up with
         * N different entries to install...
         */
        List<FlowEntryInstall> toInstallList = deriveInstallEntries(flowEntry
                .clone(), container.getContainerFlows());

        // Container Flow conflict Check
        if (toInstallList.isEmpty()) {
        	String msg = "Flow Entry conflicts with all Container Flows";
        	log.warn(msg);
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Derive the list of entries good to be installed
        List<FlowEntryInstall> toInstallSafe = new ArrayList<FlowEntryInstall>();
        for (FlowEntryInstall entry : toInstallList) {
            // Conflict Check: Verify new entry would not overwrite existing ones
            if (findMatch(entry.getInstall(), false) != null) {
                log.warn("Operation Rejected: A flow with same match " + 
                		"and priority exists on the target node");
                log.trace("Aborting to install " + entry);
                continue;
            }
            toInstallSafe.add(entry);
        }

        // Declare failure if all the container flow merged entries clash with existing entries
        if (toInstallSafe.size() == 0) {
        	String msg = "A flow with same match and priority exists " + 
        			"on the target node";
        	log.warn(msg);
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Try to install an entry at the time
        Status error = new Status(null, null);
        boolean oneSucceded = false;
        for (FlowEntryInstall installEntry : toInstallList) {

            // Install and update database
        	Status ret = addEntriesInternal(installEntry);

            if (ret.isSuccess()) {
                oneSucceded = true;
            } else {
                error = ret;
                log.warn("Failed to install the entry: " + ret.getDescription());
            }
        }

        return (oneSucceded) ? new Status(StatusCode.SUCCESS, null) : error;
    }

    /**
     * Given a flow entry and the list of container flows, it returns the list
     * of container flow merged flow entries good to be installed on this
     * container. If the list of container flows is null or empty, the install
     * entry list will contain only one entry, the original flow entry. If the
     * flow entry is  congruent with all the N container flows, then the output
     * install entry list will contain N entries. If the output list is empty,
     * it means the passed flow entry conflicts with all the container flows.
     *
     * @param cFlowList The list of container flows
     * @return the list of container flow merged entries good to be installed on this container
     */
    private List<FlowEntryInstall> deriveInstallEntries(FlowEntry request,
            List<ContainerFlow> cFlowList) {
        List<FlowEntryInstall> toInstallList = new ArrayList<FlowEntryInstall>(
                1);

        if (container.getContainerFlows() == null
                || container.getContainerFlows().isEmpty()) {
            // No container flows => entry good to be installed unchanged
            toInstallList.add(new FlowEntryInstall(request.clone(), null));
        } else {
            // Create the list of entries to be installed. If the flow entry is
            // not congruent with any container flow, no install entries will be created
            for (ContainerFlow cFlow : container.getContainerFlows()) {
                if (cFlow.allowsFlow(request.getFlow())) {
                    toInstallList.add(new FlowEntryInstall(request.clone(),
                            cFlow));
                }
            }
        }
        return toInstallList;
    }

    /**
     * Modify a flow entry with a new one
     * It runs various validity check and derive the final container flows
     * merged flow entries to work with
     *
     * @param currentFlowEntry
     * @param newFlowEntry
     * @return Success or error string
     */
    private Status modifyEntry(FlowEntry currentFlowEntry,
            FlowEntry newFlowEntry) {
    	Status retExt;

        // Sanity checks
        if (currentFlowEntry == null || currentFlowEntry.getNode() == null
                || newFlowEntry == null || newFlowEntry.getNode() == null) {
        	String msg ="Modify: Invalid FlowEntry";
            log.warn(msg + ": {} or {} ", currentFlowEntry, newFlowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }
        if (!currentFlowEntry.getNode().equals(newFlowEntry.getNode())
                || !currentFlowEntry.getFlowName().equals(
                        newFlowEntry.getFlowName())) {
        	String msg = "Modify: Incompatible Flow Entries";
            log.warn(msg +": {} and {}", currentFlowEntry, newFlowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        // Equality Check
        if (currentFlowEntry.equals(newFlowEntry)) {
        	String msg = "Modify skipped as flows are the same";
            log.debug(msg + ": " + currentFlowEntry + " and " + newFlowEntry);
            return new Status(StatusCode.SUCCESS, msg);
        }

        // Conflict Check: Verify the new entry would not conflict with another existing one
        // This is a loose check on the previous original flow entry requests. No check
        // on the container flow merged flow entries (if any) yet
        FlowEntryInstall sameMatchOriginalEntry = findMatch(newFlowEntry, true);
        if (sameMatchOriginalEntry != null
                && !sameMatchOriginalEntry.getOriginal().equals(
                        currentFlowEntry)) {
        	String msg = "Operation Rejected: Another flow with same match " +
        			"and priority exists on the target node";
            log.warn(msg);
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Derive the installed and toInstall entries
        List<FlowEntryInstall> installedList = deriveInstallEntries(
                currentFlowEntry.clone(), container.getContainerFlows());
        List<FlowEntryInstall> toInstallList = deriveInstallEntries(
                newFlowEntry.clone(), container.getContainerFlows());

        if (toInstallList.isEmpty()) {
        	String msg = "Modify Operation Rejected: The new entry " + 
        			"conflicts with all the container flows";
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
         * Modify path can be taken only if it can be performed completely,
         * for all entries.
         *
         * So, for the above two cases, to simplify, let's decouple the modify in:
         * 1) remove current entries
         * 2) install new entries
         */
        boolean decouple = false;
        if (installedList.size() != toInstallList.size()) {
            log.info("Modify: New flow entry does not satisfy the same " +
                    "number of container flows as the original entry does");
            decouple = true;
        }
        List<FlowEntryInstall> toInstallSafe = new ArrayList<FlowEntryInstall>();
        for (FlowEntryInstall installEntry : toInstallList) {
            // Conflict Check: Verify the new entry would not overwrite another existing one
            FlowEntryInstall sameMatchEntry = findMatch(installEntry
                    .getInstall(), false);
            if (sameMatchEntry != null
                    && !sameMatchEntry.getOriginal().equals(currentFlowEntry)) {
                log.info("Modify: new container flow merged flow entry " + 
                    "clashes with existing flow");
                decouple = true;
            } else {
                toInstallSafe.add(installEntry);
            }
        }

        if (decouple) {
            // Remove current entries
            for (FlowEntryInstall currEntry : installedList) {
                this.removeEntryInternal(currEntry);
            }
            // Install new entries
            for (FlowEntryInstall newEntry : toInstallSafe) {
                this.addEntriesInternal(newEntry);
            }
        } else {
            /*
             * The two list have the same size and the entries to install do not
             * clash with any existing flow on the network node. We assume here
             * (and might be wrong) that the same container flows that were satisfied
             * by the current entries are the same that are satisfied by the new
             * entries. Let's take the risk for now.
             *
             * Note: modification has to be complete. If any entry modification
             * fails, we need to stop, restore the already modified entries,
             * and declare failure.
             */
            Status retModify;
            int i = 0;
            int size = toInstallList.size();
            while (i < size) {
                // Modify and update database
                retModify = modifyEntryInternal(installedList.get(i),
                        toInstallList.get(i));
                if (retModify.isSuccess()) {
                    i++;
                } else {
                    break;
                }
            }
            // Check if uncompleted modify
            if (i < size) {
                log.warn("Unable to perform a complete modify for all " + 
                		"the container flows merged entries");
                // Restore original entries
                int j = 0;
                while (j < i) {
                    log.info("Attempting to restore initial entries");
                    retExt = modifyEntryInternal(toInstallList.get(i),
                            installedList.get(i));
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
        }
        return new Status(StatusCode.SUCCESS, null);
    }

    /**
     * This is the function that modifies the final container flows merged
     * entries on the network node and update the database. It expects that
     * all the  validity checks are passed
     *
     * @param currentEntries
     * @param newEntries
     * @return
     */
    private Status modifyEntryInternal(FlowEntryInstall currentEntries,
            FlowEntryInstall newEntries) {
        // Modify the flow on the network node
        Status status = programmer.modifyFlow(currentEntries.getNode(),
                currentEntries.getInstall().getFlow(), newEntries.getInstall()
                        .getFlow());

        if (!status.isSuccess()) {
            log.warn("SDN Plugin failed to program the flow: " + status.getDescription());
            return status;
        }

        log.trace("Modified {} => {}", currentEntries.getInstall(), newEntries
                .getInstall());

        // Update DB
        updateLocalDatabase(currentEntries, false);
        updateLocalDatabase(newEntries, true);

        return status;
    }

    /**
     * Remove a flow entry. If the entry is not present in the software view
     * (entry or node not present), it return successfully
     *
     * @param flowEntry
     * @return
     */
    private Status removeEntry(FlowEntry flowEntry) {
        Status error = new Status(null, null);
        
        // Sanity Check
        if (flowEntry == null || flowEntry.getNode() == null) {
        	String msg = "Invalid FlowEntry";
            log.warn(msg + ": " + flowEntry);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        // Derive the container flows merged installed entries
        List<FlowEntryInstall> installedList = deriveInstallEntries(flowEntry
                .clone(), container.getContainerFlows());

        Set<FlowEntryInstall> flowsOnNode = nodeFlows.get(flowEntry.getNode());
        boolean atLeastOneRemoved = false;
        for (FlowEntryInstall entry : installedList) {
            if (flowsOnNode == null) {
            	String msg = "Removal skipped (Node down)";
                log.debug(msg + " for flow entry " + flowEntry);
                return new Status(StatusCode.SUCCESS, msg);
            }
            if (!flowsOnNode.contains(entry)) {
                log.debug("Removal skipped (not present in software view) "
                        + "for flow entry " + flowEntry);

                if (installedList.size() == 1) {
                    // If we had only one entry to remove, we are done
                    return new Status(StatusCode.SUCCESS, null);
                } else {
                    continue;
                }
            }

            // Remove and update DB
            Status ret = removeEntryInternal(entry);

            if (!ret.isSuccess()) {
                error = ret;
                log.warn("Failed to remove the entry: " + ret.getDescription());
                if (installedList.size() == 1) {
                    // If we had only one entry to remove, this is fatal failure
                    return error;
                }
            } else {
                atLeastOneRemoved = true;
            }
        }

        /*
         * No worries if full removal failed. Consistency checker will
         * take care of removing the stale entries later, or adjusting
         * the software database if not in sync with hardware
         */
        return (atLeastOneRemoved) ? 
        		new Status(StatusCode.SUCCESS, null) : error;
    }

    /**
     * This is the function that removes the final container flows merged entry
     * from the network node and update the database. It expects that all the
     * validity checks are passed
     *
     * @param entry the FlowEntryInstall
     * @return "Success" or error string
     */
    private Status removeEntryInternal(FlowEntryInstall entry) {
        // Mark the entry to be deleted (for CC just in case we fail)
        entry.toBeDeleted();

        // Remove from node
        Status status = 
        		programmer.removeFlow(entry.getNode(), 
        				entry.getInstall().getFlow());

        if (!status.isSuccess()) {
            log.warn("SDN Plugin failed to remove the flow: " + 
            		status.getDescription());
            return status;
        }
        log.trace("Removed  {}", entry.getInstall());

        // Update DB
        updateLocalDatabase(entry, false);

        return status;
    }

    /**
     * This is the function that installs the final container flow merged entry
     * on the network node and updates the database. It expects that all the
     * validity and conflict checks are passed. That means it does not check
     * whether this flow would conflict or overwrite an existing one.
     *
     * @param entry the FlowEntryInstall
     * @return "Success" or error string
     */
    private Status addEntriesInternal(FlowEntryInstall entry) {
        // Install the flow on the network node
    	Status status = programmer.addFlow(entry.getNode(), 
    			entry.getInstall().getFlow());

        if (!status.isSuccess()) {
            log.warn("SDN Plugin failed to program the flow: " + 
            		status.getDescription());
            return status;
        }

        log.trace("Added    {}", entry.getInstall());

        // Update DB
        updateLocalDatabase(entry, true);

        return status;
    }

    /**
     * Returns true if the flow conflicts with all the container's flows.
     * This means that if the function returns true, the passed flow entry
     * is congruent with at least one container flow, hence it is good to
     * be installed on this container.
     *
     * @param flowEntry
     * @return true if flow conflicts with all the container flows, false otherwise
     */
    private boolean entryConflictsWithContainerFlows(FlowEntry flowEntry) {
        List<ContainerFlow> cFlowList = container.getContainerFlows();

        // Validity check and avoid unnecessary computation
        // Also takes care of default container where no container flows are present
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

    private synchronized void updateLocalDatabase(FlowEntryInstall entry,
            boolean add) {
        // Update node indexed flow database
        updateNodeFlowsDB(entry, add);

        // Update group indexed flow database
        updateGroupFlowsDB(entry, add);
    }

    /*
     * Update the node mapped flows database
     */
    private void updateNodeFlowsDB(FlowEntryInstall flowEntries, boolean add) {
        Node node = flowEntries.getNode();

        Set<FlowEntryInstall> flowEntrylist = this.nodeFlows.get(node);
        if (flowEntrylist == null) {
            if (add == false) {
                return;
            } else {
                flowEntrylist = new HashSet<FlowEntryInstall>();
            }
        }

        if (add == true) {
            flowEntrylist.add(flowEntries);
        } else {
            flowEntrylist.remove(flowEntries);
        }

        if (flowEntrylist.isEmpty()) {
            this.nodeFlows.remove(node);
        } else {
            this.nodeFlows.put(node, flowEntrylist);
        }
    }

    /*
     * Update the group name mapped flows database
     */
    private void updateGroupFlowsDB(FlowEntryInstall flowEntries, boolean add) {
        Set<FlowEntryInstall> flowList;
        FlowEntryInstall exists = null;
        String flowName = flowEntries.getFlowName();
        String groupName = flowEntries.getGroupName();

        if (this.groupFlows == null) {
            return;
        }

        // Flow may not be part of a group
        if (groupName == null) {
            return;
        }

        if (this.groupFlows.containsKey(groupName)) {
            flowList = this.groupFlows.get(groupName);
        } else {
            if (add == false) {
                return;
            } else {
                flowList = new HashSet<FlowEntryInstall>();
            }
        }

        for (FlowEntryInstall flow : flowList) {
            if (flow.equalsByNodeAndName(flowEntries.getNode(), flowName)) {
                exists = flow;
                break;
            }
        }

        if (exists == null && add == false) {
            return;
        }

        if (exists != null) {
            flowList.remove(exists);
        }

        if (add == true) {
            flowList.add(flowEntries);
        }

        if (flowList.isEmpty()) {
            this.groupFlows.remove(groupName);
        } else {
            this.groupFlows.put(groupName, flowList);
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
        for (FlowEntryInstall entry : this.nodeFlows.get(node)) {
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
        Status status = programmer.removeFlow(target.getNode(), target
                .getInstall().getFlow());

        // Update DB
        if (status.isSuccess()) {
            updateLocalDatabase(target, false);
        }

        return status;
    }

    @Override
    public Status installFlowEntry(FlowEntry flowEntry) {
    	Status status;
        if (inContainerMode) {
        	String msg = "Controller in container mode: Install Refused";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(msg);
        } else {
            status = addEntry(flowEntry);
        }
        return status;
    }

    @Override
    public Status uninstallFlowEntry(FlowEntry entry) {
    	Status status;
        if (inContainerMode) {
        	String msg = "Controller in container mode: Uninstall Refused";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(msg);
        } else {
        	status = removeEntry(entry);
        }
        return status;
    }

    @Override
    public Status modifyFlowEntry(FlowEntry currentFlowEntry,
            FlowEntry newFlowEntry) {
    	Status status = null;
        if (inContainerMode) {
            String msg = "Controller in container mode: Modify Refused";
            status = new Status(StatusCode.NOTACCEPTABLE, msg);
            log.warn(msg);
        } else {
            status = modifyEntry(currentFlowEntry, newFlowEntry);
        }
        return status;
    }

    @Override
    public Status modifyOrAddFlowEntry(FlowEntry newFlowEntry) {
        /*
         * Run a loose check on the installed entries to decide whether to go
         * with a add or modify method. A loose check means only check against
         * the original flow entry requests and not against the installed
         * flow entries which are the result of the original entry merged with
         * the container flow(s) (if any). The modifyFlowEntry method in
         * presence of conflicts with the Container flows (if any) would revert
         * back to a delete + add pattern
         */
        FlowEntryInstall currentFlowEntries = findMatch(newFlowEntry, true);

        if (currentFlowEntries != null) {
            return modifyFlowEntry(currentFlowEntries.getOriginal(),
                    newFlowEntry);
        } else {
            return installFlowEntry(newFlowEntry);
        }
    }

    /**
     * Try to find in the database if a Flow with the same Match and priority
     * of the passed one already exists for the specified network node.
     * Flow, priority and network node are all specified in the FlowEntry
     * If found, the respective FlowEntryInstall Object is returned
     *
     * @param flowEntry the FlowEntry to be tested against the ones installed
     * @param looseCheck if true, the function will run the check against the
     *          original flow entry portion of the installed entries
     * @return null if not found, otherwise the FlowEntryInstall which contains
     *          the existing flow entry
     */
    private FlowEntryInstall findMatch(FlowEntry flowEntry, boolean looseCheck) {
        Flow flow = flowEntry.getFlow();
        Match match = flow.getMatch();
        short priority = flow.getPriority();
        Set<FlowEntryInstall> thisNodeList = nodeFlows.get(flowEntry.getNode());

        if (thisNodeList != null) {
            for (FlowEntryInstall flowEntries : thisNodeList) {
                flow = (looseCheck == false) ? flowEntries.getInstall()
                        .getFlow() : flowEntries.getOriginal().getFlow();
                if (flow.getMatch().equals(match)
                        && flow.getPriority() == priority) {
                    return flowEntries;
                }
            }
        }
        return null;
    }

    public boolean checkFlowEntryConflict(FlowEntry flowEntry) {
        return entryConflictsWithContainerFlows(flowEntry);
    }

    /**
     * Updates all installed flows because the container flow got updated
     * This is obtained in two phases on per node basis:
     * 1) Uninstall of all flows
     * 2) Reinstall of all flows
     * This is needed because a new container flows merged flow may conflict with an existing
     * old container flows merged flow on the network node
     */
    private void updateFlowsContainerFlow() {
        List<FlowEntryInstall> oldCouples = new ArrayList<FlowEntryInstall>();
        List<FlowEntry> toReinstall = new ArrayList<FlowEntry>();
        for (Entry<Node, Set<FlowEntryInstall>> entry : this.nodeFlows
                .entrySet()) {
            oldCouples.clear();
            toReinstall.clear();
            if (entry.getValue() == null) {
                continue;
            }
            // Create a set of old entries and one of original entries to be reinstalled
            for (FlowEntryInstall oldCouple : entry.getValue()) {
                oldCouples.add(oldCouple);
                toReinstall.add(oldCouple.getOriginal());
            }
            // Remove the old couples. No validity checks to be run, use the internal remove
            for (FlowEntryInstall oldCouple : oldCouples) {
                this.removeEntryInternal(oldCouple);
            }
            // Reinstall the original flow entries, via the regular path: new cFlow merge + validations
            for (FlowEntry flowEntry : toReinstall) {
                this.installFlowEntry(flowEntry);
            }
        }
    }

    public void nonClusterObjectCreate() {
        nodeFlows = new ConcurrentHashMap<Node, Set<FlowEntryInstall>>();
        TSPolicies = new ConcurrentHashMap<String, Object>();
        groupFlows = new ConcurrentHashMap<String, Set<FlowEntryInstall>>();
        staticFlowsOrdinal = new ConcurrentHashMap<Integer, Integer>();
        portGroupConfigs = new ConcurrentHashMap<String, PortGroupConfig>();
        portGroupData = new ConcurrentHashMap<PortGroupConfig, Map<Node, PortGroup>>();
        staticFlows = new ConcurrentHashMap<Integer, FlowConfig>();
        flowsSaveEvent = new HashMap<Long, String>();
        inactiveFlows = new ArrayList<FlowEntry>(1);
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
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
                        log.error("Exception on callback", e);
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
        List<FlowEntry> list = null;
        if (this.groupFlows != null && this.groupFlows.containsKey(policyName)) {
            list = new ArrayList<FlowEntry>();
            for (FlowEntryInstall entries : groupFlows.get(policyName)) {
                list.add(entries.getOriginal());
            }
            return new ArrayList<FlowEntry>();
        }
        return list;
    }

    @Override
    public void addOutputPort(Node node, String flowName,
            List<NodeConnector> portList) {

        Set<FlowEntryInstall> flowEntryList = this.nodeFlows.get(node);

        for (FlowEntryInstall flow : flowEntryList) {
            if (flow.getFlowName().equals(flowName)) {
                FlowEntry currentFlowEntry = flow.getOriginal();
                FlowEntry newFlowEntry = currentFlowEntry.clone();
                for (NodeConnector dstPort : portList) {
                    newFlowEntry.getFlow().addAction(new Output(dstPort));
                }
                Status error = modifyEntry(currentFlowEntry, newFlowEntry);
                if (error.isSuccess()) {
                    log.info("Ports {} added to FlowEntry {}", portList,
                            flowName);
                } else {
                    log.warn("Failed to add ports {} to Flow entry {}: "
                            + error.getDescription(), portList, 
                            currentFlowEntry.toString());
                }
                return;
            }
        }
        log.warn("Failed to add ports to Flow {} on Node {}: Entry Not Found",
                flowName, node);
    }

    @Override
    public void removeOutputPort(Node node, String flowName,
            List<NodeConnector> portList) {

        Set<FlowEntryInstall> flowEntryList = this.nodeFlows.get(node);

        for (FlowEntryInstall flow : flowEntryList) {
            if (flow.getFlowName().equals(flowName)) {
                FlowEntry currentFlowEntry = flow.getOriginal();
                FlowEntry newFlowEntry = currentFlowEntry.clone();
                for (NodeConnector dstPort : portList) {
                    Action action = new Output(dstPort);
                    newFlowEntry.getFlow().removeAction(action);
                }
                Status status = modifyEntry(currentFlowEntry, newFlowEntry);
                if (status.isSuccess()) {
                    log.info("Ports {} removed from FlowEntry {}", portList,
                            flowName);
                } else {
                    log.warn("Failed to remove ports {} from Flow entry {}: "
                            + status.getDescription(), portList, 
                            currentFlowEntry.toString());
                }
                return;
            }
        }
        log
                .warn(
                        "Failed to remove ports from Flow {} on Node {}: Entry Not Found",
                        flowName, node);
    }

    /*
     * This function assumes the target flow has only one output port
     */
    @Override
    public void replaceOutputPort(Node node, String flowName,
            NodeConnector outPort) {
        FlowEntry currentFlowEntry = null;
        FlowEntry newFlowEntry = null;
        Set<FlowEntryInstall> flowEntryList = this.nodeFlows.get(node);

        // Find the flow
        for (FlowEntryInstall flow : flowEntryList) {
            if (flow.getFlowName().equals(flowName)) {
                currentFlowEntry = flow.getOriginal();
                break;
            }
        }
        if (currentFlowEntry == null) {
            log
                    .warn(
                            "Failed to replace output port for flow {} on node {}: Entry Not Found",
                            flowName, node);
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
        Status status = modifyEntry(currentFlowEntry, newFlowEntry);

        if (status.isSuccess()) {
            log.info("Output port replaced with " + outPort
                    + " for flow {} on node {}", flowName, node);
        } else {
            log.warn("Failed to replace output port for flow {} on node {}: ",
                    status.getDescription(), flowName, node);
        }
        return;
    }

    @Override
    public NodeConnector getOutputPort(Node node, String flowName) {
        Set<FlowEntryInstall> flowEntryList = this.nodeFlows.get(node);

        for (FlowEntryInstall flow : flowEntryList) {
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
            log
                    .warn("Un-initialized clusterContainerService, can't create cache");
            return;
        }

        log.debug("FRM allocateCaches for Container {}", container);

        try {
            clusterContainerService.createCache("frm.nodeFlows", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache("frm.groupFlows", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache("frm.staticFlows", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache("frm.flowsSaveEvent", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache("frm.staticFlowsOrdinal",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache("frm.portGroupConfigs", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache("frm.portGroupData", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

            clusterContainerService.createCache("frm.TSPolicies", EnumSet
                    .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

        } catch (CacheConfigException cce) {
            log.error("FRM CacheConfigException");
        } catch (CacheExistException cce) {
            log.error("FRM CacheExistException");
        }
    }

    @SuppressWarnings( { "unchecked", "deprecation" })
    private void retrieveCaches() {
        ConcurrentMap<?, ?> map;

        if (this.clusterContainerService == null) {
            log
                    .warn("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        log.debug("FRM retrieveCaches for Container {}", container);

        map = clusterContainerService.getCache("frm.nodeFlows");
        if (map != null) {
            nodeFlows = (ConcurrentMap<Node, Set<FlowEntryInstall>>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.nodeFlows allocation failed for Container {}",
                            container);
        }

        map = clusterContainerService.getCache("frm.groupFlows");
        if (map != null) {
            groupFlows = (ConcurrentMap<String, Set<FlowEntryInstall>>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.groupFlows allocation failed for Container {}",
                            container);
        }

        map = clusterContainerService.getCache("frm.staticFlows");
        if (map != null) {
            staticFlows = (ConcurrentMap<Integer, FlowConfig>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.staticFlows allocation failed for Container {}",
                            container);
        }

        map = clusterContainerService.getCache("frm.flowsSaveEvent");
        if (map != null) {
            flowsSaveEvent = (ConcurrentMap<Long, String>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.flowsSaveEvent allocation failed for Container {}",
                            container);
        }

        map = clusterContainerService.getCache("frm.staticFlowsOrdinal");
        if (map != null) {
            staticFlowsOrdinal = (ConcurrentMap<Integer, Integer>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.staticFlowsOrdinal allocation failed for Container {}",
                            container);
        }

        map = clusterContainerService.getCache("frm.portGroupConfigs");
        if (map != null) {
            portGroupConfigs = (ConcurrentMap<String, PortGroupConfig>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.portGroupConfigs allocation failed for Container {}",
                            container);
        }

        map = clusterContainerService.getCache("frm.portGroupData");
        if (map != null) {
            portGroupData = (ConcurrentMap<PortGroupConfig, Map<Node, PortGroup>>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.portGroupData allocation failed for Container {}",
                            container);
        }

        map = clusterContainerService.getCache("frm.TSPolicies");
        if (map != null) {
            TSPolicies = (ConcurrentMap<String, Object>) map;
        } else {
            log
                    .error(
                            "FRM Cache frm.TSPolicies allocation failed for Container {}",
                            container);
        }

    }

    @SuppressWarnings("deprecation")
	private void destroyCaches() {
        if (this.clusterContainerService == null) {
            log
                    .warn("Un-initialized clusterContainerService, can't destroy cache");
            return;
        }

        log.debug("FRM destroyCaches for Container {}", container);
        clusterContainerService.destroyCache("frm.nodeFlows");
        clusterContainerService.destroyCache("frm.TSPolicies");
        clusterContainerService.destroyCache("frm.groupFlows");
        clusterContainerService.destroyCache("frm.staticFlows");
        clusterContainerService.destroyCache("frm.flowsSaveEvent");
        clusterContainerService.destroyCache("frm.staticFlowsOrdinal");
        clusterContainerService.destroyCache("frm.portGroupData");
        clusterContainerService.destroyCache("frm.portGroupConfigs");
        nonClusterObjectCreate();
    }

    private boolean flowConfigExists(FlowConfig config) {
        // As per customer requirement, flow name has to be unique on per node
        // id basis
        for (FlowConfig fc : staticFlows.values()) {
            if (fc.isByNameAndNodeIdEqual(config)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Status addStaticFlow(FlowConfig config, boolean restore) {
        StringBuffer resultStr = new StringBuffer();
        boolean multipleFlowPush = false;
        String error;
        Status status;
        config.setStatus(StatusCode.SUCCESS.toString());

        // Presence check
        if (flowConfigExists(config)) {
            error = "Entry with this name on specified switch already exists";
            config.setStatus(error);
            return new Status(StatusCode.CONFLICT, error);
        }

        // Skip validation check if we are trying to restore a saved config
        if (!restore && !config.isValid(container, resultStr)) {
            log.debug(resultStr.toString());
            error = "Invalid Configuration (" + resultStr.toString() + ")";
            config.setStatus(error);
            return new Status(StatusCode.BADREQUEST, error);
        }

        if ((config.getIngressPort() == null) && config.getPortGroup() != null) {
            for (String portGroupName : portGroupConfigs.keySet()) {
                if (portGroupName.equalsIgnoreCase(config.getPortGroup())) {
                    multipleFlowPush = true;
                    break;
                }
            }
            if (!multipleFlowPush) {
                log.debug(resultStr.toString());
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
                status = this.addEntry(entry);
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
            PortGroupConfig pgconfig = portGroupConfigs.get(config
                    .getPortGroup());
            Map<Node, PortGroup> existingData = portGroupData.get(pgconfig);
            if (existingData != null) {
                portGroupChanged(pgconfig, existingData, true);
            }
        }
        return new Status(StatusCode.SUCCESS, null);
    }

    private void addStaticFlowsToSwitch(Node node) {
        for (FlowConfig config : staticFlows.values()) {
            if (config.isPortGroupEnabled()) {
                continue;
            }
            if (config.getNode().equals(node)) {
                if (config.installInHw()
                        && !config.getStatus().equals(
                        		StatusCode.SUCCESS.toString())) {
                    Status status = this.addEntry(config.getFlowEntry());
                    config.setStatus(status.getDescription());
                }
            }
        }
    }

    private void updateStaticFlowConfigsOnNodeDown(Node node) {
        log.trace("Updating Static Flow configs on node down: " + node);

        List<Integer> toRemove = new ArrayList<Integer>();
        for (Entry<Integer,FlowConfig> entry : staticFlows.entrySet()) {

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
    }

    private void updateStaticFlowConfigsOnContainerModeChange(UpdateType update) {
        log.trace("Updating Static Flow configs on container mode change: "
                + update);

        for (FlowConfig config : staticFlows.values()) {
            if (config.isPortGroupEnabled()) {
                continue;
            }
            if (config.installInHw()) {
                switch (update) {
                case ADDED:
                    config
                            .setStatus("Removed from node because in container mode");
                    break;
                case REMOVED:
                    config.setStatus(StatusCode.SUCCESS.toString());
                    break;
                default:
                }
            }
        }
    }

    public Status removeStaticFlow(FlowConfig config) {
        /*
         * No config.isInternal() check as NB does not take this path and GUI
         * cannot issue a delete on an internal generated flow. We need this path
         * to be accessible when switch mode is changed from proactive to
         * reactive, so that we can remove the internal generated LLDP and ARP
         * punt flows
         */
        for (Map.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            if (entry.getValue().isByNameAndNodeIdEqual(config)) {
                // Program the network node
            	Status status = this.removeEntry(config.getFlowEntry());
                // Update configuration database if programming was successful
                if (status.isSuccess()) {
                    staticFlows.remove(entry.getKey());
                    return status;
                } else {
                    entry.getValue().setStatus(status.getDescription());
                    return status;
                }
            }
        }
        return new Status(StatusCode.NOTFOUND, "Entry Not Present");
    }

    @Override
    public Status removeStaticFlow(String name, Node node) {
        for (Map.Entry<Integer, FlowConfig> mapEntry : staticFlows.entrySet()) {
            FlowConfig entry = mapEntry.getValue();
            Status status = new Status(null,null);
            if (entry.isByNameAndNodeIdEqual(name, node)) {
                // Validity check for api3 entry point
                if (entry.isInternalFlow()) {
                	String msg = "Invalid operation: Controller generated " +
                			"flow cannot be deleted";
	            	log.warn(msg);
	                return new Status(StatusCode.NOTACCEPTABLE, msg);
                }
                if (!entry.isPortGroupEnabled()) {
                    // Program the network node
                    status = this.removeEntry(entry.getFlowEntry());
                }
                // Update configuration database if programming was successful
                if (status.isSuccess()) {
                    staticFlows.remove(mapEntry.getKey());
                    return status;
                } else {
                    entry.setStatus(status.getDescription());
                    return status;
                }
            }
        }
        return new Status(StatusCode.NOTFOUND, "Entry Not Present");
    }

    public Status modifyStaticFlow(FlowConfig newFlowConfig) {
        // Validity check for api3 entry point
        if (newFlowConfig.isInternalFlow()) {
        	String msg = "Invalid operation: Controller generated flow " + 
        				"cannot be modified";
        	log.warn(msg);
            return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        // Validity Check
        StringBuffer resultStr = new StringBuffer();
        if (!newFlowConfig.isValid(container, resultStr)) {
            String msg = "Invalid Configuration (" + resultStr.toString()
                    + ")";
            newFlowConfig.setStatus(msg);
            log.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        FlowConfig oldFlowConfig = null;
        Integer index = null;
        for (Map.Entry<Integer, FlowConfig> mapEntry : staticFlows.entrySet()) {
            FlowConfig entry = mapEntry.getValue();
            if (entry.isByNameAndNodeIdEqual(newFlowConfig.getName(),
                    newFlowConfig.getNode())) {
                oldFlowConfig = entry;
                index = mapEntry.getKey();
                break;
            }
        }

        if (oldFlowConfig == null) {
        	String msg = "Attempt to modify a non existing static flow";
        	log.warn(msg);
        	return new Status(StatusCode.NOTFOUND, msg);
        }

        // Do not attempt to reinstall the flow, warn user
        if (newFlowConfig.equals(oldFlowConfig)) {
        	String msg = "No modification detected";
        	log.info("Static flow modification skipped: " + msg);
            return new Status(StatusCode.SUCCESS, msg);
        }

        // If flow is installed, program the network node
        Status status = new Status(StatusCode.SUCCESS, "Saved in config");
        if (oldFlowConfig.installInHw()) {
        	status = this.modifyEntry(oldFlowConfig.getFlowEntry(),
                    newFlowConfig.getFlowEntry());
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
        	String msg = "Invalid operation: Controller generated flow " +
    				"cannot be modified";
        	log.warn(msg);
        	return new Status(StatusCode.NOTACCEPTABLE, msg);
        }

        for (Map.Entry<Integer, FlowConfig> entry : staticFlows.entrySet()) {
            FlowConfig conf = entry.getValue();
            if (conf.isByNameAndNodeIdEqual(config)) {
                // Program the network node
                Status status = new Status(StatusCode.SUCCESS, null);
                if (conf.installInHw()) {
                    status = this.removeEntry(conf.getFlowEntry());
                } else {
                    status = this.addEntry(conf.getFlowEntry());
                }
                if (!status.isSuccess()) {
                    conf.setStatus(status.getDescription());
                    return status;
                }

                // Update Configuration database
                conf.setStatus(StatusCode.SUCCESS.toString());
                conf.toggleStatus();
                return status;
            }
        }
        return new Status(StatusCode.NOTFOUND,
        		"Unable to locate the entry. Failed to toggle status");
    }

    /**
     * Uninstall all the Flow Entries present in the software view
     * A copy of each entry is stored in the inactive list so
     * that it can be re-applied when needed
     * This function is called on the default container instance of FRM only
     * when the first container is created
     */
    private void uninstallAllFlowEntries() {
        log.info("Uninstalling all flows");

        // Store entries / create target list
        for (ConcurrentMap.Entry<Node, Set<FlowEntryInstall>> mapEntry : nodeFlows
                .entrySet()) {
            for (FlowEntryInstall flowEntries : mapEntry.getValue()) {
                inactiveFlows.add(flowEntries.getOriginal());
            }
        }

        // Now remove the entries
        for (FlowEntry flowEntry : inactiveFlows) {
            Status status = this.removeEntry(flowEntry);
            if (!status.isSuccess()) {
                log.warn("Failed to remove entry: {}: " + 
                		status.getDescription(), flowEntry);
            }
        }
    }

    /**
     * Re-install all the Flow Entries present in the inactive list
     * The inactive list will be empty at the end of this call
     * This function is called on the default container instance of FRM only
     * when the last container is deleted
     */
    private void reinstallAllFlowEntries() {
        log.info("Reinstalling all inactive flows");

        for (FlowEntry flowEntry : this.inactiveFlows) {
        	Status status = this.addEntry(flowEntry);
            if (!status.isSuccess()) {
                log.warn("Failed to install entry: {}: " + 
                		status.getDescription(), flowEntry);
            }
        }

        // Empty inactive list in any case
        inactiveFlows.clear();
    }

    public List<FlowConfig> getStaticFlows() {
        return getStaticFlowsOrderedList(staticFlows, staticFlowsOrdinal.get(0)
                .intValue());
    }

    // TODO: need to come out with a better algorithm for mantaining the order
    // of the configuration entries
    // with actual one, index associated to deleted entries cannot be reused and
    // map grows...
    private List<FlowConfig> getStaticFlowsOrderedList(
            ConcurrentMap<Integer, FlowConfig> flowMap, int maxKey) {
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
        for (FlowConfig config : staticFlows.values()) {
            if (config.isByNameAndNodeIdEqual(name, node)) {
                return config;
            }
        }
        return null;
    }

    @Override
    public List<FlowConfig> getStaticFlows(Node node) {
        List<FlowConfig> list = new ArrayList<FlowConfig>();
        for (FlowConfig config : staticFlows.values()) {
            if (config.onNode(node)) {
                list.add(config);
            }
        }
        return list;
    }

    @Override
    public List<String> getStaticFlowNamesForNode(Node node) {
        List<String> list = new ArrayList<String>();
        for (FlowConfig config : staticFlows.values()) {
            if (config.onNode(node)) {
                list.add(config.getName());
            }
        }
        return list;
    }

    @Override
    public List<Node> getListNodeWithConfiguredFlows() {
        Set<Node> set = new HashSet<Node>();
        for (FlowConfig config : staticFlows.values()) {
            set.add(config.getNode());
        }
        return new ArrayList<Node>(set);
    }

    @SuppressWarnings("unchecked")
    private void loadFlowConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<Integer, FlowConfig> confList = (ConcurrentMap<Integer, FlowConfig>) objReader
                .read(this, frmFileName);

        ConcurrentMap<String, PortGroupConfig> pgConfig = (ConcurrentMap<String, PortGroupConfig>) objReader
                .read(this, portGroupFileName);

        if (pgConfig != null) {
            for (Map.Entry<String, PortGroupConfig> entry : pgConfig.entrySet()) {
                addPortGroupConfig(entry.getKey(), entry.getValue()
                        .getMatchString(), true);
            }
        }

        if (confList == null) {
            return;
        }

        int maxKey = 0;
        for (Integer key : confList.keySet()) {
            if (key.intValue() > maxKey)
                maxKey = key.intValue();
        }

        for (FlowConfig conf : getStaticFlowsOrderedList(confList, maxKey)) {
            addStaticFlow(conf, true);
        }
    }

    @Override
    public Object readObject(ObjectInputStream ois)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }

    public Status saveConfig() {
        // Publish the save config event to the cluster nodes
        flowsSaveEvent.put(new Date().getTime(), SAVE);
        return saveConfigInternal();
    }

    private Status saveConfigInternal() {
        ObjectWriter objWriter = new ObjectWriter();
        ConcurrentHashMap<Integer, FlowConfig> nonDynamicFlows = new ConcurrentHashMap<Integer, FlowConfig>();
        for (Integer ordinal : staticFlows.keySet()) {
            FlowConfig config = staticFlows.get(ordinal);
            // Do not save dynamic and controller generated static flows
            if (config.isDynamic() || config.isInternalFlow()) {
                continue;
            }
            nonDynamicFlows.put(ordinal, config);
        }
        objWriter.write(nonDynamicFlows, frmFileName);
        objWriter.write(new ConcurrentHashMap<String, PortGroupConfig>(
                portGroupConfigs), portGroupFileName);
        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public void entryCreated(Long key, String cacheName, boolean local) {
    }

    @Override
    public void entryUpdated(Long key, String new_value, String cacheName,
            boolean originLocal) {
        saveConfigInternal();
    }

    @Override
    public void entryDeleted(Long key, String cacheName, boolean originLocal) {
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
        allowARP.setName("**Punt ARP Reply");
        allowARP.setPriority("500");
        allowARP.setNode(node);
        allowARP.setEtherType("0x"
                + Integer.toHexString(EtherTypes.ARP.intValue()).toUpperCase());
        allowARP.setDstMac(HexEncode.bytesToHexString(switchManager
                .getControllerMAC()));
        allowARP.setActions(puntAction);
        addStaticFlow(allowARP, false);
    }

    @Override
    public void modeChangeNotify(Node node, boolean proactive) {
        List<FlowConfig> defaultConfigs = new ArrayList<FlowConfig>();

        List<String> puntAction = new ArrayList<String>();
        puntAction.add(ActionType.CONTROLLER.toString());

        FlowConfig allowARP = new FlowConfig();
        allowARP.setInstallInHw(true);
        allowARP.setName("**Punt ARP");
        allowARP.setPriority("1");
        allowARP.setNode(node);
        allowARP.setEtherType("0x"
                + Integer.toHexString(EtherTypes.ARP.intValue()).toUpperCase());
        allowARP.setActions(puntAction);
        defaultConfigs.add(allowARP);

        FlowConfig allowLLDP = new FlowConfig();
        allowLLDP.setInstallInHw(true);
        allowLLDP.setName("**Punt LLDP");
        allowLLDP.setPriority("1");
        allowLLDP.setNode(node);
        allowLLDP.setEtherType("0x"
                               + Integer.toHexString(EtherTypes.LLDP.intValue())
                                .toUpperCase());
        allowLLDP.setActions(puntAction);
        defaultConfigs.add(allowLLDP);

        List<String> dropAction = new ArrayList<String>();
        dropAction.add(ActionType.DROP.toString());

        FlowConfig dropAllConfig = new FlowConfig();
        dropAllConfig.setInstallInHw(true);
        dropAllConfig.setName("**Catch-All Drop");
        dropAllConfig.setPriority("0");
        dropAllConfig.setNode(node);
        dropAllConfig.setActions(dropAction);
        defaultConfigs.add(dropAllConfig);

        for (FlowConfig fc : defaultConfigs) {
            if (proactive) {
                addStaticFlow(fc, false);
            } else {
                removeStaticFlow(fc);
            }
        }

        log.info("Set Switch {} Mode to {}", node, proactive);
    }

    /**
     * Remove from the databases all the flows installed on the node
     *
     * @param node
     */
    private synchronized void cleanDatabaseForNode(Node node) {
        log.info("Cleaning Flow database for Node " + node.toString());

        // Find out which groups the node's flows are part of
        Set<String> affectedGroups = new HashSet<String>();
        Set<FlowEntryInstall> flowEntryList = nodeFlows.get(node);
        if (flowEntryList != null) {
            for (FlowEntryInstall entry : flowEntryList) {
                String groupName = entry.getGroupName();
                if (groupName != null) {
                    affectedGroups.add(groupName);
                }
            }
        }

        // Remove the node's flows from the group indexed flow database
        if (!affectedGroups.isEmpty()) {
            for (String group : affectedGroups) {
                Set<FlowEntryInstall> flowList = groupFlows.get(group);
                Set<FlowEntryInstall> toRemove = new HashSet<FlowEntryInstall>();
                for (FlowEntryInstall entry : flowList) {
                    if (node.equals(entry.getNode())) {
                        toRemove.add(entry);
                    }
                }
                flowList.removeAll(toRemove);
                if (flowList.isEmpty()) {
                    groupFlows.remove(group);
                }
            }
        }

        // Remove the node's flows from the node indexed flow database
        nodeFlows.remove(node);
    }

    @Override
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        switch (type) {
        case ADDED: 
            addStaticFlowsToSwitch(node);
            break;
        case REMOVED:        	
            cleanDatabaseForNode(node);
            updateStaticFlowConfigsOnNodeDown(node);
            break;
        default:
            break;
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector,
    		UpdateType type, Map<String, Property> propMap) {
    }

    private FlowConfig getDerivedFlowConfig(FlowConfig original,
            String configName, Short port) {
        FlowConfig derivedFlow = new FlowConfig(original);
        derivedFlow.setDynamic(true);
        derivedFlow.setPortGroup(null);
        derivedFlow.setName(original.getName() + "_" + configName + "_" + port);
        derivedFlow.setIngressPort(port + "");
        return derivedFlow;
    }

    private void addPortGroupFlows(PortGroupConfig config, Node node,
            PortGroup data) {
        for (Iterator<FlowConfig> it = staticFlows.values().iterator(); it
                .hasNext();) {
            FlowConfig staticFlow = it.next();
            if (staticFlow.getPortGroup() == null) {
                continue;
            }
            if ((staticFlow.getNode().equals(node))
                    && (staticFlow.getPortGroup().equals(config.getName()))) {
                for (Short port : data.getPorts()) {
                    FlowConfig derivedFlow = getDerivedFlowConfig(staticFlow,
                            config.getName(), port);
                    addStaticFlow(derivedFlow, false);
                }
            }
        }
    }

    private void removePortGroupFlows(PortGroupConfig config, Node node,
            PortGroup data) {
        for (Iterator<FlowConfig> it = staticFlows.values().iterator(); it
                .hasNext();) {
            FlowConfig staticFlow = it.next();
            if (staticFlow.getPortGroup() == null) {
                continue;
            }
            if ((staticFlow.getNode().equals(node))
                    && (staticFlow.getPortGroup().equals(config.getName()))) {
                for (Short port : data.getPorts()) {
                    FlowConfig derivedFlow = getDerivedFlowConfig(staticFlow,
                            config.getName(), port);
                    removeStaticFlow(derivedFlow);
                }
            }
        }
    }

    @Override
    public void portGroupChanged(PortGroupConfig config,
            Map<Node, PortGroup> data, boolean add) {
        log.info("PortGroup Changed for :" + config + " Data: "
                 + portGroupData);
        Map<Node, PortGroup> existingData = portGroupData.get(config);
        if (existingData != null) {
            for (Map.Entry<Node, PortGroup> entry : data.entrySet()) {
                PortGroup existingPortGroup = existingData.get(entry.getKey());
                if (existingPortGroup == null) {
                    if (add) {
                        existingData.put(entry.getKey(), entry.getValue());
                        addPortGroupFlows(config, entry.getKey(), entry
                                .getValue());
                    }
                } else {
                    if (add) {
                        existingPortGroup.getPorts().addAll(
                                entry.getValue().getPorts());
                        addPortGroupFlows(config, entry.getKey(), entry
                                .getValue());
                    } else {
                        existingPortGroup.getPorts().removeAll(
                                entry.getValue().getPorts());
                        removePortGroupFlows(config, entry.getKey(), entry
                                .getValue());
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

    public boolean addPortGroupConfig(String name, String regex, boolean restore) {
        PortGroupConfig config = portGroupConfigs.get(name);
        if (config != null)
            return false;

        if ((portGroupProvider == null) && !restore) {
            return false;
        }
        if ((portGroupProvider != null)
                && (!portGroupProvider.isMatchCriteriaSupported(regex))) {
            return false;
        }

        config = new PortGroupConfig(name, regex);
        portGroupConfigs.put(name, config);
        if (portGroupProvider != null) {
            portGroupProvider.createPortGroupConfig(config);
        }
        return true;
    }

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
            Map<Node, PortGroup> data = portGroupProvider
                    .getPortGroupData(config);
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

    // Fir PortGroupProvider to use regular Dependency Manager
    /* @SuppressWarnings("rawtypes") */
    /* public void bind(Object arg0, Map arg1) throws Exception { */
    /* if (arg0 instanceof PortGroupProvider) { */
    /* setPortGroupProvider((PortGroupProvider)arg0); */
    /* } */
    /* } */

    /* @SuppressWarnings("rawtypes") */
    /* @Override */
    /* public void unbind(Object arg0, Map arg1) throws Exception { */
    /* if (arg0 instanceof PortGroupProvider) { */
    /* portGroupProvider = null; */
    /* } */
    /* } */

    public void setIContainer(IContainer s) {
        this.container = s;
    }

    public void unsetIContainer(IContainer s) {
        if (this.container == s) {
            this.container = null;
        }
    }

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

    public void setHostFinder(IfIptoHost hostFinder) {
        this.hostFinder = hostFinder;
    }

    public void unsetHostFinder(IfIptoHost hostFinder) {
        if (this.hostFinder == hostFinder) {
            this.hostFinder = null;
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
        frmAware = Collections
                .synchronizedSet(new HashSet<IForwardingRulesManagerAware>());
        frmFileName = GlobalConstants.STARTUPHOME.toString() + "frm_staticflows_"
                + this.getContainerName() + ".conf";
        portGroupFileName = GlobalConstants.STARTUPHOME.toString() + "portgroup_"
                + this.getContainerName() + ".conf";

        inContainerMode = false;

        if (portGroupProvider != null) {
            portGroupProvider.registerPortGroupChange(this);
        }

        nonClusterObjectCreate();

        cacheStartup();

        registerWithOSGIConsole();

        /*
         * If we are not the first cluster node to come up, do not initialize
         * the static flow entries ordinal
         */
        if (staticFlowsOrdinal.size() == 0) {
            staticFlowsOrdinal.put(0, Integer.valueOf(0));
        }
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        destroyCaches();
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
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
     *
     */
    void stop() {
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
    public void tagUpdated(String containerName, Node n, short oldTag,
            short newTag, UpdateType t) {

    }

    @Override
    public void containerFlowUpdated(String containerName,
            ContainerFlow previousFlow, ContainerFlow currentFlow, UpdateType t) {
        /*
         * Whether it is an addition or removal, we have to recompute the
         * merged flows entries taking into account all the current container flows
         * because flow merging is not an injective function
         */
        updateFlowsContainerFlow();
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector p,
            UpdateType t) {
        // No action
    }

    @Override
    public void containerModeUpdated(UpdateType update) {
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
            e.printStackTrace();
        }
        ci.println(this.programmer.addFlow(node, getSampleFlow(node)));
    }

    public void _frmremoveflow(CommandInterpreter ci)
            throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = NodeCreator.createOFNode(Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        ci.println(this.programmer.removeFlow(node, getSampleFlow(node)));
    }

    private Flow getSampleFlow(Node node) throws UnknownHostException {
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        NodeConnector oport = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
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
        return  saveConfig();
    }

    public void _frmNodeFlows(CommandInterpreter ci) {
        boolean verbose = false;
        String verboseCheck = ci.nextArgument();
        if (verboseCheck != null) {
            verbose = verboseCheck.equals("true");
        }

        // Dump per node database
        for (Entry<Node, Set<FlowEntryInstall>> entry : this.nodeFlows
                .entrySet()) {
            Node node = entry.getKey();
            for (FlowEntryInstall flow : entry.getValue()) {
                if (!verbose) {
                    ci.println(node + " " + flow.getFlowName());
                } else {
                    ci.println(node + " " + flow.toString());
                }
            }
        }
    }

    public void _frmGroupFlows(CommandInterpreter ci) {
        boolean verbose = false;
        String verboseCheck = ci.nextArgument();
        if (verboseCheck != null) {
            verbose = verboseCheck.equalsIgnoreCase("true");
        }

        // Dump per node database
        for (Entry<String, Set<FlowEntryInstall>> entry : this.groupFlows
                .entrySet()) {
            String group = entry.getKey();
            ci.println("Group " + group + ":");
            for (FlowEntryInstall flow : entry.getValue()) {
                if (!verbose) {
                    ci.println(flow.getNode() + " " + flow.getFlowName());
                } else {
                    ci.println(flow.getNode() + " " + flow.toString());
                }
            }
        }
    }

}
