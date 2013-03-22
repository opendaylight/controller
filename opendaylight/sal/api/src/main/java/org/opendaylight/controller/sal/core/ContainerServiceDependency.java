
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

/**
 * @file   ContainerServiceDependency.java
 *
 * @brief  Class representing a ServiceDependency on a container
 *
 * Class representing a ServiceDependency on a container
 */

import java.util.Dictionary;
import org.osgi.framework.ServiceReference;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyActivation;
import org.apache.felix.dm.DependencyService;

/**
 * Class representing a ServiceDependency on a container
 *
 */
public class ContainerServiceDependency implements ServiceDependency,
        DependencyActivation {
    private ServiceDependency m_dep;
    private String containerName;

    public ContainerServiceDependency(DependencyManager manager,
            String containerName) {
        this.m_dep = manager.createServiceDependency();
        this.containerName = containerName;
    }

    private ContainerServiceDependency(ServiceDependency explicitDependency,
            String containerName) {
        this.m_dep = explicitDependency;
        this.containerName = containerName;
    }

    @Override
    public ServiceDependency setService(Class serviceName) {
        this.m_dep.setService(serviceName, "(containerName="
                + this.containerName + ")");
        return this;
    }

    @Override
    public ServiceDependency setService(Class serviceName, String serviceFilter) {
        this.m_dep.setService(serviceName, "(&(containerName="
                + this.containerName + ")" + serviceFilter + ")");
        return this;
    }

    @Override
    public ServiceDependency setService(String serviceFilter) {
        this.m_dep.setService("(&(containerName=" + this.containerName + ")"
                + serviceFilter + ")");
        return this;
    }

    @Override
    public ServiceDependency setService(Class serviceName,
            ServiceReference serviceReference) {
        this.m_dep.setService(serviceName, serviceReference);
        return this;
    }

    @Override
    public ServiceDependency setDefaultImplementation(Object implementation) {
        this.m_dep.setDefaultImplementation(implementation);
        return this;
    }

    @Override
    public ServiceDependency setRequired(boolean required) {
        this.m_dep.setRequired(required);
        return this;
    }

    @Override
    public ServiceDependency setAutoConfig(boolean autoConfig) {
        this.m_dep.setAutoConfig(autoConfig);
        return this;
    }

    @Override
    public ServiceDependency setAutoConfig(String instanceName) {
        this.m_dep.setAutoConfig(instanceName);
        return this;
    }

    @Override
    public ServiceDependency setCallbacks(String added, String removed) {
        this.m_dep.setCallbacks(added, removed);
        return this;
    }

    @Override
    public ServiceDependency setCallbacks(String added, String changed,
            String removed) {
        this.m_dep.setCallbacks(added, changed, removed);
        return this;
    }

    @Override
    public ServiceDependency setCallbacks(String added, String changed,
            String removed, String swapped) {
        this.m_dep.setCallbacks(added, changed, removed, swapped);
        return this;
    }

    @Override
    public ServiceDependency setCallbacks(Object instance, String added,
            String removed) {
        this.m_dep.setCallbacks(instance, added, removed);
        return this;
    }

    @Override
    public ServiceDependency setCallbacks(Object instance, String added,
            String changed, String removed) {
        this.m_dep.setCallbacks(instance, added, changed, removed);
        return this;
    }

    @Override
    public ServiceDependency setCallbacks(Object instance, String added,
            String changed, String removed, String swapped) {
        this.m_dep.setCallbacks(instance, added, changed, removed, swapped);
        return this;
    }

    @Override
    public ServiceDependency setPropagate(boolean propagate) {
        this.m_dep.setPropagate(propagate);
        return this;
    }

    @Override
    public ServiceDependency setPropagate(Object instance, String method) {
        this.m_dep.setPropagate(instance, method);
        return this;
    }

    @Override
    public ServiceDependency setInstanceBound(boolean isInstanceBound) {
        this.m_dep.setInstanceBound(isInstanceBound);
        return this;
    }

    @Override
    public Dependency createCopy() {
        return new ContainerServiceDependency((ServiceDependency) this.m_dep
                .createCopy(), this.containerName);
    }

    @Override
    public Dictionary getProperties() {
        return this.m_dep.getProperties();
    }

    @Override
    public boolean isPropagated() {
        return this.m_dep.isPropagated();
    }

    @Override
    public boolean isRequired() {
        return this.m_dep.isRequired();
    }

    @Override
    public boolean isAvailable() {
        return this.m_dep.isAvailable();
    }

    @Override
    public boolean isInstanceBound() {
        return this.m_dep.isInstanceBound();
    }

    @Override
    public boolean isAutoConfig() {
        return this.m_dep.isAutoConfig();
    }

    @Override
    public Class getAutoConfigType() {
        return this.m_dep.getAutoConfigType();
    }

    @Override
    public Object getAutoConfigInstance() {
        return this.m_dep.getAutoConfigInstance();
    }

    @Override
    public String getAutoConfigName() {
        return this.m_dep.getAutoConfigName();
    }

    @Override
    public void invokeAdded(DependencyService service) {
        this.m_dep.invokeAdded(service);
    }

    @Override
    public void invokeRemoved(DependencyService service) {
        this.m_dep.invokeRemoved(service);
    }

    @Override
    public String getName() {
        return this.m_dep.getName();
    }

    @Override
    public String getType() {
        return this.m_dep.getType();
    }

    @Override
    public int getState() {
        return this.m_dep.getState();
    }

    @Override
    public void start(DependencyService service) {
        DependencyActivation a = (DependencyActivation) this.m_dep;
        a.start(service);
    }

    @Override
    public void stop(DependencyService service) {
        DependencyActivation a = (DependencyActivation) this.m_dep;
        a.stop(service);
    }
}
