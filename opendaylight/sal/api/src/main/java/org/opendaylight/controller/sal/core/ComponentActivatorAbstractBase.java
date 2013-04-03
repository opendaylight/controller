
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

/**
 * @file   ComponentActivatorAbstractBase.java
 *
 * @brief  Abstract class which need to be subclassed in order to
 * track and register dependencies per-container
 *
 * Abstract class which need to be subclassed in order to
 * track and register dependencies per-container
 *
 */

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class which need to be subclassed in order to track and
 * register dependencies per-container
 *
 */
abstract public class ComponentActivatorAbstractBase implements
        BundleActivator, IContainerAware {
    Logger logger = LoggerFactory
            .getLogger(ComponentActivatorAbstractBase.class);
    private ServiceRegistration containerAwareRegistration;
    private DependencyManager dm;
    private ConcurrentMap<ImmutablePair<String, Object>, Component> dbInstances = (ConcurrentMap<ImmutablePair<String, Object>, Component>) new ConcurrentHashMap<ImmutablePair<String, Object>, Component>();
    private ConcurrentMap<Object, Component> dbGlobalInstances = (ConcurrentMap<Object, Component>) new ConcurrentHashMap<Object, Component>();

    /**
     * Abstract method that MUST be implemented by the derived class
     * that wants to activate the Component bundle in a container. Here
     * customization for the component are expected
     */
    abstract protected void init();

    /**
     * Abstract method that MUST be implemented by the derived class
     * that wants to DE-activate the Component bundle in a container. Here
     * customization for the component are expected
     */
    abstract protected void destroy();

    /**
     * Method which tells how many implementations are supported by
     * the bundle. This way we can tune the number of components
     * created.
     *
     *
     * @return The list of implementations the bundle will support,
     * this will be used to decide how many components need to be
     * created per-container
     */
    protected Object[] getImplementations() {
        return null;
    }

    /**
     * Method which tells how many Global implementations are
     * supported by the bundle. This way we can tune the number of
     * components created. This components will be created ONLY at the
     * time of bundle startup and will be destroyed only at time of
     * bundle destruction, this is the major difference with the
     * implementation retrieved via getImplementations where all of
     * them are assumed to be in a container!
     *
     *
     * @return The list of implementations the bundle will support,
     * in Global version
     */
    protected Object[] getGlobalImplementations() {
        return null;
    }

    /**
     * Configure the dependency for a given instance inside a container
     *
     * @param c Component assigned for this instance, this will be
     * what will be used for configuration
     * @param imp implementation to be configured
     * @param containerName container on which the configuration happens
     */
    protected void configureInstance(Component c, Object imp,
            String containerName) {
        // do nothing by default
    }

    /**
     * Configure the dependency for a given instance Global
     *
     * @param c Component assigned for this instance, this will be
     * what will be used for configuration
     * @param imp implementation to be configured
     * @param containerName container on which the configuration happens
     */
    protected void configureGlobalInstance(Component c, Object imp) {
        // Do nothing by default
    }

    // Private class used to listen to state transition so we can
    // implement the necessary logic to call "started" and "stopping"
    // methods on the component. Right now the framework natively
    // support only the call of:
    // - "init": Called after dependency are satisfied
    // - "start": Called after init but before services are registered
    // - "stop": Called after services are unregistered but before the
    // component is going to be destroyed
    // - "destroy": Called to destroy the component.
    // There is still a need for two notifications:
    // - "started" method to be called after "start" and after the
    // services has been registered in the OSGi service registry
    // - "stopping" method to be called before "stop" method and
    // before the services of the component are removed from OSGi
    // service registry
    class ListenerComponentStates implements ComponentStateListener {
        @Override
        public void starting(Component component) {
            // do nothing
        }

        @Override
        public void started(Component component) {
            if (component == null) {
                return;
            }
            component.invokeCallbackMethod(new Object[] { component
                    .getService() }, "started", new Class[][] {
                    { Component.class }, {} }, new Object[][] { { component },
                    {} });
        }

        @Override
        public void stopped(Component component) {
            if (component == null) {
                return;
            }
            component.invokeCallbackMethod(new Object[] { component
                    .getService() }, "stopping", new Class[][] {
                    { Component.class }, {} }, new Object[][] { { component },
                    {} });
        }

        @Override
        public void stopping(Component component) {
            // do nothing
        }
    }

    /**
     * Method of IContainerAware called when a new container is available
     *
     * @param containerName Container being created
     */
    @Override
    public void containerCreate(String containerName) {
        try {
            Object[] imps = getImplementations();
            logger.trace("Creating instance {}", containerName);
            if (imps != null) {
                for (int i = 0; i < imps.length; i++) {
                    ImmutablePair<String, Object> key = new ImmutablePair<String, Object>(
                            containerName, imps[i]);
                    Component c = this.dbInstances.get(key);
                    if (c == null) {
                        c = this.dm.createComponent();
                        c.addStateListener(new ListenerComponentStates());
                        // Now let the derived class to configure the
                        // dependencies it wants
                        configureInstance(c, imps[i], containerName);
                        // Set the implementation so the component can manage
                        // its lifecycle
                        if (c.getService() == null) {
                            logger.trace("Setting implementation to: {}",
                                          imps[i]);
                            c.setImplementation(imps[i]);
                        }

                        //Set the service properties to include the containerName
                        //in the service, that is fundamental for supporting
                        //multiple services just distinguished via a container
                        Dictionary<String, String> serviceProps = c
                                .getServiceProperties();
                        if (serviceProps != null) {
                            logger.trace("Adding new property for container");
                            serviceProps.put("containerName", containerName);
                        } else {
                            logger
                                    .trace("Create a new properties for the service");
                            serviceProps = new Hashtable<String, String>();
                            serviceProps.put("containerName", containerName);
                        }
                        c.setServiceProperties(serviceProps);

                        // Now add the component to the dependency Manager
                        // which will immediately start tracking the dependencies
                        this.dm.add(c);

                        //Now lets keep track in our shadow database of the
                        //association
                        this.dbInstances.put(key, c);
                    } else {
                        logger
                                .error("I have been asked again to create an instance "
                                        + "on: "
                                        + containerName
                                        + "for object: "
                                        + imps[i]
                                        + "when i already have it!!");
                    }
                }
            }
        } catch (Exception ex) {
            logger
                    .error("During containerDestroy invocation caught exception: "
                            + ex
                            + "\nStacktrace:"
                            + stackToString(ex.getStackTrace()));
        }
    }

    @Override
    public void containerDestroy(String containerName) {
        try {
            Object[] imps = getImplementations();
            logger.trace("Destroying instance {}", containerName);
            if (imps != null) {
                for (int i = 0; i < imps.length; i++) {
                    ImmutablePair<String, Object> key = new ImmutablePair<String, Object>(
                            containerName, imps[i]);
                    Component c = this.dbInstances.get(key);
                    if (c != null) {
                        // Now remove the component from dependency manager,
                        // which will implicitely stop it first
                        this.dm.remove(c);
                    } else {
                        logger
                                .error("I have been asked again to remove an instance "
                                        + "on: "
                                        + containerName
                                        + "for object: "
                                        + imps[i]
                                        + "when i already have cleared it!!");
                    }

                    //Now lets remove the association from our shadow
                    //database so the component can be recycled, this is done
                    //unconditionally in case of spurious conditions
                    this.dbInstances.remove(key);
                }
            }
        } catch (Exception ex) {
            logger
                    .error("During containerDestroy invocation caught exception: "
                            + ex
                            + "\nStacktrace:"
                            + stackToString(ex.getStackTrace()));
        }
    }

    private String stackToString(StackTraceElement[] stack) {
        if (stack == null) {
            return "<EmptyStack>";
        }
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < stack.length; i++) {
            buffer.append("\n\t" + stack[i].toString());
        }
        return buffer.toString();
    }

    /**
     * Method called by the OSGi framework when the OSGi bundle
     * starts. The functionality we want to perform here are:
     *
     * 1) Register with the OSGi framework, that we are a provider of
     * IContainerAware service and so in case of startup of a container we
     * want to be called
     *
     * 2) Create data structures that allow to keep track of all the
     * instances created per-container given the derived class of
     * ComponentActivatorAbstractBase will act as a Factory manager
     *
     * @param context OSGi bundle context to interact with OSGi framework
     */
    @Override
    public void start(BundleContext context) {
        try {
            this.dm = new DependencyManager(context);

            logger.trace("Activating");

            // Now create Global components
            Object[] imps = getGlobalImplementations();
            if (imps != null) {
                for (int i = 0; i < imps.length; i++) {
                    Object key = imps[i];
                    Component c = this.dbGlobalInstances.get(key);
                    if (c == null) {
                        try {
                            c = this.dm.createComponent();
                            c.addStateListener(new ListenerComponentStates());
                            // Now let the derived class to configure the
                            // dependencies it wants
                            configureGlobalInstance(c, imps[i]);
                            // Set the implementation so the component
                            // can manage its lifesycle
                            if (c.getService() == null) {
                                logger.trace("Setting implementation to: {}",
                                        imps[i]);
                                c.setImplementation(imps[i]);
                            }

                            // Now add the component to the dependency
                            // Manager which will immediately start
                            // tracking the dependencies
                            this.dm.add(c);
                        } catch (Exception nex) {
                            logger.error("During creation of a Global "
                                    + "instance caught exception: " + nex
                                    + "\nStacktrace:"
                                    + stackToString(nex.getStackTrace()));
                        }

                        //Now lets keep track in our shadow database of the
                        //association
                        if (c != null)
                            this.dbGlobalInstances.put(key, c);
                    } else {
                        logger.error("I have been asked again to create an "
                                + "instance " + " Global for object: "
                                + imps[i] + "when i already have it!!");
                    }
                }
            }

            // Register with OSGi the provider for the service IContainerAware
            this.containerAwareRegistration = context.registerService(
                    IContainerAware.class.getName(), this, null);

            // Now call the derived class init function
            this.init();

            logger.trace("Activation DONE!");
        } catch (Exception ex) {
            logger.error("During Activator start caught exception: " + ex
                    + "\nStacktrace:" + stackToString(ex.getStackTrace()));
        }
    }

    /**
     * Method called by the OSGi framework when the OSGi bundle
     * stops. The functionality we want to perform here are:
     *
     * 1) Force all the instances to stop and do cleanup and
     * unreference them so garbage collection can clean them up
     *
     * NOTE: UN-Register with the OSGi framework,is not needed because
     * the framework will automatically do it
     *
     * @param context OSGi bundle context to interact with OSGi framework
     */
    @Override
    public void stop(BundleContext context) {
        try {
            logger.trace("DE-Activating");

            // Now call the derived class destroy function
            this.destroy();

            // Now remove all the components tracked for container components
            for (ImmutablePair<String, Object> key : this.dbInstances.keySet()) {
                try {
                    Component c = this.dbInstances.get(key);
                    if (c != null) {
                        logger.trace("Remove component on container: {} Object: {}",
                                key.getLeft(), key.getRight());
                        this.dm.remove(c);
                    }
                } catch (Exception nex) {
                    logger.error("During removal of a container component "
                            + "instance caught exception: " + nex
                            + "\nStacktrace:"
                            + stackToString(nex.getStackTrace()));
                }
                this.dbInstances.remove(key);
            }

            // Now remove all the components tracked for Global Components
            for (Object key : this.dbGlobalInstances.keySet()) {
                try {
                    Component c = this.dbGlobalInstances.get(key);
                    if (c != null) {
                        logger.trace("Remove component for Object: {}" , key);
                        this.dm.remove(c);
                    }
                } catch (Exception nex) {
                    logger.error("During removal of a Global "
                            + "instance caught exception: " + nex
                            + "\nStacktrace:"
                            + stackToString(nex.getStackTrace()));
                }

                this.dbGlobalInstances.remove(key);
            }

            // Detach Dependency Manager
            this.dm = null;

            logger.trace("Deactivation DONE!");
        } catch (Exception ex) {
            logger.error("During Activator stop caught exception: " + ex
                    + "\nStacktrace:" + stackToString(ex.getStackTrace()));
        }
    }

    /**
     * Return a ServiceDependency customized ad hoc for slicing, this
     * essentially the same org.apache.felix.dm.ServiceDependency just
     * with some filters pre-set
     *
     * @param containerName containerName for which we want to create the dependency
     *
     * @return a ServiceDependency
     */
    protected ServiceDependency createContainerServiceDependency(
            String containerName) {
        return (new ContainerServiceDependency(this.dm, containerName));
    }

    /**
     * Return a ServiceDependency as provided by Dependency Manager as it's
     *
     *
     * @return a ServiceDependency
     */
    protected ServiceDependency createServiceDependency() {
        return this.dm.createServiceDependency();
    }
}
