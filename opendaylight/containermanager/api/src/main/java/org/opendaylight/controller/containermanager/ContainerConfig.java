
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * Container Configuration Java Object for Container Manager Represents a container
 * configuration information for Container Manager.
 *
 * Objects of this class are also serialized to and deserialized from binary
 * files through java serialization API when saving to/reading from Container
 * Manager startup configuration file.
 */
@XmlRootElement(name = "containerConfig")
@XmlAccessorType(XmlAccessType.NONE)
public class ContainerConfig implements Serializable {
    private static final long serialVersionUID = 2L;
    private static final String regexName = "^\\w+$";
    private static final String containerProfile = System.getProperty("container.profile") == null ? "Container"
            : System.getProperty("container.profile");
    private static final String ADMIN_SUFFIX = "Admin";
    private static final String OPERATOR_SUFFIX = "Operator";

    @XmlElement
    private String container;

    @XmlElement
    private String staticVlan;

    @XmlElement(name = "nodeConnectors")
    private List<String> ports;

    @XmlElement(name = "flowSpecs")
    private List<ContainerFlowConfig> containerFlows;

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getStaticVlan() {
        return staticVlan;
    }

    public void setStaticVlan(String staticVlan) {
        this.staticVlan = staticVlan;
    }

    public List<ContainerFlowConfig> getContainerFlows() {
        return containerFlows;
    }

    public void setContainerFlows(List<ContainerFlowConfig> containerFlows) {
        this.containerFlows = containerFlows;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public static String getRegexname() {
        return regexName;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    /**
     * Default constructor needed by Gson.
     *
     * @return a Default ContainerConfig
     */
    public ContainerConfig() {
        this.container = null;
        this.staticVlan = null;
        this.ports = new ArrayList<String>(0);
        this.containerFlows = new ArrayList<ContainerFlowConfig>(0);
    }

    /**
     * Constructor for the ContainerConfig.
     *
     * @param container
     *            Name of the container in this configuration
     * @param vlan
     *            vlan assigned to this container
     * @param nodeName
     *            the name of the node assigned to the container from this
     *            configuration
     * @param ports
     *            the list of NodeConnectors on the Node belonging to the container
     * @return the constructed object
     */
    public ContainerConfig(String container, String vlan, List<String> portList, List<ContainerFlowConfig> containerFlows) {
        this.container = container;
        this.staticVlan = vlan;
        this.ports = (portList == null) ? new ArrayList<String>(0) : new ArrayList<String>(portList);
        this.containerFlows = (containerFlows == null) ? new ArrayList<ContainerFlowConfig>(0)
                : new ArrayList<ContainerFlowConfig>(containerFlows);
    }

    public ContainerConfig(ContainerConfig config) {
        this.container = config.container;
        this.staticVlan = config.staticVlan;
        this.ports = (config.ports == null) ? new ArrayList<String>(0) : new ArrayList<String>(config.ports);
        this.containerFlows = (config.containerFlows == null) ? new ArrayList<ContainerFlowConfig>(0)
                : new ArrayList<ContainerFlowConfig>(config.containerFlows);
    }

    /**
     * Returns the container name.
     *
     * @return the container name
     */
    public String getContainerName() {
        return container;
    }

    /**
     * Returns the Vlan tag.
     *
     * @return the Vlan Tag configured for this container configuration
     */
    public String getVlanTag() {
        return staticVlan;
    }

    /**
     * Returns the configured ports.
     *
     * @return the string with the list of ports associated to the container on this
     *         configuration
     */
    public List<String> getPorts() {
        return new ArrayList<String>(ports);
    }

    /**
     * Returns the list of container flows configured for this container
     *
     * @return
     */
    public List<ContainerFlowConfig> getContainerFlowConfigs() {
        return (containerFlows == null || containerFlows.isEmpty()) ? new ArrayList<ContainerFlowConfig>(0)
                : new ArrayList<ContainerFlowConfig>(containerFlows);
    }

    /**
     * Matches container name against passed parameter.
     *
     * @param name
     *            name of the container to be matched
     * @return true if the passed argument correspond with the container name in the
     *         configuration
     */
    public boolean matchName(String name) {
        return this.container.equals(name);
    }

    /**
     * Parse the port list in several NodeConnector descriptor.
     *
     * @return the list of NodeConnector corresponding to the ports configured
     *         on this configuration
     */
    public List<NodeConnector> getPortList() {
        List<NodeConnector> portList = new ArrayList<NodeConnector>();
        if (ports != null && !ports.isEmpty()) {
            for (String portString : ports) {
                portList.add(NodeConnector.fromString(portString));
            }
        }
        return portList;
    }

    /**
     * Checks if this is a valid container configuration
     *
     * @return true, if is valid container configuration, false otherwise
     */
    public Status validate() {
        Status status = validateName();
        if (status.isSuccess()) {
            status = validateStaticVlan();
            if (status.isSuccess()) {
                status = validatePorts();
                if (status.isSuccess()) {
                    status = validateContainerFlows();
                }
            }
        }
        return status;
    }

    /**
     * Checks for valid name.
     *
     * @return true, if successful
     */
    private Status validateName() {
        // No Container configuration allowed to container default
        return ((container != null) && container.matches(regexName) && !container.equalsIgnoreCase(GlobalConstants.DEFAULT.toString())) ?
                new Status(StatusCode.SUCCESS) : new Status(StatusCode.BADREQUEST, "Invalid container name");
    }

    /**
     * Checks for valid ports.
     *
     * @return true, if successful
     */
    private Status validatePorts() {
        return validateNodeConnectors(this.ports);
    }

    public static Status validateNodeConnectors(List<String> connectorList) {
        if (connectorList != null && !connectorList.isEmpty()) {
            for (String ncString : connectorList) {
                if (NodeConnector.fromString(ncString) == null) {
                    return new Status(StatusCode.BADREQUEST, "Invalid node connector: " + ncString);
                }
            }

        }
        return new Status(StatusCode.SUCCESS);
    }

    public static List<NodeConnector> nodeConnectorsFromString(List<String> nodeConnectorStrings) {
        List<NodeConnector> list = new ArrayList<NodeConnector>(nodeConnectorStrings.size());
        for (String str : nodeConnectorStrings) {
            list.add(NodeConnector.fromString(str));
        }
        return list;
    }

    /**
     * Checks for valid static vlan.
     *
     * @return true, if successful
     */
    private Status validateStaticVlan() {
        if (staticVlan != null && !staticVlan.trim().isEmpty()) {
            short vl = 0;
            try {
                vl = Short.valueOf(staticVlan);
            } catch (NumberFormatException e) {
                return new Status(StatusCode.BADREQUEST, "Static Vlan Value must be between 1 and 4095");
            }
            if ((vl < 1) || (vl > 4095)) {
                return new Status(StatusCode.BADREQUEST, "Static Vlan Value must be between 1 and 4095");
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status validateContainerFlows() {
        if (containerFlows != null && !containerFlows.isEmpty()) {
            for (ContainerFlowConfig conf : containerFlows) {
                Status status = conf.validate();
                if (!status.isSuccess()) {
                    return new Status(StatusCode.BADREQUEST, "Invalid Flow Spec: " + status.getDescription());
                }
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Returns Vlan value in short
     *
     * @return the Vlan tag
     */
    public short getStaticVlanValue() {
        if ((staticVlan == null) || (staticVlan.trim().isEmpty())) {
            return 0;
        }
        try {
            return Short.valueOf(staticVlan);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Status addNodeConnectors(List<String> ncList) {
        // Syntax check
        Status status = ContainerConfig.validateNodeConnectors(ncList);
        if (!status.isSuccess()) {
            return status;
        }

        // Add ports
        ports.addAll(ncList);
        return new Status(StatusCode.SUCCESS);
    }

    public Status removeNodeConnectors(List<String> ncList) {
        // Syntax check
        Status status = ContainerConfig.validateNodeConnectors(ncList);
        if (!status.isSuccess()) {
            return status;
        }
        // Presence check
        if (ports.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "The following node connectors are not part of this container: "
                    + ncList);
        }
        List<String> extra = new ArrayList<String>(ncList);
        extra.removeAll(ports);
        if (!extra.isEmpty()) {
            return new Status(StatusCode.CONFLICT, "The following node connectors are not part of this container: " + extra);
        }
        // Remove ports
        ports.removeAll(ncList);
        return new Status(StatusCode.SUCCESS);
    }

    public Status validateContainerFlowModify(List<ContainerFlowConfig> cFlowConfigs, boolean delete) {
        // Sanity Check
        if (cFlowConfigs == null || cFlowConfigs.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid Flow Spec configuration(s): null or empty list");
        }
        // Validity check
        for (ContainerFlowConfig cFlowConf : cFlowConfigs) {
            Status status = cFlowConf.validate();
            if (!status.isSuccess()) {
                return new Status(StatusCode.BADREQUEST, String.format("Invalid Flow Spec configuration (%s): %s",
                        cFlowConf.getName(), status.getDescription()));
            }
        }
        // Name conflict check
        List<String> existingNames = this.getContainerFlowConfigsNames();
        List<String> proposedNames = this.getContainerFlowConfigsNames(cFlowConfigs);

        // Check for duplicates in the request
        if (proposedNames.size() < cFlowConfigs.size()) {
            return new Status(StatusCode.BADREQUEST,
                    "Invalid Flow Spec configuration(s): duplicate name configs present");
        }

        // Check for overflow
        if (delete) {
            // Raw size check
            if (proposedNames.size() > existingNames.size()) {
                return new Status(StatusCode.BADREQUEST,
                        "Invalid request: requested to remove more flow spec configs than available ones");
            }
            // Presence check
            for (ContainerFlowConfig config : cFlowConfigs) {
                if (!this.containerFlows.contains(config)) {
                    return new Status(StatusCode.BADREQUEST, String.format(
                            "Invalid request: requested to remove nonexistent flow spec config: %s",
                            config.getName()));
                }
            }
        } else {
            // Check for conflicting names with existing cFlows
            List<String> conflicting = new ArrayList<String>(existingNames);
            conflicting.retainAll(proposedNames);
            if (!conflicting.isEmpty()) {
                return new Status(StatusCode.CONFLICT,
                        "Invalid Flow Spec configuration: flow spec name(s) conflict with existing flow specs: "
                                + conflicting.toString());
            }

            /*
             * Check for conflicting flow spec match (we only check for strict
             * equality). Remove this in case (*) is reintroduced
             */
            if (this.containerFlows != null && !this.containerFlows.isEmpty()) {
                Set<Match> existingMatches = new HashSet<Match>();
                for (ContainerFlowConfig existing : this.containerFlows) {
                    existingMatches.addAll(existing.getMatches());
                }
                for (ContainerFlowConfig proposed : cFlowConfigs) {
                    if (existingMatches.removeAll(proposed.getMatches())) {
                        return new Status(StatusCode.CONFLICT, String.format(
                                "Invalid Flow Spec configuration: %s conflicts with existing flow spec",
                                proposed.getName()));
                    }
                }
            }
        }


        return new Status(StatusCode.SUCCESS);
    }

    public ContainerFlowConfig getContainerFlowConfig(String name) {
        if (this.containerFlows != null && !this.containerFlows.isEmpty()) {
            for (ContainerFlowConfig conf : this.containerFlows) {
                if (conf.getName().equals(name)) {
                    return new ContainerFlowConfig(conf);
                }
            }
        }
        return null;
    }

    public List<String> getContainerFlowConfigsNames() {
        return getContainerFlowConfigsNames(this.containerFlows);
    }

    /**
     * Returns the list of unique names for the passed list of
     * ContainerFlowConfig objects. the list will not contain duplicates even
     * though the passed object list has ContainerFlowConfig objects with same
     * names
     *
     * @param confList
     *            the list of ContainerFlowConfig objects
     * @return the list of correspondent unique container flow names. The return
     *         list may differ from the passed list in size, if the latter
     *         contains duplicates
     */
    private List<String> getContainerFlowConfigsNames(List<ContainerFlowConfig> confList) {
        // Use set to check for duplicates later
        Set<String> namesSet = new HashSet<String>();
        if (confList != null) {
            for (ContainerFlowConfig conf : confList) {
                namesSet.add(conf.getName());
            }
        }
        return new ArrayList<String>(namesSet);
    }

    /**
     * Add the proposed list of container flow configurations to this container
     * configuration. A validation check on the operation is first run.
     *
     * @param containerFlowConfigs
     *            the proposed list of container flow configuration objects to
     *            add to this container configuration object
     * @return the result of this request as Status object
     */
    public Status addContainerFlows(List<ContainerFlowConfig> containerFlowConfigs) {
        Status status = this.validateContainerFlowModify(containerFlowConfigs, false);
        if (!status.isSuccess()) {
            return status;
        }
        if (this.containerFlows.addAll(containerFlowConfigs) == false) {
            return new Status(StatusCode.INTERNALERROR, "Unable to update the flow spec configuration(s)");
        }
        return new Status(StatusCode.SUCCESS);
    }

    public Status removeContainerFlows(List<ContainerFlowConfig> containerFlowConfigs) {
        Status status = this.validateContainerFlowModify(containerFlowConfigs, true);
        if (!status.isSuccess()) {
            return status;
        }
        if (this.containerFlows.removeAll(containerFlowConfigs) == false) {
            return new Status(StatusCode.INTERNALERROR, "Unable to update the flow spec configuration(s)");
        }
        return new Status(StatusCode.SUCCESS);
    }

    public Status removeContainerFlows(Set<String> names) {
        // Sanity check
        if (names == null || names.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid flow spec names list");
        }
        // Validation check
        List<String> present = this.getContainerFlowConfigsNames();
        if (!present.containsAll(names)) {
            List<String> notPresent = new ArrayList<String>(names);
            notPresent.retainAll(present);
            return new Status(StatusCode.BADREQUEST, "Following flow spec(s) are not present: " + notPresent);
        }
        // Remove
        List<ContainerFlowConfig> toDelete = new ArrayList<ContainerFlowConfig>(names.size());
        for (ContainerFlowConfig config : this.containerFlows) {
            if (names.contains(config.getName())) {
                toDelete.add(config);
            }
        }
        if (this.containerFlows.removeAll(toDelete) == false) {
            return new Status(StatusCode.INTERNALERROR, "Unable to remove the flow spec configuration(s)");
        }
        return new Status(StatusCode.SUCCESS);
    }

    public List<ContainerFlowConfig> getContainerFlowConfigs(Set<String> names) {
        List<ContainerFlowConfig> list = new ArrayList<ContainerFlowConfig>(names.size());
        for (String name : names) {
            ContainerFlowConfig conf = this.getContainerFlowConfig(name);
            if (conf != null) {
                list.add(new ContainerFlowConfig(conf));
            }
        }
        return list;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((containerFlows == null) ? 0 : containerFlows.hashCode());
        result = prime * result + ((ports == null) ? 0 : ports.hashCode());
        result = prime * result + ((container == null) ? 0 : container.hashCode());
        result = prime * result + ((staticVlan == null) ? 0 : staticVlan.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContainerConfig other = (ContainerConfig) obj;
        if (containerFlows == null) {
            if (other.containerFlows != null) {
                return false;
            }
        } else if (!containerFlows.equals(other.containerFlows)) {
            return false;
        }
        if (ports == null) {
            if (other.ports != null) {
                return false;
            }
        } else if (!ports.equals(other.ports)) {
            return false;
        }
        if (container == null) {
            if (other.container != null) {
                return false;
            }
        } else if (!container.equals(other.container)) {
            return false;
        }
        if (staticVlan == null) {
            if (other.staticVlan != null) {
                return false;
            }
        } else if (!staticVlan.equals(other.staticVlan)) {
            return false;
        }
        return true;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String vlString = "";
        if (staticVlan != null) {
            vlString = staticVlan;
        }
        return "container=" + container + ((vlString.equals("") ? "" : " static Vlan=" + vlString)) + " ports=" + ports + " flowspecs=" + containerFlows;
    }

    /**
     * Returns whether this Container configuration object has any ports specified
     *
     * @return true if any port is specified, false otherwise
     */
    public boolean hasNodeConnectors() {
        return (ports != null && !ports.isEmpty());
    }

    /**
     * Returns whether this Container configuration object has any flow specs specified
     *
     * @return true if any flow spec is specified, false otherwise
     */
    public boolean hasFlowSpecs() {
        return (containerFlows != null && !containerFlows.isEmpty());
    }

    public List<ContainerFlow> getContainerFlowSpecs() {
        List<ContainerFlow> list = new ArrayList<ContainerFlow>();
        if (containerFlows != null && !containerFlows.isEmpty()) {
            for (ContainerFlowConfig flowSpec : containerFlows) {
                for (Match match : flowSpec.getMatches()) {
                    list.add(new ContainerFlow(match));
                }
            }
        }
        return list;
    }

    private String getContainerRole(boolean admin) {
        return String.format("%s-%s-%s", containerProfile, container, (admin ? ADMIN_SUFFIX : OPERATOR_SUFFIX));
    }

    /**
     * Return the well known administrator role for this container
     *
     * @return The administrator role for this container
     */
    public String getContainerAdminRole() {
        return getContainerRole(true);
    }

    /**
     * Return the well known operator role for this container
     *
     * @return The operator role for this container
     */
    public String getContainerOperatorRole() {
        return getContainerRole(false);
    }

    public String getContainerGroupName() {
        return String.format("%s-%s", containerProfile, container);
    }
}
