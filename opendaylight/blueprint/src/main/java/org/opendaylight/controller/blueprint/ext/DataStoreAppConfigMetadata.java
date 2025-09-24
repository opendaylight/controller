/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.stream.XMLStreamException;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModification.WithDataAfter;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaTreeInference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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

    private static final Path DEFAULT_APP_CONFIG_FILE_PATH =
        Path.of("etc", "opendaylight", "datastore", "initial", "config");

    private final Element defaultAppConfigElement;
    private final String defaultAppConfigFileName;
    private final String appConfigBindingClassName;
    private final String appConfigListKeyValue;
    private final UpdateStrategy appConfigUpdateStrategy;
    private final AtomicBoolean readingInitialAppConfig = new AtomicBoolean(true);

    private volatile BindingContext bindingContext;
    private volatile Registration appConfigChangeListenerReg;
    private volatile DataObject currentAppConfig;

    // Note: the BindingNormalizedNodeSerializer interface is annotated as deprecated because there's an
    // equivalent interface in the mdsal project but the corresponding binding classes in the controller
    // project are still used - conversion to the mdsal binding classes hasn't occurred yet.
    private volatile BindingNormalizedNodeSerializer bindingSerializer;

    public DataStoreAppConfigMetadata(final String id, final @NonNull String appConfigBindingClassName,
            final @Nullable String appConfigListKeyValue, final @Nullable String defaultAppConfigFileName,
            final @NonNull UpdateStrategy updateStrategyValue, final @Nullable Element defaultAppConfigElement) {
        super(id);
        this.defaultAppConfigElement = defaultAppConfigElement;
        this.defaultAppConfigFileName = defaultAppConfigFileName;
        this.appConfigBindingClassName = appConfigBindingClassName;
        this.appConfigListKeyValue = appConfigListKeyValue;
        appConfigUpdateStrategy = updateStrategyValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(final ExtendedBlueprintContainer container) {
        super.init(container);

        Class<DataObject> appConfigBindingClass;
        try {
            Class<?> bindingClass = container.getBundleContext().getBundle().loadClass(appConfigBindingClassName);
            if (!ChildOf.class.isAssignableFrom(bindingClass)) {
                throw new ComponentDefinitionException(String.format(
                        "%s: Specified app config binding class %s does not extend %s",
                        logName(), appConfigBindingClassName, ChildOf.class.getName()));
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

        // We register a DTCL to get updates and also read the app config data from the data store. If the app config
        // data is present then both the read and initial DTCN update will return it. If the the data isn't present, we
        // will not get an initial DTCN update so the read will indicate the data is not present.
        appConfigChangeListenerReg = dataBroker.registerTreeChangeListener(
            LogicalDatastoreType.CONFIGURATION, bindingContext.appConfigPath, this::onAppConfigChanged);

        readInitialAppConfig(dataBroker);
    }

    private void readInitialAppConfig(final DataBroker dataBroker) {
        final FluentFuture<Optional<DataObject>> future;
        try (ReadTransaction readOnlyTx = dataBroker.newReadOnlyTransaction()) {
            future = readOnlyTx.read(LogicalDatastoreType.CONFIGURATION, bindingContext.appConfigPath);
        }

        future.addCallback(new FutureCallback<Optional<DataObject>>() {
            @Override
            public void onSuccess(final Optional<DataObject> possibleAppConfig) {
                LOG.debug("{}: Read of app config {} succeeded: {}", logName(),
                    bindingContext.appConfigBindingClass.getName(), possibleAppConfig);

                setInitialAppConfig(possibleAppConfig);
            }

            @Override
            public void onFailure(final Throwable failure) {
                // We may have gotten the app config via the data tree change listener so only retry if not.
                if (readingInitialAppConfig.get()) {
                    LOG.warn("{}: Read of app config {} failed - retrying", logName(),
                            bindingContext.appConfigBindingClass.getName(), failure);

                    readInitialAppConfig(dataBroker);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void onAppConfigChanged(final Collection<DataTreeModification<DataObject>> changes) {
        for (var change : changes) {
            switch (change.getRootNode()) {
                case WithDataAfter<DataObject> present -> {
                    final var newAppConfig = present.dataAfter();
                    LOG.debug("New app config instance: {}, previous: {}", newAppConfig, currentAppConfig);

                    if (!setInitialAppConfig(Optional.of(newAppConfig))
                            && !Objects.equals(currentAppConfig, newAppConfig)) {
                        LOG.debug("App config was updated");

                        if (appConfigUpdateStrategy == UpdateStrategy.RELOAD) {
                            restartContainer();
                        }
                    }
                }
                case DataObjectDeleted<DataObject> deleted -> {
                    LOG.debug("App config was deleted");

                    if (appConfigUpdateStrategy == UpdateStrategy.RELOAD) {
                        restartContainer();
                    }
                }
            }
        }
    }

    private boolean setInitialAppConfig(final Optional<DataObject> possibleAppConfig) {
        boolean result = readingInitialAppConfig.compareAndSet(true, false);
        if (result) {
            DataObject localAppConfig;
            if (possibleAppConfig.isPresent()) {
                localAppConfig = possibleAppConfig.orElseThrow();
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
            final var reader = new DataStoreAppConfigDefaultXMLReader<>(logName(), defaultAppConfigFileName,
                getOSGiService(DOMSchemaService.class), bindingSerializer, bindingContext, appConfigFileName -> {
                    final var appConfigFile = DEFAULT_APP_CONFIG_FILE_PATH.resolve(appConfigFileName);
                    final var absPath = appConfigFile.toAbsolutePath();
                    LOG.debug("{}: parsePossibleDefaultAppConfigXMLFile looking for file {}", logName(), absPath);

                    if (Files.exists(appConfigFile)) {
                        LOG.debug("{}: Found file {}", logName(), absPath);
                        return Optional.of(appConfigFile.toUri().toURL());
                    }
                    return Optional.empty();
                });
            return reader.createDefaultInstance(dataSchema -> {
                // Fallback if file cannot be read, try XML from Config
                final var dataNode = parsePossibleDefaultAppConfigElement(dataSchema);
                if (dataNode != null) {
                    return dataNode;
                }
                // or, as last resort, defaults from the model
                return bindingContext.newDefaultNode(dataSchema);
            });

        } catch (ConfigXMLReaderException | IOException | SAXException | XMLStreamException | URISyntaxException e) {
            if (e.getCause() == null) {
                setFailureMessage(e.getMessage());
            } else {
                setFailure(e.getMessage(), e);
            }
            return null;
        }
    }

    private @Nullable NormalizedNode parsePossibleDefaultAppConfigElement(final SchemaTreeInference dataSchema)
            throws URISyntaxException, IOException, SAXException, XMLStreamException {
        if (defaultAppConfigElement == null) {
            return null;
        }

        LOG.debug("{}: parsePossibleDefaultAppConfigElement for {}", logName(), bindingContext.bindingQName);

        LOG.debug("{}: Got app config schema: {}", logName(), dataSchema);

        NormalizedNode dataNode = bindingContext.parseDataElement(defaultAppConfigElement, dataSchema);

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
