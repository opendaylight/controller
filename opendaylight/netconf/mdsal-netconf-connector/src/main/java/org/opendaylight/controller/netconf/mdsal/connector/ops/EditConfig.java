/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.mdsal.connector.TransactionProvider;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.data.operations.DataModificationException;
import org.opendaylight.yangtools.yang.data.operations.DataModificationException.DataExistsException;
import org.opendaylight.yangtools.yang.data.operations.DataModificationException.DataMissingException;
import org.opendaylight.yangtools.yang.data.operations.DataOperations;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EditConfig extends AbstractLastNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(EditConfig.class);

    private static final String OPERATION_NAME = "edit-config";
    private static final String CONFIG_KEY = "config";

    private final CurrentSchemaContext schemaContext;
    private final TransactionProvider transactionProvider;

    public EditConfig(final String netconfSessionIdForReporting, final CurrentSchemaContext schemaContext, final TransactionProvider transactionProvider) {
        super(netconfSessionIdForReporting);
        this.schemaContext = schemaContext;
        this.transactionProvider = transactionProvider;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        final XmlElement configElement = getConfigElement(operationElement);

        for (XmlElement element : configElement.getChildElements()) {
            final String ns = element.getNamespace();
            final DataSchemaNode schemaNode = getSchemaNodeFromNamespace(ns, element).get();
            YangInstanceIdentifier ident = YangInstanceIdentifier.of(schemaNode.getQName());

            final NormalizedNode storedNode = readStoredNode(LogicalDatastoreType.CONFIGURATION, ident);
            try {
                final Optional<NormalizedNode<?,?>> newNode = modifyNode(schemaNode, element, storedNode);
                final DOMDataReadWriteTransaction rwTx = transactionProvider.getOrCreateTransaction();
                if (newNode.isPresent()) {
                    rwTx.put(LogicalDatastoreType.CONFIGURATION, ident, newNode.get());
                } else {
                    rwTx.delete(LogicalDatastoreType.CONFIGURATION, ident);
                }
            } catch (final DataModificationException e) {
                if (e instanceof DataExistsException) {
                    throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.protocol, ErrorTag.data_exists, ErrorSeverity.error);
                } else if (e instanceof DataMissingException) {
                    throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.protocol, ErrorTag.data_missing, ErrorSeverity.error);
                } else {
                    //should never happen, since in edit-config only the 2 previous cases can happen
                    throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.protocol, ErrorTag.operation_failed, ErrorSeverity.error);
                }
            }
        }

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    private NormalizedNode readStoredNode(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier path) throws NetconfDocumentedException{
        final  DOMDataReadWriteTransaction rwTx = transactionProvider.getOrCreateTransaction();
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture = rwTx.read(logicalDatastoreType, path);
        try {
            if (readFuture.checkedGet().isPresent()) {
                final NormalizedNode node = readFuture.checkedGet().get();
                return node;
            } else {
                LOG.warn("Unable to read node : {} from {} datastore", path, logicalDatastoreType);
            }
        } catch (final ReadFailedException e) {
            //only log this since DataOperations.modify will handle throwing an exception or writing the node.
            LOG.warn("Unable to read stored data: {}", path, e);
        }

        //we can return null here since DataOperations.modify handles null as input
        return null;
    }

    private Optional<DataSchemaNode> getSchemaNodeFromNamespace(final String namespace, final XmlElement element){
        Optional<DataSchemaNode> dataSchemaNode = Optional.absent();
        try {
            //returns module with newest revision since findModuleByNamespace returns a set of modules and we only need the newest one
            final Module module = schemaContext.getCurrentContext().findModuleByNamespaceAndRevision(new URI(namespace), null);
            dataSchemaNode = Optional.of(module.getDataChildByName(element.getName()));
        } catch (URISyntaxException e) {
            LOG.debug("Unable to create URI for namespace : {}", namespace);
        }

        return dataSchemaNode;
    }

    private Optional<NormalizedNode<?, ?>> modifyNode(final DataSchemaNode schemaNode, final XmlElement element, final NormalizedNode storedNode) throws DataModificationException{
        if (schemaNode instanceof ContainerSchemaNode) {
            final ContainerNode modifiedNode =
                    DomToNormalizedNodeParserFactory
                            .getInstance(DomUtils.defaultValueCodecProvider())
                            .getContainerNodeParser()
                            .parse(Collections.singletonList(element.getDomElement()), (ContainerSchemaNode) schemaNode);

            final Optional<ContainerNode> oNode = DataOperations.modify((ContainerSchemaNode) schemaNode, (ContainerNode) storedNode, modifiedNode);
            if (!oNode.isPresent()) {
                return Optional.absent();
            }

            final NormalizedNode<?,?> node = oNode.get();
            return Optional.<NormalizedNode<?,?>>of(node);
        } else if (schemaNode instanceof ListSchemaNode) {
            final MapNode modifiedNode =
                DomToNormalizedNodeParserFactory
                        .getInstance(DomUtils.defaultValueCodecProvider())
                        .getMapNodeParser()
                        .parse(Collections.singletonList(element.getDomElement()), (ListSchemaNode) schemaNode);

            final Optional<MapNode> oNode = DataOperations.modify((ListSchemaNode) schemaNode, (MapNode) storedNode, modifiedNode);
            if (!oNode.isPresent()) {
                return Optional.absent();
            }

            final NormalizedNode<?, ?> node = oNode.get();
            return Optional.<NormalizedNode<?,?>>of(node);
        } else {
            //this should never happen since edit-config on any other node type should not be possible nor makes sense
            LOG.debug("DataNode from module is not ContainerSchemaNode nor ListSchemaNode, aborting..");
            return Optional.absent();
        }

    }

    private XmlElement getConfigElement(final XmlElement operationElement) throws NetconfDocumentedException{
        final Optional<XmlElement> configChildNode = operationElement.getOnlyChildElementOptionally(CONFIG_KEY);
        if (!configChildNode.isPresent()) {
            throw new NetconfDocumentedException("Can't get child element with name: " + CONFIG_KEY,
                    ErrorType.application,
                    ErrorTag.unknown_element,
                    ErrorSeverity.error);
        }

        return configChildNode.get();
    }

    @Override
    protected String getOperationName() {
        return OPERATION_NAME;
    }
}
