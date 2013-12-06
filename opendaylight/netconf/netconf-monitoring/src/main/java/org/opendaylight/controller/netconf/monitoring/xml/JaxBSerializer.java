/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.monitoring.xml;

import org.opendaylight.controller.netconf.monitoring.xml.model.NetconfState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.dom.DOMResult;

public class JaxBSerializer {

    public Element toXml(NetconfState monitoringModel) {
        DOMResult res = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(NetconfState.class);
            Marshaller marshaller = jaxbContext.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            res = new DOMResult();
            marshaller.marshal(monitoringModel, res);
        } catch (JAXBException e) {
           throw new RuntimeException("Unable to serialize netconf state " + monitoringModel, e);
        }
        return ((Document)res.getNode()).getDocumentElement();
    }
}
