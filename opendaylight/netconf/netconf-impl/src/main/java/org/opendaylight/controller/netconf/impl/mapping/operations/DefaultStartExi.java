/*

 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.

 *

 * This program and the accompanying materials are made available under the

 * terms of the Eclipse Public License v1.0 which accompanies this distribution,

 * and is available at http://www.eclipse.org/legal/epl-v10.html

 */

package org.opendaylight.controller.netconf.impl.mapping.operations;



import org.opendaylight.controller.netconf.api.NetconfDocumentedException;

import org.opendaylight.controller.netconf.api.NetconfSession;

import org.opendaylight.controller.netconf.mapping.api.DefaultNetconfOperation;

import org.opendaylight.controller.netconf.util.mapping.AbstractSingletonNetconfOperation;

import org.opendaylight.controller.netconf.util.xml.XmlElement;

import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;

import org.opendaylight.controller.netconf.util.xml.XmlUtil;

import org.w3c.dom.Document;

import org.w3c.dom.Element;



public class DefaultStartExi extends AbstractSingletonNetconfOperation implements DefaultNetconfOperation {



    public static final String START_EXI = "start-exi";



    private NetconfSession netconfSession;





    public DefaultStartExi(String netconfSessionIdForReporting) {

        super(netconfSessionIdForReporting);

    }



    @Override

    protected String getOperationName() {

        return START_EXI;

    }



    @Override

    protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement) throws NetconfDocumentedException {





        Element getSchemaResult = document

                .createElement(XmlNetconfConstants.OK);

        XmlUtil.addNamespaceAttr(getSchemaResult,

                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        throw new UnsupportedOperationException("Not implemented");

    }



    @Override

    public void setNetconfSession(NetconfSession s) {

        netconfSession = s;

    }



    public NetconfSession getNetconfSession() {

        return netconfSession;

    }





}

