/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.jaxb;

import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class Parser {

    public static Host parse(String xmlFileContent, String fileName) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Host.class);
        javax.xml.bind.Unmarshaller um = context.createUnmarshaller();
        Host host = (Host) um.unmarshal(new StringReader(xmlFileContent));
        host.initialize(fileName);
        return host;
    }

}
