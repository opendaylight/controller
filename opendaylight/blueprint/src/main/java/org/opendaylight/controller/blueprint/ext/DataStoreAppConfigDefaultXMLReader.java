/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaTreeInference;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * DataObject XML file reader used by {@link DataStoreAppConfigMetadata}.
 * Available as a standalone class to make it easy to write unit tests which can
 * catch malformed default "clustered-app-conf" config data XML files in
 * downstream projects.
 *
 * @author Thomas Pantelis (originally; re-factored by Michael Vorburger.ch)
 */
public class DataStoreAppConfigDefaultXMLReader<T extends DataObject> {

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreAppConfigDefaultXMLReader.class);

    private final String logName;
    private final String defaultAppConfigFileName;
    private final DOMSchemaService schemaService;
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final BindingContext bindingContext;
    private final ConfigURLProvider inputStreamProvider;

    @FunctionalInterface
    public interface FallbackConfigProvider {
        NormalizedNode get(SchemaTreeInference dataSchema)
            throws IOException, XMLStreamException, ParserConfigurationException, SAXException, URISyntaxException;
    }

    @FunctionalInterface
    public interface ConfigURLProvider {
        Optional<URL> getURL(String appConfigFileName) throws IOException;
    }

    public DataStoreAppConfigDefaultXMLReader(
            final String logName,
            final String defaultAppConfigFileName,
            final DOMSchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingSerializer,
            final BindingContext bindingContext,
            final ConfigURLProvider inputStreamProvider) {

        this.logName = logName;
        this.defaultAppConfigFileName = defaultAppConfigFileName;
        this.schemaService = schemaService;
        this.bindingSerializer = bindingSerializer;
        this.bindingContext = bindingContext;
        this.inputStreamProvider = inputStreamProvider;
    }

    public DataStoreAppConfigDefaultXMLReader(
            final Class<?> testClass,
            final String defaultAppConfigFileName,
            final DOMSchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingSerializer,
            final Class<T> klass) {
        this(testClass.getName(), defaultAppConfigFileName, schemaService, bindingSerializer,
            BindingContext.create(testClass.getName(), klass, null),
            appConfigFileName -> Optional.of(getURL(testClass, defaultAppConfigFileName)));
    }

    private static URL getURL(final Class<?> testClass, final String defaultAppConfigFileName) {
        return Resources.getResource(testClass, defaultAppConfigFileName);
    }

    public T createDefaultInstance() throws ConfigXMLReaderException, ParserConfigurationException, XMLStreamException,
            IOException, SAXException, URISyntaxException {
        return createDefaultInstance(dataSchema -> {
            throw new IllegalArgumentException(
                "Failed to read XML (not creating model from defaults as runtime would, for better clarity in tests)");
        });
    }

    @SuppressWarnings("unchecked")
    public T createDefaultInstance(final FallbackConfigProvider fallback) throws ConfigXMLReaderException,
            URISyntaxException, ParserConfigurationException, XMLStreamException, SAXException, IOException {
        YangInstanceIdentifier yangPath = bindingSerializer.toYangInstanceIdentifier(bindingContext.appConfigPath);

        LOG.debug("{}: Creating app config instance from path {}, Qname: {}", logName, yangPath,
                bindingContext.bindingQName);

        checkNotNull(schemaService, "%s: Could not obtain the SchemaService OSGi service", logName);

        EffectiveModelContext schemaContext = schemaService.getGlobalContext();

        Module module = schemaContext.findModule(bindingContext.bindingQName.getModule()).orElse(null);
        checkNotNull(module, "%s: Could not obtain the module schema for namespace %s, revision %s",
                logName, bindingContext.bindingQName.getNamespace(), bindingContext.bindingQName.getRevision());

        final SchemaInferenceStack schemaStack = SchemaInferenceStack.of(schemaContext);
        final SchemaTreeEffectiveStatement<?> dataSchema;
        try {
            dataSchema = schemaStack.enterSchemaTree(bindingContext.bindingQName);
        } catch (IllegalArgumentException e) {
            throw new ConfigXMLReaderException(
                logName + ": Could not obtain the schema for " + bindingContext.bindingQName, e);
        }

        checkCondition(bindingContext.schemaType.isInstance(dataSchema),
                "%s: Expected schema type %s for %s but actual type is %s", logName,
                bindingContext.schemaType, bindingContext.bindingQName, dataSchema.getClass());

        NormalizedNode dataNode = parsePossibleDefaultAppConfigXMLFile(schemaStack);
        if (dataNode == null) {
            dataNode = fallback.get(schemaStack.toSchemaTreeInference());
        }

        DataObject appConfig = bindingSerializer.fromNormalizedNode(yangPath, dataNode).getValue();

        // This shouldn't happen but need to handle it in case...
        checkNotNull(appConfig, "%s: Could not create instance for app config binding %s", logName,
                bindingContext.appConfigBindingClass);

        return (T) appConfig;
    }

    private static void checkNotNull(final Object reference, final String errorMessageFormat,
            final Object... formatArgs) throws ConfigXMLReaderException {
        checkCondition(reference != null, errorMessageFormat, formatArgs);
    }

    private static void checkCondition(final boolean expression, final String errorMessageFormat,
            final Object... formatArgs) throws ConfigXMLReaderException {
        if (!expression) {
            throw new ConfigXMLReaderException(String.format(errorMessageFormat, formatArgs));
        }
    }

    private NormalizedNode parsePossibleDefaultAppConfigXMLFile(final SchemaInferenceStack schemaStack)
            throws ConfigXMLReaderException {
        String appConfigFileName = defaultAppConfigFileName;
        if (Strings.isNullOrEmpty(appConfigFileName)) {
            String moduleName = schemaStack.currentModule().argument().getLocalName();

            appConfigFileName = moduleName + "_" + bindingContext.bindingQName.getLocalName() + ".xml";
        }

        Optional<URL> optionalURL;
        try {
            optionalURL = inputStreamProvider.getURL(appConfigFileName);
        } catch (final IOException e) {
            String msg = String.format("%s: Could not getURL()", logName);
            LOG.error(msg, e);
            throw new ConfigXMLReaderException(msg, e);
        }
        if (!optionalURL.isPresent()) {
            return null;
        }
        URL url = optionalURL.get();
        try (InputStream is = url.openStream()) {
            Document root = UntrustedXML.newDocumentBuilder().parse(is);
            NormalizedNode dataNode = bindingContext.parseDataElement(root.getDocumentElement(),
                schemaStack.toSchemaTreeInference());

            LOG.debug("{}: Parsed data node: {}", logName, dataNode);

            return dataNode;
        } catch (final IOException | SAXException | XMLStreamException | ParserConfigurationException
                | URISyntaxException e) {
            String msg = String.format("%s: Could not read/parse app config %s", logName, url);
            LOG.error(msg, e);
            throw new ConfigXMLReaderException(msg, e);
        }
    }
}
