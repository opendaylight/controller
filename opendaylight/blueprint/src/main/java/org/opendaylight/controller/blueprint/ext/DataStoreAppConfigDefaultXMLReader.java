/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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
    private final SchemaService schemaService;
    private final BindingNormalizedNodeSerializer bindingSerializer;
    private final BindingContext bindingContext;
    private final ConfigURLProvider inputStreamProvider;

    @FunctionalInterface
    public interface FallbackConfigProvider {
        NormalizedNode<?,?> get(SchemaContext schemaContext, DataSchemaNode dataSchema);
    }

    @FunctionalInterface
    public interface ConfigURLProvider {
        Optional<URL> getURL(String appConfigFileName) throws IOException;
    }

    public DataStoreAppConfigDefaultXMLReader(
            final String logName,
            final String defaultAppConfigFileName,
            final SchemaService schemaService,
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
            final SchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingSerializer,
            final Class<T> klass) {
        this(testClass.getName(), defaultAppConfigFileName, schemaService, bindingSerializer,
            BindingContext.create(testClass.getName(), klass, null),
            appConfigFileName -> Optional.of(getURL(testClass, defaultAppConfigFileName)));
    }

    private static URL getURL(final Class<?> testClass, final String defaultAppConfigFileName) {
        return Resources.getResource(testClass, defaultAppConfigFileName);
    }

    public T createDefaultInstance() throws ConfigXMLReaderException {
        return createDefaultInstance((schemaContext, dataSchema) -> {
            throw new IllegalArgumentException("Failed to read XML "
                    + "(not creating model from defaults as runtime would, for better clarity in tests)");
        });
    }

    @SuppressWarnings("unchecked")
    public T createDefaultInstance(final FallbackConfigProvider fallback) throws ConfigXMLReaderException {
        YangInstanceIdentifier yangPath = bindingSerializer.toYangInstanceIdentifier(bindingContext.appConfigPath);

        LOG.debug("{}: Creating app config instance from path {}, Qname: {}", logName, yangPath,
                bindingContext.bindingQName);

        if (schemaService == null) {
            throw new ConfigXMLReaderException(
                    String.format("%s: Could not obtain the SchemaService OSGi service", logName));
        }

        SchemaContext schemaContext = schemaService.getGlobalContext();

        Module module = schemaContext.findModuleByNamespaceAndRevision(bindingContext.bindingQName.getNamespace(),
                bindingContext.bindingQName.getRevision());
        if (module == null) {
            throw new ConfigXMLReaderException(
                    String.format("%s: Could not obtain the module schema for namespace %s, revision %s",
                    logName, bindingContext.bindingQName.getNamespace(), bindingContext.bindingQName.getRevision()));
        }

        DataSchemaNode dataSchema = module.getDataChildByName(bindingContext.bindingQName);
        if (dataSchema == null) {
            throw new ConfigXMLReaderException(
                    String.format("%s: Could not obtain the schema for %s", logName,
                    bindingContext.bindingQName));
        }

        if (!bindingContext.schemaType.isAssignableFrom(dataSchema.getClass())) {
            throw new ConfigXMLReaderException(
                    String.format("%s: Expected schema type %s for %s but actual type is %s", logName,
                    bindingContext.schemaType, bindingContext.bindingQName, dataSchema.getClass()));
        }

        NormalizedNode<?, ?> dataNode = parsePossibleDefaultAppConfigXMLFile(schemaContext, dataSchema);
        if (dataNode == null) {
            dataNode = fallback.get(schemaService.getGlobalContext(), dataSchema);
        }

        DataObject appConfig = bindingSerializer.fromNormalizedNode(yangPath, dataNode).getValue();
        if (appConfig == null) {
            // This shouldn't happen but need to handle it in case...
            throw new ConfigXMLReaderException(
                    String.format("%s: Could not create instance for app config binding %s",
                    logName, bindingContext.appConfigBindingClass));
        } else {
            return (T) appConfig;
        }
    }

    private NormalizedNode<?, ?> parsePossibleDefaultAppConfigXMLFile(final SchemaContext schemaContext,
            final DataSchemaNode dataSchema) throws ConfigXMLReaderException {

        String appConfigFileName = defaultAppConfigFileName;
        if (Strings.isNullOrEmpty(appConfigFileName)) {
            String moduleName = findYangModuleName(bindingContext.bindingQName, schemaContext);
            appConfigFileName = moduleName + "_" + bindingContext.bindingQName.getLocalName() + ".xml";
        }

        final DomToNormalizedNodeParserFactory parserFactory = DomToNormalizedNodeParserFactory.getInstance(
                XmlUtils.DEFAULT_XML_CODEC_PROVIDER, schemaContext);

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
            NormalizedNode<?, ?> dataNode = bindingContext.parseDataElement(root.getDocumentElement(), dataSchema,
                    parserFactory);

            LOG.debug("{}: Parsed data node: {}", logName, dataNode);

            return dataNode;
        } catch (SAXException | IOException e) {
            String msg = String.format("%s: Could not read/parse app config %s", logName, url);
            LOG.error(msg, e);
            throw new ConfigXMLReaderException(msg, e);
        }
    }

    private String findYangModuleName(final QName qname, final SchemaContext schemaContext)
            throws ConfigXMLReaderException {
        for (Module m : schemaContext.getModules()) {
            if (qname.getModule().equals(m.getQNameModule())) {
                return m.getName();
            }
        }
        throw new ConfigXMLReaderException(
                String.format("%s: Could not find yang module for QName %s", logName, qname));
    }

}
