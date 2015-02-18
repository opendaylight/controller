/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.util.Set;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.impl.CommitNotifier;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationRouter;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultCommit extends AbstractNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCommit.class);

    private static final String NOTIFY_ATTR = "notify";

    private final CommitNotifier notificationProducer;
    private final NetconfMonitoringService cap;
    private final NetconfOperationRouter operationRouter;

    public DefaultCommit(CommitNotifier notifier, NetconfMonitoringService cap,
                         String netconfSessionIdForReporting, NetconfOperationRouter netconfOperationRouter) {
        super(netconfSessionIdForReporting);
        this.notificationProducer = notifier;
        this.cap = cap;
        this.operationRouter = netconfOperationRouter;
        this.getConfigMessage = loadGetConfigMessage();
    }

    private final Document getConfigMessage;
    public static final String GET_CONFIG_CANDIDATE_XML_LOCATION = "/getConfig_candidate.xml";

    private static Document loadGetConfigMessage() {
        try (InputStream asStream = DefaultCommit.class.getResourceAsStream(GET_CONFIG_CANDIDATE_XML_LOCATION)) {
            return XmlUtil.readXmlToDocument(asStream);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load getConfig message for notifications from "
                    + GET_CONFIG_CANDIDATE_XML_LOCATION);
        }
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.COMMIT;
    }

    @Override
    public Document handle(Document requestMessage, NetconfOperationChainedExecution subsequentOperation) throws NetconfDocumentedException {
        Preconditions.checkArgument(!subsequentOperation.isExecutionTermination(),
                "Subsequent netconf operation expected by %s", this);

        if (isCommitWithoutNotification(requestMessage)) {
            LOG.debug("Skipping commit notification");
        } else {
            // Send commit notification if commit was not issued by persister
            removePersisterAttributes(requestMessage);
            Element cfgSnapshot = getConfigSnapshot(operationRouter);
            LOG.debug("Config snapshot retrieved successfully {}", cfgSnapshot);
            notificationProducer.sendCommitNotification("ok", cfgSnapshot, transformCapabilities(cap.getCapabilities()));
        }

        return subsequentOperation.execute(requestMessage);
    }

    // FIXME move somewhere to util since this is required also by negotiatiorFactory
    public static Set<String> transformCapabilities(final Capabilities capabilities) {
        return Sets.newHashSet(Collections2.transform(capabilities.getCapability(), new Function<Uri, String>() {
            @Override
            public String apply(final Uri uri) {
                return uri.getValue();
            }
        }));
    }

    @Override
    protected Element handle(Document document, XmlElement message, NetconfOperationChainedExecution subsequentOperation) throws NetconfDocumentedException {
        throw new UnsupportedOperationException("Never gets called");
    }

    @Override
    protected HandlingPriority getHandlingPriority() {
        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.increasePriority(1);
    }

    private void removePersisterAttributes(Document message) {
        message.getDocumentElement().removeAttribute(NOTIFY_ATTR);
    }

    private boolean isCommitWithoutNotification(Document message) {
        XmlElement xmlElement = null;
        try {
            xmlElement = XmlElement.fromDomElementWithExpected(message.getDocumentElement(),
                    XmlNetconfConstants.RPC_KEY, XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        } catch (NetconfDocumentedException e) {
            LOG.trace("Commit operation is not valid due to ",e);
            return false;
        }

        String attr = xmlElement.getAttribute(NOTIFY_ATTR);

        if (attr == null || attr.equals("")){
            return false;
        } else if (attr.equals(Boolean.toString(false))) {
            LOG.debug("Commit operation received with notify=false attribute {}", message);
            return true;
        } else {
            return false;
        }
    }

    private Element getConfigSnapshot(NetconfOperationRouter opRouter) throws NetconfDocumentedException {
        final Document responseDocument = opRouter.onNetconfMessage(
                getConfigMessage, null);

        XmlElement dataElement;
        XmlElement xmlElement = XmlElement.fromDomElementWithExpected(responseDocument.getDocumentElement(),
                XmlNetconfConstants.RPC_REPLY_KEY, XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        dataElement = xmlElement.getOnlyChildElement(XmlNetconfConstants.DATA_KEY);
        return dataElement.getDomElement();
    }

}
