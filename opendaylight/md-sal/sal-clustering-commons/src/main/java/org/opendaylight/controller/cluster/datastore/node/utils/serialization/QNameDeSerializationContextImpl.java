/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import java.util.List;

/**
 * Implementation of the QNameDeSerializationContext interface.
 *
 * @author Thomas Pantelis
 */
public class QNameDeSerializationContextImpl implements QNameDeSerializationContext {

    private final List<String> codeList;

    public QNameDeSerializationContextImpl(List<String> codeList) {
        this.codeList = codeList;
    }

    @Override
    public String getNamespace(int namespace) {
        return codeList.get(namespace);
    }

    @Override
    public String getRevision(int revision) {
        return codeList.get(revision);
    }

    @Override
    public String getLocalName(int localName) {
        return codeList.get(localName);
    }
}
