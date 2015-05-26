/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;

public class NormalizedNodeBuilderWrapper {
    private final NormalizedNodeContainerBuilder<?,?,?,?> builder;
    private final YangInstanceIdentifier.PathArgument identifier;


    NormalizedNodeBuilderWrapper(NormalizedNodeContainerBuilder<?,?,?,?> builder, YangInstanceIdentifier.PathArgument identifier) {
        this.builder = builder;
        this.identifier = identifier;
    }

    public NormalizedNodeContainerBuilder builder(){
        return builder;
    }

    public QName nodeType(){
        return identifier.getNodeType();
    }

    public YangInstanceIdentifier.PathArgument identifier(){
        return identifier;
    }

}
