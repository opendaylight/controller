/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops.get;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.mdsal.connector.ops.Datastore;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.sal.connect.netconf.util.InstanceIdToNodes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class AbstractGet extends AbstractLastNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractGet.class);

    protected static final String FILTER = "filter";
    protected static final YangInstanceIdentifier ROOT = YangInstanceIdentifier.builder().build();
    protected final CurrentSchemaContext schemaContext;

    public AbstractGet(final String netconfSessionIdForReporting, final CurrentSchemaContext schemaContext) {
        super(netconfSessionIdForReporting);
        this.schemaContext = schemaContext;
    }

    private static final XMLOutputFactory XML_OUTPUT_FACTORY;

    static {
        XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
        XML_OUTPUT_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    protected Node transformNormalizedNode(final Document document, final NormalizedNode<?, ?> data, final YangInstanceIdentifier dataRoot) {

        final DOMResult result = new DOMResult(document.createElement(XmlNetconfConstants.DATA_KEY));

        final XMLStreamWriter xmlWriter = getXmlStreamWriter(result);

        final NormalizedNodeStreamWriter nnStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(xmlWriter,
                schemaContext.getCurrentContext(), getSchemaPath(dataRoot));

        final NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter);

        writeRootElement(xmlWriter, nnWriter, (ContainerNode) data);
        return result.getNode();
    }


    private XMLStreamWriter getXmlStreamWriter(final DOMResult result) {
        try {
            return XML_OUTPUT_FACTORY.createXMLStreamWriter(result);
        } catch (final XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Function<PathArgument, QName> PATH_ARG_TO_QNAME = new Function<YangInstanceIdentifier.PathArgument, QName>() {
        @Override
        public QName apply(final YangInstanceIdentifier.PathArgument input) {
            return input.getNodeType();
        }
    };

    private SchemaPath getSchemaPath(final YangInstanceIdentifier dataRoot) {
        return SchemaPath.create(Iterables.transform(dataRoot.getPathArguments(), PATH_ARG_TO_QNAME), dataRoot.equals(ROOT));
    }

    // TODO this code is located in Restconf already
    private void writeRootElement(final XMLStreamWriter xmlWriter, final NormalizedNodeWriter nnWriter, final ContainerNode data) {
        try {
            if (data.getNodeType().equals(SchemaContext.NAME)) {
                for (final DataContainerChild<? extends PathArgument, ?> child : data.getValue()) {
                    nnWriter.write(child);
                }
            } else {
                nnWriter.write(data);
            }
            nnWriter.flush();
            xmlWriter.flush();
        } catch (XMLStreamException | IOException e) {
            Throwables.propagate(e);
        }
    }

    protected DataSchemaNode getSchemaNodeFromNamespace(final String namespace, final XmlElement element) throws NetconfDocumentedException{
        DataSchemaNode dataSchemaNode = null;
        try {
            //returns module with newest revision since findModuleByNamespace returns a set of modules and we only need the newest one
            final Module module = schemaContext.getCurrentContext().findModuleByNamespaceAndRevision(new URI(namespace), null);
            DataSchemaNode schemaNode = module.getDataChildByName(element.getName());
            if (schemaNode != null) {
                dataSchemaNode = schemaNode;
            } else {
                throw new NetconfDocumentedException("Unable to find node with namespace: " + namespace + "in module: " + module.toString(),
                        ErrorType.application,
                        ErrorTag.unknown_namespace,
                        ErrorSeverity.error);
            }

        } catch (URISyntaxException e) {
            LOG.debug("Unable to create URI for namespace : {}", namespace);
        }

        return dataSchemaNode;
    }

    protected FilterSchema createFilter(XmlElement filterElement) throws NetconfDocumentedException {
        List<XmlElement> filterElements = filterElement.getChildElements();
        if (filterElements.size() != 1) {
            //more than one container, we need to read everything above them and filter that
            return new FilterSchema() {
                @Override
                public YangInstanceIdentifier getReadPointFromSchema(DataSchemaNode schemaNode) {
                    return ROOT;
                }
            };
        }

        return new FilterSchema(filterElement.getOnlyChildElement());

    }

    protected YangInstanceIdentifier getInstanceIdentifierFromFilter(XmlElement filterElement) throws NetconfDocumentedException {
        FilterSchema filter = createFilter(filterElement);
        XmlElement node = filterElement.getChildElements().get(0);
        String nodeNamespace = node.getNamespace();
        DataSchemaNode schemaNode = getSchemaNodeFromNamespace(nodeNamespace, node);
        return filter.getReadPointFromSchema(schemaNode);
    }

    protected Element serializeNodeWithParentStructure(Document document, YangInstanceIdentifier dataRoot, NormalizedNode node) {
        if (!dataRoot.equals(ROOT)) {
            return (Element) transformNormalizedNode(document,
                    InstanceIdToNodes.serialize(schemaContext.getCurrentContext(), dataRoot, node),
                    ROOT);
        }
        return  (Element) transformNormalizedNode(document, node, ROOT);
    }

    protected static final class GetConfigExecution {
        private final Optional<Datastore> datastore;

        public GetConfigExecution(final Optional<Datastore> datastore) {
            this.datastore = datastore;
        }

        public Optional<Datastore> getDatastore() {
            return datastore;
        }

        static GetConfigExecution fromXml(final XmlElement xml, final String operationName) throws NetconfDocumentedException {
            try {
                validateInputRpc(xml, operationName);
            } catch (final NetconfDocumentedException e) {
                throw new NetconfDocumentedException("Incorrect RPC: " + e.getMessage(), e.getErrorType(), e.getErrorTag(), e.getErrorSeverity(), e.getErrorInfo());
            }

            final Optional<Datastore> sourceDatastore;
            try {
                sourceDatastore = parseSource(xml);
            } catch (final NetconfDocumentedException e) {
                throw new NetconfDocumentedException("Get-config source attribute error: " + e.getMessage(), e.getErrorType(), e.getErrorTag(), e.getErrorSeverity(), e.getErrorInfo());
            }

            return new GetConfigExecution(sourceDatastore);
        }

        private static Optional<Datastore> parseSource(final XmlElement xml) throws NetconfDocumentedException {
            final Optional<XmlElement> sourceElement = xml.getOnlyChildElementOptionally(XmlNetconfConstants.SOURCE_KEY,
                    XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

            return  sourceElement.isPresent() ?
                    Optional.of(Datastore.valueOf(sourceElement.get().getOnlyChildElement().getName())) : Optional.<Datastore>absent();
        }

        private static void validateInputRpc(final XmlElement xml, String operationName) throws NetconfDocumentedException{
            xml.checkName(operationName);
            xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        }
    }

}
