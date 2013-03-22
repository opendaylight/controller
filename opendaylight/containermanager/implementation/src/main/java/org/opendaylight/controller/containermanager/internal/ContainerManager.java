
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   ContainerManager.java
 *
 * @brief  Manage one or many Containers
 *
 *
 */
package org.opendaylight.controller.containermanager.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.sal.core.IContainerAware;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.Status;

public class ContainerManager implements IContainerManager {
    private static final Logger logger = LoggerFactory
            .getLogger(ContainerManager.class);
    private IClusterGlobalServices clusterServices;
    /*
     * Collection containing the configuration objects.
     * This is configuration world: container names (also the map key)
     * are maintained as they were configured by user, same case
     */
    private Set<IContainerAware> iContainerAware = (Set<IContainerAware>) Collections
            .synchronizedSet(new HashSet<IContainerAware>());
    private Set<IContainerListener> iContainerListener = Collections
            .synchronizedSet(new HashSet<IContainerListener>());

    void setIContainerListener(IContainerListener s) {
        if (this.iContainerListener != null) {
            this.iContainerListener.add(s);
        }
    }

    void unsetIContainerListener(IContainerListener s) {
        if (this.iContainerListener != null) {
            this.iContainerListener.remove(s);
        }
    }

    public void setIContainerAware(IContainerAware iContainerAware) {
        if (!this.iContainerAware.contains(iContainerAware)) {
            this.iContainerAware.add(iContainerAware);
            // Now call the container creation for all the known containers so
            // far
            List<String> containerDB = getContainerNames();
            if (containerDB != null) {
                for (int i = 0; i < containerDB.size(); i++) {
                    iContainerAware.containerCreate(containerDB.get(i));
                }
            }
        }
    }

    public void unsetIContainerAware(IContainerAware iContainerAware) {
        this.iContainerAware.remove(iContainerAware);
        // There is no need to do cleanup of the component when
        // unregister because it will be taken care by the Containerd
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

    public void init() {
        logger.info("ContainerManager startup....");
    }

    public void destroy() {
        // Clear local states
        this.iContainerAware.clear();
        this.iContainerListener.clear();

        logger.info("ContainerManager Shutdown....");
    }

    @Override
    public List<String> getContainerNames() {
        /*
         * Return container names as they were configured by user (case sensitive)
         * along with the default container
         */
        List<String> containerNameList = new ArrayList<String>();
        containerNameList.add(GlobalConstants.DEFAULT.toString());
        return containerNameList;
    }

    @Override
    public boolean hasNonDefaultContainer() {
        return false;
    }

    @Override
    public Status saveContainerConfig() {
        return null;
    }

}
