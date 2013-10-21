/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api;

import org.w3c.dom.Document;

/**
 * NetconfMessage represents a wrapper around org.w3c.dom.Document. Needed for
 * implementing ProtocolMessage interface.
 */
public final class NetconfMessage {

    private static final long serialVersionUID = 462175939836367285L;

    private final Document doc;

    public NetconfMessage(final Document doc) {
        this.doc = doc;
    }

    public Document getDocument() {
        return this.doc;
    }
}
