/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops.get;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
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
import org.opendaylight.controller.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class AbstractGet extends AbstractSingletonNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractGet.class);

    protected static final String FILTER = "filter";
    static final YangInstanceIdentifier ROOT = YangInstanceIdentifier.builder().build();
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

        final NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter, true);

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

    private DataSchemaNode getSchemaNodeFromNamespace(final XmlElement element) throws NetconfDocumentedException {

        try {
            final Module module = schemaContext.getCurrentContext().findModuleByNamespaceAndRevision(new URI(element.getNamespace()), null);
            DataSchemaNode dataSchemaNode = module.getDataChildByName(element.getName());
            if (dataSchemaNode != null) {
                return dataSchemaNode;
            }
        } catch (URISyntaxException e) {
            LOG.debug("Error during parsing of element namespace, this should not happen since namespace of an xml " +
                    "element is valid and if the xml was parsed then the URI should be as well");
            throw new IllegalArgumentException("Unable to parse element namespace, this should not happen since " +
                    "namespace of an xml element is valid and if the xml was parsed then the URI should be as well");
        }
        throw new NetconfDocumentedException("Unable to find node with namespace: " + element.getNamespace() + "in schema context: " + schemaContext.getCurrentContext().toString(),
                ErrorType.application,
                ErrorTag.unknown_namespace,
                ErrorSeverity.error);
    }

    protected Element serializeNodeWithParentStructure(Document document, YangInstanceIdentifier dataRoot, NormalizedNode node) {
        if (!dataRoot.equals(ROOT)) {
            return (Element) transformNormalizedNode(document,
                    ImmutableNodes.fromInstanceId(schemaContext.getCurrentContext(), dataRoot, node),
                    ROOT);
        }
        return  (Element) transformNormalizedNode(document, node, ROOT);
    }

    /**
     *
     * @param operationElement operation element
     * @return if Filter is present and not empty returns Optional of the InstanceIdentifier to the read location in datastore.
     *          empty filter returns Optional.absent() which should equal an empty <data/> container in the response.
     *         if filter is not present we want to read the entire datastore - return ROOT.
     * @throws NetconfDocumentedException
     */
    protected Optional<YangInstanceIdentifier> getDataRootFromFilter(XmlElement operationElement) throws NetconfDocumentedException {
        Optional<XmlElement> filterElement = operationElement.getOnlyChildElementOptionally(FILTER);
        if (filterElement.isPresent()) {
            if (filterElement.get().getChildElements().size() == 0) {
                return Optional.absent();
            }
            return Optional.of(getInstanceIdentifierFromFilter(filterElement.get()));
        } else {
            return Optional.of(ROOT);
        }
    }

    @VisibleForTesting
    protected YangInstanceIdentifier getInstanceIdentifierFromFilter(XmlElement filterElement) throws NetconfDocumentedException {

        if (filterElement.getChildElements().size() != 1) {
            throw new NetconfDocumentedException("Multiple filter roots not supported yet",
                    ErrorType.application, ErrorTag.operation_not_supported, ErrorSeverity.error);
        }

        XmlElement element = filterElement.getOnlyChildElement();
        DataSchemaNode schemaNode = getSchemaNodeFromNamespace(element);

        return getReadPointFromNode(YangInstanceIdentifier.builder().build(), filterToNormalizedNode(element, schemaNode));
    }

    private YangInstanceIdentifier getReadPointFromNode(final YangInstanceIdentifier pathArg, final NormalizedNode nNode) {
        final YangInstanceIdentifier path = pathArg.node(nNode.getIdentifier());
        if (nNode instanceof DataContainerNode) {
            DataContainerNode node = (DataContainerNode) nNode;
            if (node.getValue().size() == 1) {
                return getReadPointFromNode(path, (NormalizedNode) Lists.newArrayList(node.getValue()).get(0));
            }
        }
        return path;
    }

    private NormalizedNode filterToNormalizedNode(XmlElement element, DataSchemaNode schemaNode) throws NetconfDocumentedException {
        DomToNormalizedNodeParserFactory parserFactory = DomToNormalizedNodeParserFactory
                .getInstance(DomUtils.defaultValueCodecProvider(), schemaContext.getCurrentContext());

        final NormalizedNode parsedNode;

        if (schemaNode instanceof ContainerSchemaNode) {
            parsedNode = parserFactory.getContainerNodeParser().parse(Collections.singletonList(element.getDomElement()), (ContainerSchemaNode) schemaNode);
        } else if (schemaNode instanceof ListSchemaNode) {
            parsedNode = parserFactory.getMapNodeParser().parse(Collections.singletonList(element.getDomElement()), (ListSchemaNode) schemaNode);
        } else {
            throw new NetconfDocumentedException("Schema node of the top level element is not an instance of container or list",
                    ErrorType.application, ErrorTag.unknown_element, ErrorSeverity.error);
        }
        return parsedNode;
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
