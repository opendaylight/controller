
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

/**
 * PortGroupProvider interface provides all the necessary blueprint for a custom application to implement
 * in order to provide Port Grouping Service. Custom Application that implements this interface will have
 * to handle the opaque match criteria passed to it via PortGroupConfig.
 *
 *
 *
 */
public interface PortGroupProvider {
    /**
     * This method is invoked by the Controller towards the Provider when a new port group is configured.
     *
     * @param config New PortGroupConfig object created by user Configuration.
     * @return true if successful. false otherwise.
     */
    public boolean createPortGroupConfig(PortGroupConfig config);

    /**
     * This method is invoked by the Controller towards the Provider when an existing port group is deleted.
     *
     * @param config Existing Port Group Configuration deleted by the user.
     * @return true if successful. false otherwise.
     */
    public boolean deletePortGroupConfig(PortGroupConfig config);

    /**
     * Returns the complete mapping database corresponds to a PortGroup Configuration.
     * Its the PortGroupProvider Application's responsibility to manage the Switches & the Set of its Ports that
     * correspond to each of the Configuration and return it to the Controller when requested.
     *
     * @param config User Configuration
     * @see PortGroupConfig
     * @return Database of Switch-Id to PortGroup mapping that corresponds to the Port Group User Configuration.
     */
    public Map<Node, PortGroup> getPortGroupData(PortGroupConfig config);

    /**
     * Returns PortGroup data for a given Switch and user Configuration.
     * Its the PortGroupProvider Application's responsibility to manage the Switches & the Set of its Ports that
     * correspond to each of the Configuration and return it to the Controller when requested.
     *
     * @param config User Configuration
     * @param matrixSwitchId Switch Id that represents an openflow Switch
     * @see PortGroupConfig
     * @return PortGroup data for a given Openflow switch.
     * @see PortGroup
     */
    public PortGroup getPortGroupData(PortGroupConfig config,
            long matrixSwitchId);

    /**
     * Registers a Listener for Port Group membership changes based on Custom application algorithm.
     * @param listener A Controller module that listens to events from the Custom Port Grouping Application.
     */
    public void registerPortGroupChange(PortGroupChangeListener listener);

    /**
     * Application returns an Usage string for the Match Criteria User Configuration.
     * Controller provides an opportunity for application to implement Custom Algorithm for Port Grouping.
     * This method exposes the custom algorithm to the user so that the user can configure the matchString
     * regular expression in PortGroupConfig appropriately.
     *
     * @return Usage string.
     */
    public String getApplicationDrivenMatchCriteriaUsage();

    /**
     * Returns the name of the Custom Application that implements  PortGroupProvider interface.
     *
     * @return Provider Name
     */
    public String getProviderName();

    /**
     * Controller uses this method to check with the Provider supports the matchCriteria String configured by the User.
     *
     * @param matchCriteria
     * @return true if the Provider supports the matchCriteria String. false otherwise.
     */
    public boolean isMatchCriteriaSupported(String matchCriteria);
}
