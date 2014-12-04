/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.netconf.monitoring.xml.model.NetconfState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JaxBSerializer {
    private static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(NetconfState.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public Element toXml(final NetconfState monitoringModel) {
        final DOMResult res;
        try {
            final Marshaller marshaller = JAXB_CONTEXT.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            res = new DOMResult();
            marshaller.marshal(monitoringModel, res);
        } catch (final JAXBException e) {
            throw new RuntimeException("Unable to serialize netconf state " + monitoringModel, e);
        }
        return ((Document)res.getNode()).getDocumentElement();
    }
}
