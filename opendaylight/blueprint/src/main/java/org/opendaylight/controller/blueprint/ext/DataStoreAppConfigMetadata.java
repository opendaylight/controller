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
import com.google.common.base.Strings;
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
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
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
public class DataStoreAppConfigMetadata extends AbstractDependentComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(DataStoreAppConfigMetadata.class);

    static final String BINDING_CLASS = "binding-class";
    static final String DEFAULT_CONFIG = "default-config";
    static final String LIST_KEY_VALUE = "list-key-value";

    private final Element defaultAppConfigElement;
    private final String appConfigBindingClassName;
    private final String appConfigListKeyValue;
    private final AtomicBoolean readingInitialAppConfig = new AtomicBoolean(true);

    private volatile BindingContext bindingContext;
    private volatile ListenerRegistration<?> appConfigChangeListenerReg;
    private volatile DataObject currentAppConfig;

    // Note: the BindingNormalizedNodeSerializer interface is annotated as deprecated because there's an
    // equivalent interface in the mdsal project but the corresponding binding classes in the controller
    // project are still used - conversion to the mdsal binding classes hasn't occurred yet.
    private volatile BindingNormalizedNodeSerializer bindingSerializer;

    public DataStoreAppConfigMetadata(@Nonnull String id, @Nonnull String appConfigBindingClassName,
            @Nullable String appConfigListKeyValue, @Nullable Element defaultAppConfigElement) {
        super(id);
        this.defaultAppConfigElement = defaultAppConfigElement;
        this.appConfigBindingClassName = appConfigBindingClassName;
        this.appConfigListKeyValue = appConfigListKeyValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(ExtendedBlueprintContainer container) {
        super.init(container);

        Class<DataObject> appConfigBindingClass;
        try {
            Class<?> bindingClass = container.getBundleContext().getBundle().loadClass(appConfigBindingClassName);
            if(!DataObject.class.isAssignableFrom(bindingClass)) {
                throw new ComponentDefinitionException(String.format(
                        "%s: Specified app config binding class %s does not extend %s",
                        logName(), appConfigBindingClassName, DataObject.class.getName()));
            }

            appConfigBindingClass = (Class<DataObject>) bindingClass;
        } catch(ClassNotFoundException e) {
            throw new ComponentDefinitionException(String.format("%s: Error loading app config binding class %s",
                    logName(), appConfigBindingClassName), e);
        }

        if(Identifiable.class.isAssignableFrom(appConfigBindingClass)) {
            // The binding class corresponds to a yang list.
            if(Strings.isNullOrEmpty(appConfigListKeyValue)) {
                throw new ComponentDefinitionException(String.format(
                        "%s: App config binding class %s represents a yang list therefore \"%s\" must be specified",
                        logName(), appConfigBindingClassName, LIST_KEY_VALUE));
            }

            try {
                bindingContext = ListBindingContext.newInstance(appConfigBindingClass, appConfigListKeyValue);
            } catch(Exception e) {
                throw new ComponentDefinitionException(String.format(
                        "%s: Error initializing for app config list binding class %s",
                        logName(), appConfigBindingClassName), e);
            }

        } else {
            bindingContext = new ContainerBindingContext(appConfigBindingClass);
        }
    }

    @Override
    public Object create() throws ComponentDefinitionException {
        LOG.debug("{}: In create - currentAppConfig: {}", logName(), currentAppConfig);

        super.onCreate();

        return currentAppConfig;
    }

    @Override
    protected void startTracking() {
        // First get the BindingNormalizedNodeSerializer OSGi service. This will be used to create a default
        // instance of the app config binding class, if necessary.

        retrieveService("binding-codec", BindingNormalizedNodeSerializer.class, service -> {
            bindingSerializer = (BindingNormalizedNodeSerializer)service;
            retrieveDataBrokerService();
        });
    }

    private void retrieveDataBrokerService() {
        LOG.debug("{}: In retrieveDataBrokerService", logName());

        // Get the binding DataBroker OSGi service.

        retrieveService("data-broker", DataBroker.class, service -> retrieveInitialAppConfig((DataBroker)service));
    }

    private void retrieveInitialAppConfig(DataBroker dataBroker) {
        LOG.debug("{}: Got DataBroker instance - reading app config {}", logName(), bindingContext.appConfigPath);

        setDependendencyDesc("Initial app config " + bindingContext.appConfigBindingClass.getSimpleName());

        // We register a DTCL to get updates and also read the app config data from the data store. If
        // the app config data is present then both the read and initial DTCN update will return it. If the
        // the data isn't present, we won't get an initial DTCN update so the read will indicate the data
        // isn't present.

        DataTreeIdentifier<DataObject> dataTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                bindingContext.appConfigPath);
        appConfigChangeListenerReg = dataBroker.registerDataTreeChangeListener(dataTreeId,
                new ClusteredDataTreeChangeListener<DataObject>() {
                    @Override
                    public void onDataTreeChanged(Collection<DataTreeModification<DataObject>> changes) {
                        onAppConfigChanged(changes);
                    }
                });

        readInitialAppConfig(dataBroker);
    }

    private void readInitialAppConfig(final DataBroker dataBroker) {

        final ReadOnlyTransaction readOnlyTx = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<DataObject>, ReadFailedException> future = readOnlyTx.read(
                LogicalDatastoreType.CONFIGURATION, bindingContext.appConfigPath);
        Futures.addCallback(future, new FutureCallback<Optional<DataObject>>() {
            @Override
            public void onSuccess(Optional<DataObject> possibleAppConfig) {
                LOG.debug("{}: Read of app config {} succeeded: {}", logName(), bindingContext.appConfigBindingClass.getName(),
                        possibleAppConfig);

                readOnlyTx.close();
                setInitialAppConfig(possibleAppConfig);
            }

            @Override
            public void onFailure(Throwable t) {
                readOnlyTx.close();

                // We may have gotten the app config via the data tree change listener so only retry if not.
                if(readingInitialAppConfig.get()) {
                    LOG.warn("{}: Read of app config {} failed - retrying", logName(),
                            bindingContext.appConfigBindingClass.getName(), t);

                    readInitialAppConfig(dataBroker);
                }
            }
        });
    }

    private void onAppConfigChanged(Collection<DataTreeModification<DataObject>> changes) {
        for(DataTreeModification<DataObject> change: changes) {
            DataObjectModification<DataObject> changeRoot = change.getRootNode();
            ModificationType type = changeRoot.getModificationType();

            LOG.debug("{}: onAppConfigChanged: {}, {}", logName(), type, change.getRootPath());

            if(type == ModificationType.SUBTREE_MODIFIED || type == ModificationType.WRITE) {
                DataObject newAppConfig = changeRoot.getDataAfter();

                LOG.debug("New app config instance: {}, previous: {}", newAppConfig, currentAppConfig);

                if(!setInitialAppConfig(Optional.of(newAppConfig)) &&
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

    private boolean setInitialAppConfig(Optional<DataObject> possibleAppConfig) {
        boolean result = readingInitialAppConfig.compareAndSet(true, false);
        if(result) {
            DataObject localAppConfig;
            if(possibleAppConfig.isPresent()) {
                localAppConfig = possibleAppConfig.get();
            } else {
                // No app config data is present so create an empty instance via the bindingSerializer service.
                // This will also return default values for leafs that haven't been explicitly set.
                localAppConfig = createDefaultInstance();
            }

            LOG.debug("{}: Setting currentAppConfig instance: {}", logName(), localAppConfig);

            // Now publish the app config instance to the volatile field and notify the callback to let the
            // container know our dependency is now satisfied.
            currentAppConfig = localAppConfig;
            setSatisfied();
        }

        return result;
    }

    private DataObject createDefaultInstance() {
        YangInstanceIdentifier yangPath = bindingSerializer.toYangInstanceIdentifier(bindingContext.appConfigPath);

        LOG.debug("{}: Creating app config instance from path {}, Qname: {}", logName(), yangPath, bindingContext.bindingQName);

        SchemaService schemaService = getOSGiService(SchemaService.class);
        if(schemaService == null) {
            setFailureMessage(String.format("%s: Could not obtain the SchemaService OSGi service", logName()));
            return null;
        }

        SchemaContext schemaContext = schemaService.getGlobalContext();

        Module module = schemaContext.findModuleByNamespaceAndRevision(bindingContext.bindingQName.getNamespace(),
                bindingContext.bindingQName.getRevision());
        if(module == null) {
            setFailureMessage(String.format("%s: Could not obtain the module schema for namespace %s, revision %s",
                    logName(), bindingContext.bindingQName.getNamespace(), bindingContext.bindingQName.getRevision()));
            return null;
        }

        DataSchemaNode dataSchema = module.getDataChildByName(bindingContext.bindingQName);
        if(dataSchema == null) {
            setFailureMessage(String.format("%s: Could not obtain the schema for %s", logName(), bindingContext.bindingQName));
            return null;
        }

        if(!bindingContext.schemaType.isAssignableFrom(dataSchema.getClass())) {
            setFailureMessage(String.format("%s: Expected schema type %s for %s but actual type is %s", logName(),
                    bindingContext.schemaType, bindingContext.bindingQName, dataSchema.getClass()));
            return null;
        }

        NormalizedNode<?, ?> dataNode = parsePossibleDefaultAppConfigElement(schemaContext, dataSchema);
        if(dataNode == null) {
            dataNode = bindingContext.newDefaultNode(dataSchema);
        }

        DataObject appConfig = bindingSerializer.fromNormalizedNode(yangPath, dataNode).getValue();

        if(appConfig == null) {
            // This shouldn't happen but need to handle it in case...
            setFailureMessage(String.format("%s: Could not create instance for app config binding %s",
                    logName(), bindingContext.appConfigBindingClass));
        }

        return appConfig;
    }

    @Nullable
    private NormalizedNode<?, ?> parsePossibleDefaultAppConfigElement(SchemaContext schemaContext,
            DataSchemaNode dataSchema) {
        if(defaultAppConfigElement == null) {
            return null;
        }

        LOG.debug("{}: parsePossibleDefaultAppConfigElement for {}", logName(), bindingContext.bindingQName);

        DomToNormalizedNodeParserFactory parserFactory = DomToNormalizedNodeParserFactory.getInstance(
                XmlUtils.DEFAULT_XML_CODEC_PROVIDER, schemaContext);


        LOG.debug("{}: Got app config schema: {}", logName(), dataSchema);

        NormalizedNode<?, ?> dataNode = bindingContext.parseDataElement(defaultAppConfigElement, dataSchema,
                parserFactory);

        LOG.debug("{}: Parsed data node: {}", logName(), dataNode);

        return dataNode;
    }

    private void restartContainer() {
        BlueprintContainerRestartService restartService = getOSGiService(BlueprintContainerRestartService.class);
        if(restartService != null) {
            restartService.restartContainerAndDependents(container().getBundleContext().getBundle());
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T getOSGiService(Class<T> serviceInterface) {
        try {
            ServiceReference<T> serviceReference =
                    container().getBundleContext().getServiceReference(serviceInterface);
            if(serviceReference == null) {
                LOG.warn("{}: {} reference not found", logName(), serviceInterface.getSimpleName());
                return null;
            }

            T service = (T)container().getService(serviceReference);
            if(service == null) {
                // This could happen on shutdown if the service was already unregistered so we log as debug.
                LOG.debug("{}: {} was not found", logName(), serviceInterface.getSimpleName());
            }

            return service;
        } catch(IllegalStateException e) {
            // This is thrown if the BundleContext is no longer valid which is possible on shutdown so we
            // log as debug.
            LOG.debug("{}: Error obtaining {}", logName(), serviceInterface.getSimpleName(), e);
        }

        return null;
    }

    @Override
    public void destroy(Object instance) {
        super.destroy(instance);

        if(appConfigChangeListenerReg != null) {
            appConfigChangeListenerReg.close();
            appConfigChangeListenerReg = null;
        }
    }

    /**
     * Internal base class to abstract binding type-specific behavior.
     */
    private static abstract class BindingContext {
        final InstanceIdentifier<DataObject> appConfigPath;
        final Class<DataObject> appConfigBindingClass;
        final Class<? extends DataSchemaNode> schemaType;
        final QName bindingQName;

        protected BindingContext(Class<DataObject> appConfigBindingClass, InstanceIdentifier<DataObject> appConfigPath,
                Class<? extends DataSchemaNode> schemaType) {
            this.appConfigBindingClass = appConfigBindingClass;
            this.appConfigPath = appConfigPath;
            this.schemaType = schemaType;

            bindingQName = BindingReflections.findQName(appConfigBindingClass);
        }

        abstract NormalizedNode<?, ?> parseDataElement(Element element, DataSchemaNode dataSchema,
                DomToNormalizedNodeParserFactory parserFactory);

        abstract NormalizedNode<?, ?> newDefaultNode(DataSchemaNode dataSchema);
    }

    /**
     * BindingContext implementation for a container binding.
     */
    private static class ContainerBindingContext extends BindingContext {
        ContainerBindingContext(Class<DataObject> appConfigBindingClass) {
            super(appConfigBindingClass, InstanceIdentifier.create(appConfigBindingClass), ContainerSchemaNode.class);
        }

        @Override
        NormalizedNode<?, ?> newDefaultNode(DataSchemaNode dataSchema) {
            return ImmutableNodes.containerNode(bindingQName);
        }

        @Override
        NormalizedNode<?, ?> parseDataElement(Element element, DataSchemaNode dataSchema,
                DomToNormalizedNodeParserFactory parserFactory) {
            return parserFactory.getContainerNodeParser().parse(Collections.singletonList(element),
                    (ContainerSchemaNode)dataSchema);
        }
    }

    /**
     * BindingContext implementation for a list binding.
     */
    private static class ListBindingContext extends BindingContext {
        final String appConfigListKeyValue;

        ListBindingContext(Class<DataObject> appConfigBindingClass, InstanceIdentifier<DataObject> appConfigPath,
                String appConfigListKeyValue) {
            super(appConfigBindingClass, appConfigPath, ListSchemaNode.class);
            this.appConfigListKeyValue = appConfigListKeyValue;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static ListBindingContext newInstance(Class<DataObject> bindingClass, String listKeyValue)
                throws Exception {
            // We assume the yang list key type is string.
            Identifier keyInstance = (Identifier) bindingClass.getMethod("getKey").getReturnType().
                    getConstructor(String.class).newInstance(listKeyValue);
            InstanceIdentifier appConfigPath = InstanceIdentifier.builder((Class)bindingClass, keyInstance).build();
            return new ListBindingContext(bindingClass, appConfigPath, listKeyValue);
        }

        @Override
        NormalizedNode<?, ?> newDefaultNode(DataSchemaNode dataSchema) {
            // We assume there's only one key for the list.
            List<QName> keys = ((ListSchemaNode)dataSchema).getKeyDefinition();
            Preconditions.checkArgument(keys.size() == 1, "Expected only 1 key for list %s", appConfigBindingClass);
            QName listKeyQName = keys.iterator().next();
            return ImmutableNodes.mapEntryBuilder(bindingQName, listKeyQName, appConfigListKeyValue).build();
        }

        @Override
        NormalizedNode<?, ?> parseDataElement(Element element, DataSchemaNode dataSchema,
                DomToNormalizedNodeParserFactory parserFactory) {
            return parserFactory.getMapEntryNodeParser().parse(Collections.singletonList(element),
                    (ListSchemaNode)dataSchema);
        }
    }
}
