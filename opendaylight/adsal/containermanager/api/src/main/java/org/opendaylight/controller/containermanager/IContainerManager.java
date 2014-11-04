
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Container Manager provides an ability for the Network Administrators to
 * partitions a production network into smaller, isolated and manageable
 * networks. IContainerManager interface exposes these Container management capabilities
 * via the supported APIs
 */
public interface IContainerManager {

    /**
     * Create a Container
     *
     * @param configObject
     *            ContainerConfig object that carries name of the Container to be
     *            created
     * @return returns the status code of adding a container
     */
    public Status addContainer(ContainerConfig configObject);

    /**
     * Remove a container
     *
     * @param configObject
     *            ContainerConfig object that carries the name of the Container to be
     *            removed
     * @return returns the status code of removing a container
     */
    public Status removeContainer(ContainerConfig configObject);

    /**
     * Remove a container
     *
     * @param containerName
     *            the container name
     * @return returns the status code of removing a container
     */
    public Status removeContainer(String containerName);

    /**
     * Adds resources to a given container. Updates the container data based on new
     * resources added
     *
     * @param configObject
     *            refer to {@link com.ContainerConfig.csdn.containermanager.ContainerConfig
     *            ContainerConfig}
     * @return returns the status code of adding a container entry
     */
    public Status addContainerEntry(String containerName, List<String> portList);

    /**
     * Remove a resource from a given container. Updates the container data based on new
     * resources removed.
     *
     * @param configObject
     *            refer to {@link com.ContainerConfig.csdn.containermanager.ContainerConfig
     *            ContainerConfig}
     * @return returns the status code of removing a container entry
     */
    public Status removeContainerEntry(String containerName, List<String> portList);

    /**
     * Adds/Removes a container flow
     *
     * @param configObject
     *            refer to {@link com.ContainerConfig.csdn.containermanager.ContainerConfig
     *            ContainerConfig}
     * @return returns the status code of adding a container flow
     */
    public Status addContainerFlows(String containerName, List<ContainerFlowConfig> configObject);

    /**
     * Remove a container flow
     *
     * @param configObject
     *            refer to {@link com.ContainerConfig.csdn.containermanager.ContainerConfig
     *            ContainerConfig}
     * @return returns the status of removing a container flow
     */
    public Status removeContainerFlows(String containerName, List<ContainerFlowConfig> configObject);

    /**
     * Remove a container flow
     *
     * @param containerName
     *            the name of the container
     * @param name
     *            the name of the container flow
     * @return the status of the request
     */
    public Status removeContainerFlows(String containerName, Set<String> name);

    /**
     * Get the list of {@link com.ContainerConfig.csdn.containermanager.ContainerConfig
     * ContainerConfig} objects representing all the containers that have been
     * configured previously.
     *
     * @return the lsit of {@link com.ContainerConfig.csdn.containermanager.ContainerConfig
     *         ContainerConfig} objects configured so far
     */
    public List<ContainerConfig> getContainerConfigList();

    /**
     * Get the configuration object for the specified container
     *
     * @param containerName
     *            the name of the container
     * @return a copy of the {@link com.ContainerConfig.csdn.containermanager.ContainerConfig
     *         ContainerConfig} object for the specified container if present, null
     *         otherwise
     */
    public ContainerConfig getContainerConfig(String containerName);

    /**
     * Returns a list of container names that currently exist.
     *
     * @return array of String container names
     */
    public List<String> getContainerNameList();

    /**
     * Check for the existence of a container
     *
     * @param ContainerId
     *            Name of the Container
     *
     * @return true if it exists, false otherwise
     */
    public boolean doesContainerExist(String ContainerId);

    /**
     * Get an array of ContainerFlowConfig objects representing all the
     * container flows that have been configured previously.
     *
     * @return array of {@link org.opendaylight.controller.containermanager.ContainerFlowConfig
     *         ContainerFlowConfig}
     */
    public Map<String, List<ContainerFlowConfig>> getContainerFlows();

    /**
     * Get an array of {@link org.opendaylight.controller.containermanager.ContainerFlowConfig
     * ContainerFlowConfig} objects representing all the container flows that
     * have been configured previously on the given containerName
     *
     * @param containerName
     *            the container name
     * @return array of {@link org.opendaylight.controller.containermanager.ContainerFlowConfig
     *         ContainerFlowConfig}
     */
    public List<ContainerFlowConfig> getContainerFlows(String containerName);

    /**
     * Get an the list of names of the container flows that have been configured
     * previously on the given containerName
     *
     * @param containerName
     *            the container name
     * @return the array containing the names of the container flows configured
     *         on the specified container
     */
    public List<String> getContainerFlowNameList(String containerName);

    /**
     * Returns true if there are any non-default Containers present.
     *
     * @return  true if any non-default container is present false otherwise.
     */
    public boolean hasNonDefaultContainer();

    /**
     * Returns a list of the existing containers.
     *
     * @return  List of Container name strings.
     */
    public List<String> getContainerNames();

    /**
     * Returns whether the controller is running in container mode
     *
     * @return true if controller is in container mode, false otherwise
     */
    public boolean inContainerMode();
}
