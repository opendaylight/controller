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
import java.util.ArrayList;
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
import org.opendaylight.controller.netconf.mdsal.connector.ops.DataTreeChangeTracker.DataTreeChange;
import org.opendaylight.controller.netconf.mdsal.connector.ops.DataTreeChangeTracker.NetconfOperationLeafStrategy;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedNamespaceException;
import org.opendaylight.controller.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EditConfig extends AbstractSingletonNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(EditConfig.class);

    private static final String OPERATION_NAME = "edit-config";
    private static final String CONFIG_KEY = "config";
    private static final String TARGET_KEY = "target";
    private static final String DEFAULT_OPERATION_KEY = "default-operation";
    private final CurrentSchemaContext schemaContext;
    private final TransactionProvider transactionProvider;

    public EditConfig(final String netconfSessionIdForReporting, final CurrentSchemaContext schemaContext, final TransactionProvider transactionProvider) {
        super(netconfSessionIdForReporting);
        this.schemaContext = schemaContext;
        this.transactionProvider = transactionProvider;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        final Datastore targetDatastore = extractTargetParameter(operationElement);
        if (targetDatastore == Datastore.running) {
            throw new NetconfDocumentedException("edit-config on running datastore is not supported",
                    ErrorType.protocol,
                    ErrorTag.operation_not_supported,
                    ErrorSeverity.error);
        }

        final ModifyAction defaultAction = getDefaultOperation(operationElement);

        final XmlElement configElement = getElement(operationElement, CONFIG_KEY);

        for (XmlElement element : configElement.getChildElements()) {
            final String ns = element.getNamespace();
            final DataSchemaNode schemaNode = getSchemaNodeFromNamespace(ns, element).get();

            DataTreeChangeTracker changeTracker = new DataTreeChangeTracker(ModifyAction.MERGE);

            DataTreeChangeTracker.NetconfOperationContainerStrategy containerStrategy =
                    new DataTreeChangeTracker.NetconfOperationContainerStrategy(changeTracker);
            DataTreeChangeTracker.NetconfOperationLeafStrategy leafStrategy = new NetconfOperationLeafStrategy();
            parseIntoNormalizedNode(schemaNode, element, containerStrategy, leafStrategy);

            executeOperations(changeTracker);
        }

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    private void executeOperations(DataTreeChangeTracker changeTracker) {
        ArrayList<DataTreeChange> aa = changeTracker.getDataTreeChanges();
        Collections.reverse(aa);
        final DOMDataReadWriteTransaction rwTx = transactionProvider.getOrCreateTransaction();

        for (DataTreeChange change : aa) {
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath()), change.getChangeRoot());
        }
    }

    private NormalizedNode parseIntoNormalizedNode(final DataSchemaNode schemaNode, XmlElement element,
                                                   DataTreeChangeTracker.NetconfOperationContainerStrategy containerStrategy,
                                                   DataTreeChangeTracker.NetconfOperationLeafStrategy leafStrategy) {
        if (schemaNode instanceof ContainerSchemaNode) {
            return DomToNormalizedNodeParserFactory
                    .getInstance(DomUtils.defaultValueCodecProvider(), schemaContext.getCurrentContext(), containerStrategy, leafStrategy)
                    .getContainerNodeParser()
                    .parse(Collections.singletonList(element.getDomElement()), (ContainerSchemaNode) schemaNode);
        } else if (schemaNode instanceof ListSchemaNode) {
            return DomToNormalizedNodeParserFactory
                    .getInstance(DomUtils.defaultValueCodecProvider(), schemaContext.getCurrentContext(), containerStrategy, leafStrategy)
                    .getMapNodeParser()
                    .parse(Collections.singletonList(element.getDomElement()), (ListSchemaNode) schemaNode);
        } else {
            //this should never happen since edit-config on any other node type should not be possible nor makes sense
            LOG.debug("DataNode from module is not ContainerSchemaNode nor ListSchemaNode, aborting..");
        }
        throw new UnsupportedOperationException("implement exception if parse fails");
    }

    private NormalizedNode readStoredNode(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier path) throws NetconfDocumentedException{
        final DOMDataReadWriteTransaction rwTx = transactionProvider.getOrCreateTransaction();
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture = rwTx.read(logicalDatastoreType, path);
        try {
            if (readFuture.checkedGet().isPresent()) {
                final NormalizedNode node = readFuture.checkedGet().get();
                return node;
            } else {
                LOG.debug("Unable to read node : {} from {} datastore", path, logicalDatastoreType);
            }
        } catch (final ReadFailedException e) {
            //only log this since DataOperations.modify will handle throwing an exception or writing the node.
            LOG.debug("Unable to read stored data: {}", path, e);
        }

        //we can return null here since DataOperations.modify handles null as input
        return null;
    }

    private Optional<DataSchemaNode> getSchemaNodeFromNamespace(final String namespace, final XmlElement element) throws NetconfDocumentedException{
        Optional<DataSchemaNode> dataSchemaNode = Optional.absent();
        try {
            //returns module with newest revision since findModuleByNamespace returns a set of modules and we only need the newest one
            final Module module = schemaContext.getCurrentContext().findModuleByNamespaceAndRevision(new URI(namespace), null);
            DataSchemaNode schemaNode = module.getDataChildByName(element.getName());
            if (schemaNode != null) {
                dataSchemaNode = Optional.of(module.getDataChildByName(element.getName()));
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

    private Datastore extractTargetParameter(final XmlElement operationElement) throws NetconfDocumentedException {
        final XmlElement targetChildNode;
        try {
            final XmlElement targetElement = operationElement.getOnlyChildElementWithSameNamespace(TARGET_KEY);
            targetChildNode = targetElement.getOnlyChildElementWithSameNamespace();
        } catch (final MissingNameSpaceException | UnexpectedNamespaceException e) {
            LOG.trace("Can't get only child element with same namespace", e);
            throw NetconfDocumentedException.wrap(e);
        }

        return Datastore.valueOf(targetChildNode.getName());
    }

    private ModifyAction getDefaultOperation(final XmlElement operationElement) throws NetconfDocumentedException{
        try {
            return ModifyAction.fromXmlValue(getElement(operationElement, DEFAULT_OPERATION_KEY).getTextContent());
        } catch (NetconfDocumentedException e) {
            if (e.getErrorType() == ErrorType.protocol
                    && e.getErrorSeverity() == ErrorSeverity.error
                    && e.getErrorTag() == ErrorTag.missing_element) {
                return ModifyAction.MERGE;
            }
            else {
                throw e;
            }
        }
    }

    private XmlElement getElement(final XmlElement operationElement, String elementName) throws NetconfDocumentedException {
        final Optional<XmlElement> childNode = operationElement.getOnlyChildElementOptionally(elementName);
        if (!childNode.isPresent()) {
            throw new NetconfDocumentedException(elementName + " element is missing",
                    ErrorType.protocol,
                    ErrorTag.missing_element,
                    ErrorSeverity.error);
        }

        return childNode.get();
    }

    @Override
    protected String getOperationName() {
        return OPERATION_NAME;
    }
}
