/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NodeIdentifierGenerator {
    private final String id;
    private final QName qName;

    public NodeIdentifierGenerator(String id){
        this.id = id;
        this.qName = QNameFactory.create(id);
    }

    public YangInstanceIdentifier.PathArgument getArgument(){
        return new YangInstanceIdentifier.NodeIdentifier(qName);
    }
}
