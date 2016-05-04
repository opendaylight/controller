/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.aries.blueprint.ext.DependentComponentFactoryMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory metadata corresponding to the "clustered-app-config" element that obtains an application's
 * config data from the binding data broker and provides the binding DataObject instance to the
 * Blueprint container. In addition registers a DataTreeChangeListener to restart the Blueprint container
 * when the config data is changed.
 *
 * @author Thomas Pantelis
 */
public class DataStoreAppConfigMetadata implements DependentComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(DataStoreAppConfigMetadata.class);

    private final String id;
    private final String appConfigBindingClassName;
    private final AtomicBoolean readingInitialAppConfig = new AtomicBoolean(true);
    private final AtomicBoolean started = new AtomicBoolean();

    private volatile Class<DataObject> appConfigBindingClass;
    private volatile ExtendedBlueprintContainer container;
    private volatile StaticServiceReferenceRecipe dataBrokerServiceRecipe;
    private volatile StaticServiceReferenceRecipe bindingCodecServiceRecipe;
    private volatile BindingNormalizedNodeSerializer bindingSerializer;
    private volatile String dependendencyDesc;
    private volatile ListenerRegistration<?> appConfigChangeListenerReg;
    private volatile DataObject currentAppConfig;
    private volatile boolean updatesEnabled;
    private volatile SatisfactionCallback satisfactionCallback;
    private volatile String failureMessage;

    public DataStoreAppConfigMetadata(String id, String appConfigBindingClassName) {
        this.id = id;
        this.appConfigBindingClassName = appConfigBindingClassName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getActivation() {
        return ACTIVATION_LAZY;
    }

    @Override
    public List<String> getDependsOn() {
        return Collections.emptyList();
    }

    @Override
    public boolean isSatisfied() {
        return currentAppConfig != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(ExtendedBlueprintContainer container) {
        LOG.debug("{}: In init", id);

        this.container = container;

        try {
            Class<?> bindingClass = container.getBundleContext().getBundle().loadClass(appConfigBindingClassName);
            if(!DataObject.class.isAssignableFrom(bindingClass)) {
                throw new ComponentDefinitionException(String.format(
                        "%s: Specified app config binding class %s does not extend %s",
                        id, appConfigBindingClassName, DataObject.class.getName()));
            }

            appConfigBindingClass = (Class<DataObject>) bindingClass;
        } catch(ClassNotFoundException e) {
            throw new ComponentDefinitionException(String.format("%s: Error loading for app config binding class %s",
                    appConfigBindingClassName), e);
        }
    }

    @Override
    public Object create() throws ComponentDefinitionException {
        LOG.debug("{}: In create - currentAppConfig: {}", id, currentAppConfig);

        if(failureMessage != null) {
            throw new ComponentDefinitionException(failureMessage);
        }

        return currentAppConfig;
    }

    @Override
    public void startTracking(final SatisfactionCallback satisfactionCallback) {
        if(!started.compareAndSet(false, true)) {
            return;
        }

        LOG.debug("{}: In startTracking", id);

        this.satisfactionCallback = satisfactionCallback;

        // First get the BindingNormalizedNodeSerializer OSGi service. This will be used to create a default
        // instance of the app config binding class, if necessary.

        String serviceId = id + "-binding-codec";
        bindingCodecServiceRecipe = new StaticServiceReferenceRecipe(serviceId, container,
                new MandatoryServiceReferenceMetadata(serviceId, BindingNormalizedNodeSerializer.class.getName()));
        dependendencyDesc = bindingCodecServiceRecipe.getOsgiFilter();

        bindingCodecServiceRecipe.startTracking(service -> {
            bindingSerializer = (BindingNormalizedNodeSerializer)service;
            retrieveDataBrokerService();
        });
    }

    private void retrieveDataBrokerService() {
        LOG.debug("{}: In retrieveDataBrokerService", id);

        // Get the binding DataBroker OSGi service.

        String serviceId = id + "-data-broker";
        dataBrokerServiceRecipe = new StaticServiceReferenceRecipe(serviceId, container,
                new MandatoryServiceReferenceMetadata(serviceId, DataBroker.class.getName()));
        dependendencyDesc = dataBrokerServiceRecipe.getOsgiFilter();

        dataBrokerServiceRecipe.startTracking(service -> retrieveInitialAppConfig((DataBroker)service));

    }

    private void retrieveInitialAppConfig(DataBroker dataBroker) {
        LOG.debug("{}: Got DataBroker instance - reading app config {}", id, appConfigBindingClass);

        dependendencyDesc = "Reading initial app config " + appConfigBindingClass.getSimpleName();

        // We register a DTCL to get updates and also read the app config data from the data store. If
        // the app config data is present then both the read and initial DTCN update will return it. If the
        // the data isn't present, we won't get an initial DTCN update so the read will indicate the data
        // isn't present.

        InstanceIdentifier<DataObject> appConfigPath = InstanceIdentifier.create(appConfigBindingClass);
        DataTreeIdentifier<DataObject> dataTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                appConfigPath);
        appConfigChangeListenerReg = dataBroker.registerDataTreeChangeListener(dataTreeId,
                new ClusteredDataTreeChangeListener<DataObject>() {
                    @Override
                    public void onDataTreeChanged(Collection<DataTreeModification<DataObject>> changes) {
                        onAppConfigChanged(changes);
                    }
                });

        readInitialAppConfig(dataBroker, appConfigPath);
    }

    private void readInitialAppConfig(final DataBroker dataBroker, final InstanceIdentifier<DataObject> appConfigPath) {

        final ReadOnlyTransaction readOnlyTx = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<DataObject>, ReadFailedException> future = readOnlyTx.read(
                LogicalDatastoreType.CONFIGURATION, appConfigPath);
        Futures.addCallback(future, new FutureCallback<Optional<DataObject>>() {
            @Override
            public void onSuccess(Optional<DataObject> possibleAppConfig) {
                LOG.debug("{}: Read of app config {} succeeded: {}", id, appConfigBindingClass.getName(),
                        possibleAppConfig);

                readOnlyTx.close();
                setInitialAppConfig(possibleAppConfig, appConfigPath);
            }

            @Override
            public void onFailure(Throwable t) {
                readOnlyTx.close();

                // We may have gotten the app config via the data tree change listener so only retry if not.
                if(readingInitialAppConfig.get()) {
                    LOG.warn("{}: Read of app config {} failed - retrying", id, appConfigBindingClass.getName(), t);

                    readInitialAppConfig(dataBroker, appConfigPath);
                }
            }
        });
    }

    private void onAppConfigChanged(Collection<DataTreeModification<DataObject>> changes) {
        for(DataTreeModification<DataObject> change: changes) {
            DataObjectModification<DataObject> changeRoot = change.getRootNode();
            ModificationType type = changeRoot.getModificationType();

            LOG.debug("{}: onAppConfigChanged: {}, {}", id, type, change.getRootPath());

            if(type == ModificationType.SUBTREE_MODIFIED || type == ModificationType.WRITE) {
                DataObject newAppConfig = changeRoot.getDataAfter();

                LOG.debug("New app config instance: {}", newAppConfig);

                if(!setInitialAppConfig(Optional.of(newAppConfig), change.getRootPath().getRootIdentifier()) &&
                        !Objects.equals(currentAppConfig, newAppConfig)) {
                    LOG.debug("App config was updated - scheduling container for restart");
                    // TODO - handle update
                }
            }
        }
    }

    private boolean setInitialAppConfig(Optional<DataObject> possibleAppConfig, InstanceIdentifier<DataObject> path) {
        boolean result = readingInitialAppConfig.compareAndSet(true, false);
        if(result) {
            DataObject localAppConfig;
            if(possibleAppConfig.isPresent()) {
                localAppConfig = possibleAppConfig.get();
            } else {
                // No app config data is present so create an empty instance via the bindingSerializer service.
                // This will also return default values for leafs that haven't been explicitly set.

                QName bindingQName = BindingReflections.findQName(appConfigBindingClass);
                YangInstanceIdentifier yangPath = bindingSerializer.toYangInstanceIdentifier(path);

                LOG.debug("{}: Creating app config instance from path {}, Qname: {}", id, yangPath, bindingQName);

                // We assume the app config binding class represents a top-level container.
                localAppConfig = bindingSerializer.fromNormalizedNode(yangPath,
                        ImmutableNodes.containerNode(bindingQName)).getValue();

                if(localAppConfig == null) {
                    failureMessage = String.format("%s: Could not create instance for app config binding %s",
                            id, appConfigBindingClass);
                }
            }

            LOG.debug("{}: Setting currentAppConfig instance: {}", id, localAppConfig);

            currentAppConfig = localAppConfig;
            satisfactionCallback.notifyChanged();
        }

        return result;
    }

    @Override
    public void stopTracking() {
        LOG.debug("{}: In stopTracking", id);

        stopServiceRecipes();
    }

    @Override
    public void destroy(Object instance) {
        LOG.debug("{}: In destroy", id);

        if(appConfigChangeListenerReg != null) {
            appConfigChangeListenerReg.close();
        }

        stopServiceRecipes();
    }

    private void stopServiceRecipes() {
        stopServiceRecipe(dataBrokerServiceRecipe);
        stopServiceRecipe(bindingCodecServiceRecipe);
        dataBrokerServiceRecipe = null;
        bindingCodecServiceRecipe = null;
    }

    private void stopServiceRecipe(StaticServiceReferenceRecipe recipe) {
        if(recipe != null) {
            recipe.stop();
        }
    }

    @Override
    public String getDependencyDescriptor() {
        return dependendencyDesc;
    }
}
