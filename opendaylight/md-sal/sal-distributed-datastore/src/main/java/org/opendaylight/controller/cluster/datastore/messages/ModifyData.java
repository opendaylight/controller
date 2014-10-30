/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ModifyData {
    private static final Logger LOG = LoggerFactory.getLogger(ModifyData.class);

    private final YangInstanceIdentifier path;
    private final NormalizedNode<?, ?> data;

    public ModifyData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        this.path = path;
        this.data = data;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }
}
