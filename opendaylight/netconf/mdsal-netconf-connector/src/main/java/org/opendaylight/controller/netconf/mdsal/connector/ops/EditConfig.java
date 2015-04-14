/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import com.google.common.base.Optional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
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
import org.opendaylight.controller.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
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
import org.w3c.dom.NodeList;

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

            final DataTreeChangeTracker changeTracker = new DataTreeChangeTracker(defaultAction);

            final DataTreeChangeTracker.NetconfOperationContainerStrategy containerStrategy =
                    new DataTreeChangeTracker.NetconfOperationContainerStrategy(changeTracker);
            final DataTreeChangeTracker.NetconfOperationLeafStrategy leafStrategy = new NetconfOperationLeafStrategy(changeTracker);
            parseIntoNormalizedNode(schemaNode, element, containerStrategy, leafStrategy);

            executeOperations(changeTracker);
        }

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    private void executeOperations(final DataTreeChangeTracker changeTracker) throws NetconfDocumentedException {
//        List<DataTreeChange> aa = Lists.reverse(changeTracker.getDataTreeChanges());
        final DOMDataReadWriteTransaction rwTx = transactionProvider.getOrCreateTransaction();
        final List<DataTreeChange> aa = changeTracker.getDataTreeChanges();
        final ListIterator<DataTreeChange> iterator = aa.listIterator(aa.size());

        while (iterator.hasPrevious()) {
            final DataTreeChange dtc = iterator.previous();
            executeChange(rwTx, dtc);
        }
    }

    private void executeChange(final DOMDataReadWriteTransaction rwtx, final DataTreeChange change) throws NetconfDocumentedException {
        switch (change.getAction()) {
        case NONE:
            return;
        case MERGE:
            rwtx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath()), change.getChangeRoot());
            break;
        case CREATE:
            try {
                final Optional<NormalizedNode<?, ?>> readResult = rwtx.read(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath())).checkedGet();
                if (readResult.isPresent()) {
                    throw new NetconfDocumentedException("Data already exists, cannot execute CREATE operation", ErrorType.protocol, ErrorTag.data_exists, ErrorSeverity.error);
                }
                rwtx.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath()), change.getChangeRoot());
            } catch (ReadFailedException e) {
                LOG.warn("Read from datastore failed when trying to read data for create operation", change, e);
            }
            break;
        case REPLACE:
            rwtx.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath()), change.getChangeRoot());
            break;
        case DELETE:
            try {
                final Optional<NormalizedNode<?, ?>> readResult = rwtx.read(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath())).checkedGet();
                if (!readResult.isPresent()) {
                    throw new NetconfDocumentedException("Data is missing, cannot execute DELETE operation", ErrorType.protocol, ErrorTag.data_missing, ErrorSeverity.error);
                }
                rwtx.delete(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath()));
            } catch (ReadFailedException e) {
                LOG.warn("Read from datastore failed when trying to read data for delete operation", change, e);
            }
            break;
        case REMOVE:
            rwtx.delete(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(change.getPath()));
            break;
        default:
            LOG.warn("Unknown/not implemented operation, not executing");
        }
    }

    private NormalizedNode parseIntoNormalizedNode(final DataSchemaNode schemaNode, final XmlElement element,
                                                   final DataTreeChangeTracker.NetconfOperationContainerStrategy containerStrategy,
                                                   final DataTreeChangeTracker.NetconfOperationLeafStrategy leafStrategy) {
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
        final NodeList elementsByTagName = operationElement.getDomElement().getElementsByTagName(TARGET_KEY);
        // Direct lookup instead of using XmlElement class due to performance
        if (elementsByTagName.getLength() == 0) {
            throw new NetconfDocumentedException("Missing target element", ErrorType.rpc, ErrorTag.missing_attribute, ErrorSeverity.error);
        } else if (elementsByTagName.getLength() > 1) {
            throw new NetconfDocumentedException("Multiple target elements", ErrorType.rpc, ErrorTag.unknown_attribute, ErrorSeverity.error);
        } else {
            final XmlElement targetChildNode = XmlElement.fromDomElement((Element) elementsByTagName.item(0)).getOnlyChildElement();
            return Datastore.valueOf(targetChildNode.getName());
        }
    }

    private ModifyAction getDefaultOperation(final XmlElement operationElement) throws NetconfDocumentedException {
        final NodeList elementsByTagName = operationElement.getDomElement().getElementsByTagName(DEFAULT_OPERATION_KEY);
        if(elementsByTagName.getLength() == 0) {
            return ModifyAction.MERGE;
        } else if(elementsByTagName.getLength() > 1) {
            throw new NetconfDocumentedException("Multiple " + DEFAULT_OPERATION_KEY + " elements",
                    ErrorType.rpc, ErrorTag.unknown_attribute, ErrorSeverity.error);
        } else {
            return ModifyAction.fromXmlValue(elementsByTagName.item(0).getTextContent());
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
