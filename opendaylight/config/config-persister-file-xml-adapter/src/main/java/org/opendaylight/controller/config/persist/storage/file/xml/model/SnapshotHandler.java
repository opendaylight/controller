/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.file.xml.model;

import com.google.common.base.Preconditions;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.DomHandler;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

class SnapshotHandler implements DomHandler<String, StreamResult> {

    private static final String START_TAG = "<configuration>";
    private static final String END_TAG = "</configuration>";

    private StringWriter xmlWriter = new StringWriter();

    public StreamResult createUnmarshaller(ValidationEventHandler errorHandler) {
        xmlWriter.getBuffer().setLength(0);
        return new StreamResult(xmlWriter);
    }

    public String getElement(StreamResult rt) {
        String xml = rt.getWriter().toString();
        int beginIndex = xml.indexOf(START_TAG) + START_TAG.length();
        int endIndex = xml.indexOf(END_TAG);
        Preconditions.checkArgument(beginIndex != -1 && endIndex != -1,
                "Unknown element present in config snapshot(expected only configuration): %s", xml);
        return xml.substring(beginIndex, endIndex);
    }

    public Source marshal(String n, ValidationEventHandler errorHandler) {
        try {
            String xml = START_TAG + n.trim() + END_TAG;
            StringReader xmlReader = new StringReader(xml);
            return new StreamSource(xmlReader);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
