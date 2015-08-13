/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.runtimerpc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.RpcFacade;
import org.opendaylight.controller.config.facade.xml.rpc.InstanceRuntimeRpc;
import org.opendaylight.controller.config.facade.xml.rpc.ModuleRpcs;
import org.opendaylight.controller.config.facade.xml.rpc.Rpcs;
import org.opendaylight.controller.config.facade.xml.rpc.RuntimeRpcElementResolved;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RuntimeRpc extends AbstractConfigNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeRpc.class);

    public RuntimeRpc(final ConfigSubsystemFacade configSubsystemFacade, final String netconfSessionIdForReporting) {
        super(configSubsystemFacade, netconfSessionIdForReporting);
    }


    @Override
    public HandlingPriority canHandle(Document message) throws DocumentedException {
        XmlElement requestElement = null;
        requestElement = getRequestElementWithCheck(message);

        XmlElement operationElement = requestElement.getOnlyChildElement();
        final String netconfOperationName = operationElement.getName();
        final String netconfOperationNamespace;
        try {
            netconfOperationNamespace = operationElement.getNamespace();
        } catch (DocumentedException e) {
            LOG.debug("Cannot retrieve netconf operation namespace from message due to ", e);
            return HandlingPriority.CANNOT_HANDLE;
        }

        final Optional<XmlElement> contextInstanceElement = operationElement
                .getOnlyChildElementOptionally(RpcFacade.CONTEXT_INSTANCE);

        if (!contextInstanceElement.isPresent()){
            return HandlingPriority.CANNOT_HANDLE;
        }

        final RuntimeRpcElementResolved id = RuntimeRpcElementResolved.fromXpath(contextInstanceElement.get()
                .getTextContent(), netconfOperationName, netconfOperationNamespace);

        // TODO reuse rpcs instance in fromXml method
        final Rpcs rpcs = getConfigSubsystemFacade().getRpcFacade().mapRpcs();

        try {
            final ModuleRpcs rpcMapping = rpcs.getRpcMapping(id);
            final InstanceRuntimeRpc instanceRuntimeRpc = rpcMapping.getRpc(id.getRuntimeBeanName(),
                    netconfOperationName);
            Preconditions.checkState(instanceRuntimeRpc != null, "No rpc found for %s:%s", netconfOperationNamespace,
                    netconfOperationName);
        } catch (IllegalStateException e) {
            LOG.debug("Cannot handle runtime operation {}:{}", netconfOperationNamespace, netconfOperationName, e);
            return HandlingPriority.CANNOT_HANDLE;
        }

        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY;
    }

    @Override
    protected HandlingPriority canHandle(String netconfOperationName, String namespace) {
        throw new UnsupportedOperationException(
                "This should not be used since it is not possible to provide check with these attributes");
    }

    @Override
    protected String getOperationName() {
        throw new UnsupportedOperationException("Runtime rpc does not have a stable name");
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws DocumentedException {
        // TODO check for namespaces and unknown elements
        final RpcFacade.OperationExecution execution = getConfigSubsystemFacade().getRpcFacade().fromXml(xml);

        LOG.debug("Invoking operation {} on {} with arguments {}", execution.getOperationName(), execution.getOn(),
                execution.getAttributes());
        final Object result = getConfigSubsystemFacade().getRpcFacade().executeOperation(execution);

        LOG.trace("Operation {} called successfully on {} with arguments {} with result {}", execution.getOperationName(),
                execution.getOn(), execution.getAttributes(), result);

        if (execution.isVoid()) {
            return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
        } else {
            return getConfigSubsystemFacade().getRpcFacade().toXml(document, result, execution);
        }
    }

}
