/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.NoTransactionFoundException;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedElementException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedNamespaceException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Commit extends AbstractConfigNetconfOperation {

    private static final Logger logger = LoggerFactory.getLogger(Commit.class);

    private final TransactionProvider transactionProvider;

    public Commit(TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.transactionProvider = transactionProvider;
    }

    private static void checkXml(XmlElement xml) throws UnexpectedElementException, UnexpectedNamespaceException, MissingNameSpaceException {
        xml.checkName(XmlNetconfConstants.COMMIT);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.COMMIT;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {

        try {
            checkXml(xml);
        } catch (final MissingNameSpaceException | UnexpectedElementException | UnexpectedNamespaceException e){
            throw NetconfDocumentedException.wrap(e);
        }
        CommitStatus status;
        try {
            status = this.transactionProvider.commitTransaction();
            logger.trace("Datastore {} committed successfully: {}", Datastore.candidate, status);
        } catch (final NoTransactionFoundException | ConflictingVersionException | ValidationException e) {
            throw NetconfDocumentedException.wrap(e);
        }
        return document.createElement(XmlNetconfConstants.OK);
    }

}
