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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.mdsal.connector.ops.Datastore;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class AbstractGet extends AbstractLastNetconfOperation {

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
            for (final DataContainerChild<? extends PathArgument, ?> child : data.getValue()) {
                nnWriter.write(child);
            }
            nnWriter.flush();
            xmlWriter.flush();
        } catch (XMLStreamException | IOException e) {
            Throwables.propagate(e);
        }
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

            // Add filter

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
