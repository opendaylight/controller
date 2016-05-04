/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.aries.blueprint.di.AbstractRecipe;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.ext.DependentComponentFactoryMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.blueprint.BlueprintContainerRestartService;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Factory metadata corresponding to the "clustered-app-config" element that obtains an application's
 * config data from the data store and provides the binding DataObject instance to the Blueprint container
 * as a bean. In addition registers a DataTreeChangeListener to restart the Blueprint container when the
 * config data is changed.
 *
 * @author Thomas Pantelis
 */
public class DataStoreAppConfigMetadata implements DependentComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(DataStoreAppConfigMetadata.class);

    private final String id;
    private final String appConfigBindingClassName;
    private final Element defaultAppConfigElement;
    private final AtomicBoolean readingInitialAppConfig = new AtomicBoolean(true);
    private final AtomicBoolean started = new AtomicBoolean();

    private volatile Class<DataObject> appConfigBindingClass;
    private volatile ExtendedBlueprintContainer container;
    private volatile StaticServiceReferenceRecipe dataBrokerServiceRecipe;
    private volatile StaticServiceReferenceRecipe bindingCodecServiceRecipe;
    private volatile ListenerRegistration<?> appConfigChangeListenerReg;
    private volatile DataObject currentAppConfig;
    private volatile SatisfactionCallback satisfactionCallback;
    private volatile String failureMessage;
    private volatile String dependendencyDesc;

    // Note: the BindingNormalizedNodeSerializer interface is annotated as deprecated because there's an
    // equivalent interface in the mdsal project but the corresponding binding classes in the controller
    // project are still used - conversion to the mdsal binding classes hasn't occurred yet.
    private volatile BindingNormalizedNodeSerializer bindingSerializer;

    public DataStoreAppConfigMetadata(@Nonnull String id, @Nonnull String appConfigBindingClassName,
            @Nullable  Element defaultAppConfigElement) {
        this.id = Preconditions.checkNotNull(id);
        this.appConfigBindingClassName = Preconditions.checkNotNull(appConfigBindingClassName);
        this.defaultAppConfigElement = defaultAppConfigElement;
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
                    id, appConfigBindingClassName), e);
        }
    }

    @Override
    public Object create() throws ComponentDefinitionException {
        LOG.debug("{}: In create - currentAppConfig: {}", id, currentAppConfig);

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
            LOG.debug("{}: setPrototype to false", id);
            ((AbstractRecipe)myRecipe).setPrototype(false);
        } else {
            LOG.warn("{}: Recipe is null or not an AbstractRecipe", id);
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

        bindingCodecServiceRecipe = new StaticServiceReferenceRecipe(id + "-binding-codec", container,
                BindingNormalizedNodeSerializer.class.getName());
        dependendencyDesc = bindingCodecServiceRecipe.getOsgiFilter();

        bindingCodecServiceRecipe.startTracking(service -> {
            bindingSerializer = (BindingNormalizedNodeSerializer)service;
            retrieveDataBrokerService();
        });
    }

    private void retrieveDataBrokerService() {
        LOG.debug("{}: In retrieveDataBrokerService", id);

        // Get the binding DataBroker OSGi service.

        dataBrokerServiceRecipe = new StaticServiceReferenceRecipe(id + "-data-broker", container,
                DataBroker.class.getName());
        dependendencyDesc = dataBrokerServiceRecipe.getOsgiFilter();

        dataBrokerServiceRecipe.startTracking(service -> retrieveInitialAppConfig((DataBroker)service));

    }

    private void retrieveInitialAppConfig(DataBroker dataBroker) {
        LOG.debug("{}: Got DataBroker instance - reading app config {}", id, appConfigBindingClass);

        dependendencyDesc = "Initial app config " + appConfigBindingClass.getSimpleName();

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

                LOG.debug("New app config instance: {}, previous: {}", newAppConfig, currentAppConfig);

                if(!setInitialAppConfig(Optional.of(newAppConfig), change.getRootPath().getRootIdentifier()) &&
                        !Objects.equals(currentAppConfig, newAppConfig)) {
                    LOG.debug("App config was updated - scheduling container for restart");

                    restartContainer();
                }
            } else if(type == ModificationType.DELETE) {
                LOG.debug("App config was deleted - scheduling container for restart");

                restartContainer();
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
                localAppConfig = createDefaultInstance(path);
            }

            LOG.debug("{}: Setting currentAppConfig instance: {}", id, localAppConfig);

            // Now publish the app config instance to the volatile field and notify the callback to let the
            // container know our dependency is now satisfied.
            currentAppConfig = localAppConfig;
            satisfactionCallback.notifyChanged();
        }

        return result;
    }

    private DataObject createDefaultInstance(InstanceIdentifier<DataObject> path) {
        QName bindingQName = BindingReflections.findQName(appConfigBindingClass);
        YangInstanceIdentifier yangPath = bindingSerializer.toYangInstanceIdentifier(path);

        LOG.debug("{}: Creating app config instance from path {}, Qname: {}", id, yangPath, bindingQName);

        // We assume the app config binding class represents a top-level container.
        ContainerNode containerNode = parsePossibleDefaultAppConfigElement(bindingQName);
        if(containerNode == null) {
            containerNode = ImmutableNodes.containerNode(bindingQName);
        }

        DataObject appConfig = bindingSerializer.fromNormalizedNode(yangPath, containerNode).getValue();

        if(appConfig == null) {
            // This shouldn't happen but need to handle it in case...
            failureMessage = String.format("%s: Could not create instance for app config binding %s",
                    id, appConfigBindingClass);
            return null;
        }

        return appConfig;
    }

    @Nullable
    private ContainerNode parsePossibleDefaultAppConfigElement(QName bindingQName) {
        if(defaultAppConfigElement == null) {
            return null;
        }

        LOG.debug("{}: parsePossibleDefaultAppConfigElement for {}", id, bindingQName);

        SchemaService schemaService = getOSGiService(SchemaService.class);
        if(schemaService == null) {
            failureMessage = String.format("%s: Could not obtain the SchemaService OSGi service", id);
            return null;
        }

        SchemaContext schemaContext = schemaService.getGlobalContext();

        DomToNormalizedNodeParserFactory parserFactory = DomToNormalizedNodeParserFactory.getInstance(
                XmlUtils.DEFAULT_XML_CODEC_PROVIDER, schemaContext);

        Module module = schemaContext.findModuleByNamespaceAndRevision(bindingQName.getNamespace(),
                bindingQName.getRevision());
        if(module == null) {
            failureMessage = String.format("%s: Could not obtain the module schema for namespace %s, revision %s",
                    id, bindingQName.getNamespace(), bindingQName.getRevision());
            return null;
        }

        DataSchemaNode containerSchema = module.getDataChildByName(bindingQName);
        if(containerSchema == null) {
            failureMessage = String.format("%s: Could not obtain the schema for %s", id, bindingQName);
            return null;
        }

        if(!(containerSchema instanceof ContainerSchemaNode)) {
            failureMessage = String.format("%s: Schema for %s is not a container: %s", id, bindingQName, containerSchema);
            return null;
        }

        LOG.debug("{}: Got container schema: {}", id, containerSchema);

        ContainerNode containerNode = parserFactory.getContainerNodeParser().parse(
                Collections.singletonList(defaultAppConfigElement), (ContainerSchemaNode) containerSchema);

        LOG.debug("{}: Parsed container node: {}", id, containerNode);

        return containerNode;
    }

    private void restartContainer() {
        BlueprintContainerRestartService restartService = getOSGiService(BlueprintContainerRestartService.class);
        if(restartService != null) {
            restartService.restartContainerAndDependents(container.getBundleContext().getBundle());
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T getOSGiService(Class<T> serviceInterface) {
        try {
            ServiceReference<T> serviceReference =
                    container.getBundleContext().getServiceReference(serviceInterface);
            if(serviceReference == null) {
                LOG.warn("{}: {} reference not found", id, serviceInterface.getSimpleName());
                return null;
            }

            T service = (T)container.getService(serviceReference);
            if(service == null) {
                // This could happen on shutdown if the service was already unregistered so we log as debug.
                LOG.debug("{}: {} was not found", id, serviceInterface.getSimpleName());
                return null;
            }

            return service;
        } catch(IllegalStateException e) {
            // This is thrown if the BundleContext is no longer valid which is possible on shutdown so we
            // log as debug.
            LOG.debug("{}: Error obtaining {}", id, serviceInterface.getSimpleName(), e);
        }

        return null;
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
            appConfigChangeListenerReg = null;
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
