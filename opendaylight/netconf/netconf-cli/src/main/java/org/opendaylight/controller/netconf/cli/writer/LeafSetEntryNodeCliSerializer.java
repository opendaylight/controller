/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer;

import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.LeafSetEntryNodeBaseSerializer;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

import com.google.common.base.Preconditions;

final class LeafSetEntryNodeCliSerializer extends LeafSetEntryNodeBaseSerializer<String> {

    private final XmlCodecProvider codecProvider;

    LeafSetEntryNodeCliSerializer(final XmlCodecProvider codecProvider) {
        this.codecProvider = Preconditions.checkNotNull(codecProvider);
    }

    @Override
    protected String serializeLeaf(final LeafListSchemaNode schema, final LeafSetEntryNode<?> node) {
        return node.getValue().toString();
    }
}