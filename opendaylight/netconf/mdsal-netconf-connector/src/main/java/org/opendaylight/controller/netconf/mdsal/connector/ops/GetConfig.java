/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import java.io.IOException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class GetConfig extends AbstractLastNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(GetConfig.class);

    private static final YangInstanceIdentifier ROOT = YangInstanceIdentifier.builder().build();
    private static final String GET_CONFIG = "get-config";

    private final CurrentSchemaContext schemaContext;
    private final DOMDataBroker dataBroker;

    public GetConfig(final String netconfSessionIdForReporting, final CurrentSchemaContext schemaContext, final DOMDataBroker dataBroker) {
        super(netconfSessionIdForReporting);
        this.schemaContext = schemaContext;
        this.dataBroker = dataBroker;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        try {
            final GetConfigExecution getConfigExecution = GetConfigExecution.fromXml(operationElement);

            // TODO how to read candidate
            Preconditions.checkArgument(getConfigExecution.getDatastore() == Datastore.running);
        } catch (final NetconfDocumentedException e) {
            LOG.warn("Get-config request processing failed on session: {}", getNetconfSessionIdForReporting(), e);
            throw e;
        }

        final YangInstanceIdentifier dataRoot = ROOT;
        try (DOMDataReadOnlyTransaction domDataReadOnlyTransaction = dataBroker.newReadOnlyTransaction()) {
            final Optional<NormalizedNode<?, ?>> normalizedNodeOptional =
                    domDataReadOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, dataRoot).checkedGet();

            final Element dataElement = XmlUtil.createElement(document, XmlNetconfConstants.DATA_KEY, Optional.<String>absent());
            if(normalizedNodeOptional.isPresent()) {
                dataElement.appendChild(transformNormalizedNode(normalizedNodeOptional.get(), dataRoot));
            }
            return dataElement;
        } catch (final ReadFailedException e) {
            LOG.warn("Unable to read data: {}", dataRoot, e);
            throw new IllegalStateException("Unable to read data " + dataRoot, e);
        }
    }

    private static final XMLOutputFactory XML_OUTPUT_FACTORY;

    static {
        XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
        XML_OUTPUT_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    private Node transformNormalizedNode(final NormalizedNode<?, ?> data, final YangInstanceIdentifier dataRoot) {
//        boolean isDataRoot = true;

        final DOMResult result = new DOMResult();
        final XMLStreamWriter xmlWriter = getXmlStreamWriter(result);

        final NormalizedNodeStreamWriter nnStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(xmlWriter,
                schemaContext.getCurrentContext(), getSchemaPath(dataRoot));

        final NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter);

//        if (isDataRoot) {
        writeRootElement(xmlWriter, nnWriter, (ContainerNode) data);
//        } else {
//            if (data instanceof MapEntryNode) {
//                // Restconf allows returning one list item. We need to wrap it
//                // in map node in order to serialize it properly
//                data = ImmutableNodes.mapNodeBuilder(data.getNodeType()).addChild((MapEntryNode) data).build();
//            }
//            nnWriter.write(data);
//            nnWriter.flush();
//        }
        return result.getNode();
    }

    private XMLStreamWriter getXmlStreamWriter(final DOMResult result) {
        try {
            return XML_OUTPUT_FACTORY.createXMLStreamWriter(result);
        } catch (final XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Function<YangInstanceIdentifier.PathArgument, QName> PATH_ARG_TO_QNAME = new Function<YangInstanceIdentifier.PathArgument, QName>() {
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
            final QName name = SchemaContext.NAME;
            xmlWriter.writeStartElement(name.getNamespace().toString(), name.getLocalName());
            for (final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child : data.getValue()) {
                nnWriter.write(child);
            }
            nnWriter.flush();
            xmlWriter.writeEndElement();
            xmlWriter.flush();
        } catch (XMLStreamException | IOException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    protected String getOperationName() {
        return GET_CONFIG;
    }

    private static final class GetConfigExecution {
        private final Datastore datastore;

        public GetConfigExecution(final Datastore datastore) {
            this.datastore = datastore;
        }

        public Datastore getDatastore() {
            return datastore;
        }

        static GetConfigExecution fromXml(final XmlElement xml) throws NetconfDocumentedException {
            try {
                validateInputRpc(xml);
            } catch (final NetconfDocumentedException e) {
                throw new NetconfDocumentedException("Incorrect RPC: " + e.getMessage(), e.getErrorType(), e.getErrorTag(), e.getErrorSeverity(), e.getErrorInfo());
            }

            final Datastore sourceDatastore;
            try {
                sourceDatastore = parseSource(xml);
            } catch (final NetconfDocumentedException e) {
                throw new NetconfDocumentedException("Get-config source attribute error: " + e.getMessage(), e.getErrorType(), e.getErrorTag(), e.getErrorSeverity(), e.getErrorInfo());
            }

            // Add filter

            return new GetConfigExecution(sourceDatastore);
        }

        private static Datastore parseSource(final XmlElement xml) throws NetconfDocumentedException {
            final Datastore sourceDatastore;
            final XmlElement sourceElement = xml.getOnlyChildElement(XmlNetconfConstants.SOURCE_KEY,
                    XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

            final String sourceParsed = sourceElement.getOnlyChildElement().getName();
            sourceDatastore = Datastore.valueOf(sourceParsed);
            return sourceDatastore;
        }

        private static void validateInputRpc(final XmlElement xml) throws NetconfDocumentedException{
            xml.checkName(GET_CONFIG);
            xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        }
    }

    private static enum Datastore {
        candidate, running
    }
}
