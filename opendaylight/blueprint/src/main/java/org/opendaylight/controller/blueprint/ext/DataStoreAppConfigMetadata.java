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
import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.blueprint.ext.DataStoreAppConfigDefaultXMLReader.ConfigURLProvider;
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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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
    static final String DEFAULT_CONFIG_FILE_NAME = "default-config-file-name";
    static final String LIST_KEY_VALUE = "list-key-value";

    private static final String DEFAULT_APP_CONFIG_FILE_PATH = "etc" + File.separator + "opendaylight" + File.separator
            + "datastore" + File.separator + "initial" + File.separator + "config";

    private final Element defaultAppConfigElement;
    private final String defaultAppConfigFileName;
    private final String appConfigBindingClassName;
    private final String appConfigListKeyValue;
    private final UpdateStrategy appConfigUpdateStrategy;
    private final AtomicBoolean readingInitialAppConfig = new AtomicBoolean(true);

    private volatile BindingContext bindingContext;
    private volatile ListenerRegistration<?> appConfigChangeListenerReg;
    private volatile DataObject currentAppConfig;

    // Note: the BindingNormalizedNodeSerializer interface is annotated as deprecated because there's an
    // equivalent interface in the mdsal project but the corresponding binding classes in the controller
    // project are still used - conversion to the mdsal binding classes hasn't occurred yet.
    private volatile BindingNormalizedNodeSerializer bindingSerializer;

    public DataStoreAppConfigMetadata(@Nonnull final String id, @Nonnull final String appConfigBindingClassName,
            @Nullable final String appConfigListKeyValue, @Nullable final String defaultAppConfigFileName,
            @Nonnull final UpdateStrategy updateStrategyValue, @Nullable final Element defaultAppConfigElement) {
        super(id);
        this.defaultAppConfigElement = defaultAppConfigElement;
        this.defaultAppConfigFileName = defaultAppConfigFileName;
        this.appConfigBindingClassName = appConfigBindingClassName;
        this.appConfigListKeyValue = appConfigListKeyValue;
        this.appConfigUpdateStrategy = updateStrategyValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(final ExtendedBlueprintContainer container) {
        super.init(container);

        Class<DataObject> appConfigBindingClass;
        try {
            Class<?> bindingClass = container.getBundleContext().getBundle().loadClass(appConfigBindingClassName);
            if (!DataObject.class.isAssignableFrom(bindingClass)) {
                throw new ComponentDefinitionException(String.format(
                        "%s: Specified app config binding class %s does not extend %s",
                        logName(), appConfigBindingClassName, DataObject.class.getName()));
            }

            appConfigBindingClass = (Class<DataObject>) bindingClass;
        } catch (final ClassNotFoundException e) {
            throw new ComponentDefinitionException(String.format("%s: Error loading app config binding class %s",
                    logName(), appConfigBindingClassName), e);
        }

        bindingContext = BindingContext.create(logName(), appConfigBindingClass, appConfigListKeyValue);
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

    private void retrieveInitialAppConfig(final DataBroker dataBroker) {
        LOG.debug("{}: Got DataBroker instance - reading app config {}", logName(), bindingContext.appConfigPath);

        setDependencyDesc("Initial app config " + bindingContext.appConfigBindingClass.getSimpleName());

        // We register a DTCL to get updates and also read the app config data from the data store. If
        // the app config data is present then both the read and initial DTCN update will return it. If the
        // the data isn't present, we won't get an initial DTCN update so the read will indicate the data
        // isn't present.

        DataTreeIdentifier<DataObject> dataTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                bindingContext.appConfigPath);
        appConfigChangeListenerReg = dataBroker.registerDataTreeChangeListener(dataTreeId,
                (ClusteredDataTreeChangeListener<DataObject>) this::onAppConfigChanged);

        readInitialAppConfig(dataBroker);
    }

    private void readInitialAppConfig(final DataBroker dataBroker) {
        @SuppressWarnings("resource") // it's closed in the callback
        final ReadOnlyTransaction readOnlyTx = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<DataObject>, ReadFailedException> future = readOnlyTx.read(
                LogicalDatastoreType.CONFIGURATION, bindingContext.appConfigPath);
        Futures.addCallback(future, new FutureCallback<Optional<DataObject>>() {
            @Override
            public void onSuccess(final Optional<DataObject> possibleAppConfig) {
                LOG.debug("{}: Read of app config {} succeeded: {}", logName(), bindingContext
                        .appConfigBindingClass.getName(), possibleAppConfig);

                readOnlyTx.close();
                setInitialAppConfig(possibleAppConfig);
            }

            @Override
            public void onFailure(final Throwable failure) {
                readOnlyTx.close();

                // We may have gotten the app config via the data tree change listener so only retry if not.
                if (readingInitialAppConfig.get()) {
                    LOG.warn("{}: Read of app config {} failed - retrying", logName(),
                            bindingContext.appConfigBindingClass.getName(), failure);

                    readInitialAppConfig(dataBroker);
                }
            }
        });
    }

    private void onAppConfigChanged(final Collection<DataTreeModification<DataObject>> changes) {
        for (DataTreeModification<DataObject> change: changes) {
            DataObjectModification<DataObject> changeRoot = change.getRootNode();
            ModificationType type = changeRoot.getModificationType();

            LOG.debug("{}: onAppConfigChanged: {}, {}", logName(), type, change.getRootPath());

            if (type == ModificationType.SUBTREE_MODIFIED || type == ModificationType.WRITE) {
                DataObject newAppConfig = changeRoot.getDataAfter();

                LOG.debug("New app config instance: {}, previous: {}", newAppConfig, currentAppConfig);

                if (!setInitialAppConfig(Optional.of(newAppConfig))
                        && !Objects.equals(currentAppConfig, newAppConfig)) {
                    LOG.debug("App config was updated");

                    if (appConfigUpdateStrategy == UpdateStrategy.RELOAD) {
                        restartContainer();
                    }
                }
            } else if (type == ModificationType.DELETE) {
                LOG.debug("App config was deleted");

                if (appConfigUpdateStrategy == UpdateStrategy.RELOAD) {
                    restartContainer();
                }
            }
        }
    }

    private boolean setInitialAppConfig(final Optional<DataObject> possibleAppConfig) {
        boolean result = readingInitialAppConfig.compareAndSet(true, false);
        if (result) {
            DataObject localAppConfig;
            if (possibleAppConfig.isPresent()) {
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
        try {
            @SuppressWarnings("resource")
            ConfigURLProvider inputStreamProvider = appConfigFileName -> {
                File appConfigFile = new File(DEFAULT_APP_CONFIG_FILE_PATH, appConfigFileName);
                LOG.debug("{}: parsePossibleDefaultAppConfigXMLFile looking for file {}", logName(),
                        appConfigFile.getAbsolutePath());

                if (!appConfigFile.exists()) {
                    return Optional.absent();
                }

                LOG.debug("{}: Found file {}", logName(), appConfigFile.getAbsolutePath());

                return Optional.of(appConfigFile.toURI().toURL());
            };

            DataStoreAppConfigDefaultXMLReader<?> reader = new DataStoreAppConfigDefaultXMLReader(logName(),
                    defaultAppConfigFileName, getOSGiService(SchemaService.class), bindingSerializer, bindingContext,
                    inputStreamProvider);
            return reader.createDefaultInstance((schemaContext, dataSchema) -> {
                // Fallback if file cannot be read, try XML from Config
                NormalizedNode<?, ?> dataNode = parsePossibleDefaultAppConfigElement(schemaContext, dataSchema);
                if (dataNode == null) {
                    // or, as last resort, defaults from the model
                    return bindingContext.newDefaultNode(dataSchema);
                } else {
                    return dataNode;
                }
            });

        } catch (final ConfigXMLReaderException e) {
            if (e.getCause() == null) {
                setFailureMessage(e.getMessage());
            } else {
                setFailure(e.getMessage(), e);
            }
            return null;
        }
    }

    @Nullable
    private NormalizedNode<?, ?> parsePossibleDefaultAppConfigElement(final SchemaContext schemaContext,
            final DataSchemaNode dataSchema) {
        if (defaultAppConfigElement == null) {
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

    @Override
    public void destroy(final Object instance) {
        super.destroy(instance);

        if (appConfigChangeListenerReg != null) {
            appConfigChangeListenerReg.close();
            appConfigChangeListenerReg = null;
        }
    }

}
