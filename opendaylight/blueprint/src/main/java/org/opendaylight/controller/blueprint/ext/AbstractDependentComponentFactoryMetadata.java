/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.aries.blueprint.di.AbstractRecipe;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.ext.DependentComponentFactoryMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Pantelis
 */
abstract class AbstractDependentComponentFactoryMetadata implements DependentComponentFactoryMetadata {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String id;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean satisfied = new AtomicBoolean();
    private final List<StaticServiceReferenceRecipe> serviceRecipes = new ArrayList<>();
    private volatile ExtendedBlueprintContainer container;
    private volatile SatisfactionCallback satisfactionCallback;
    private volatile String failureMessage;
    private volatile String dependendencyDesc;

    protected AbstractDependentComponentFactoryMetadata(String id) {
        this.id = Preconditions.checkNotNull(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getActivation() {
        return ACTIVATION_EAGER;
    }

    @Override
    public List<String> getDependsOn() {
        return Collections.emptyList();
    }

    @Override
    public String getDependencyDescriptor() {
        return dependendencyDesc;
    }

    @Override
    public boolean isSatisfied() {
        return satisfied.get();
    }

    protected abstract void startTracking();

    protected void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    protected void setDependendencyDesc(String dependendencyDesc) {
        this.dependendencyDesc = dependendencyDesc;
    }

    protected final ExtendedBlueprintContainer container() {
        return container;
    }

    protected void setSatisfied() {
        satisfied.set(true);
        satisfactionCallback.notifyChanged();
    }

    protected void retrieveService(String name, Class<?> interfaceClass, Consumer<Object> onServiceRetrieved) {
        StaticServiceReferenceRecipe recipe = new StaticServiceReferenceRecipe(getId() + "-" + name,
                container, interfaceClass.getName());
        setDependendencyDesc(recipe.getOsgiFilter());
        serviceRecipes.add(recipe);

        recipe.startTracking(onServiceRetrieved);
    }

    protected final String logName() {
        return (container != null ? container.getBundleContext().getBundle().getSymbolicName() : "") +
                " (" + id + ")";
    }

    @Override
    public void init(ExtendedBlueprintContainer container) {
        this.container = container;

        log.debug("{}: In init", logName());
    }

    protected void onCreate() throws ComponentDefinitionException {
        if(failureMessage != null) {
            throw new ComponentDefinitionException(failureMessage);
        }

        // The following code is a bit odd so requires some explanation. A little background... If a bean
        // is a prototype then the corresponding Recipe create method does not register the bean as created
        // with the BlueprintRepository and thus the destroy method isn't called on container destroy. We
        // rely on destroy being called to close our DTCL registration. Unfortunately the default setting
        // for the prototype flag in AbstractRecipe is true and the DependentComponentFactoryRecipe, which
        // is created for DependentComponentFactoryMetadata types of which we are one, doesn't have a way for
        // us to indicate the prototype state via our metadata.
        //
        // The ExecutionContext is actually backed by the BlueprintRepository so we access it here to call
        // the removePartialObject method which removes any partially created instance, which does not apply
        // in our case, and also has the side effect of registering our bean as created as if it wasn't a
        // prototype. We also obtain our corresponding Recipe instance and clear the prototype flag. This
        // doesn't look to be necessary but is done so for completeness. Better late than never. Note we have
        // to do this here rather than in startTracking b/c the ExecutionContext is not available yet at that
        // point.
        //
        // Now the stopTracking method is called on container destroy but startTracking/stopTracking can also
        // be called multiple times during the container creation process for Satisfiable recipes as bean
        // processors may modify the metadata which could affect how dependencies are satisfied. An example of
        // this is with service references where the OSGi filter metadata can be modified by bean processors
        // after the initial service dependency is satisfied. However we don't have any metadata that could
        // be modified by a bean processor and we don't want to register/unregister our DTCL multiple times
        // so we only process startTracking once and close the DTCL registration once on container destroy.
        ExecutionContext executionContext = ExecutionContext.Holder.getContext();
        executionContext.removePartialObject(id);

        Recipe myRecipe = executionContext.getRecipe(id);
        if(myRecipe instanceof AbstractRecipe) {
            log.debug("{}: setPrototype to false", logName());
            ((AbstractRecipe)myRecipe).setPrototype(false);
        } else {
            log.warn("{}: Recipe is null or not an AbstractRecipe", logName());
        }
    }

    @Override
    public final void startTracking(final SatisfactionCallback satisfactionCallback) {
        if(!started.compareAndSet(false, true)) {
            return;
        }

        log.debug("{}: In startTracking", logName());

        this.satisfactionCallback = satisfactionCallback;

        startTracking();
    }

    @Override
    public void stopTracking() {
        log.debug("{}: In stopTracking", logName());

        stopServiceRecipes();
    }

    @Override
    public void destroy(Object instance) {
        log.debug("{}: In destroy", logName());

        stopServiceRecipes();
    }

    private void stopServiceRecipes() {
        for(StaticServiceReferenceRecipe recipe: serviceRecipes) {
            recipe.stop();
        }

        serviceRecipes.clear();
    }
}
