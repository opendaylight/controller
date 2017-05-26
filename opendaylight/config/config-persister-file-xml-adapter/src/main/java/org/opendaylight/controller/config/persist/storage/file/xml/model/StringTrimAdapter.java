/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.file.xml.model;

import javax.xml.bind.annotation.adapters.XmlAdapter;

final class StringTrimAdapter extends XmlAdapter<String, String> {
    @Override
    public String unmarshal(String v) throws Exception {
        if (v == null) {
            return null;
        }
        return v.trim();
    }

    @Override
    public String marshal(String v) throws Exception {
        if (v == null) {
            return null;
        }
        return v.trim();
    }
}
