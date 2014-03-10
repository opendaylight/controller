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

import org.w3c.dom.Document;

import org.w3c.dom.Element;



public class DefaultStopExi extends AbstractSingletonNetconfOperation implements DefaultNetconfOperation {



    public static final String STOP_EXI = "stop-exi";

    private NetconfSession netconfSession;



    public DefaultStopExi(String netconfSessionIdForReporting) {

        super(netconfSessionIdForReporting);

    }



    @Override

    protected String getOperationName() {

        return STOP_EXI;

    }



    @Override

    protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement)

            throws NetconfDocumentedException {

        throw new UnsupportedOperationException("Not implemented");

    }



    @Override

    public void setNetconfSession(NetconfSession s) {

        this.netconfSession = s;

    }



    public NetconfSession getNetconfSession() {

        return netconfSession;

    }

}

