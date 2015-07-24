/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import java.util.Arrays;
import java.util.List;

class ValueForMessage {
    List<String> messageKeyWords;
    String value;

    public ValueForMessage(final String value, final String... messageKeyWords) {
        this.messageKeyWords = Arrays.asList(messageKeyWords);
        this.value = value;
    }

    public List<String> getKeyWords() {
        return messageKeyWords;
    }

    public String getValue() {
        return value;
    }
}
