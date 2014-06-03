/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer;

import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.ChoiceNodeBaseSerializer;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;

import com.google.common.base.Preconditions;

final class ChoiceNodeCliSerializer extends ChoiceNodeBaseSerializer<String> {
    private final NodeSerializerDispatcher<String> dispatcher;

    ChoiceNodeCliSerializer(final NodeSerializerDispatcher<String> dispatcher) {
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    @Override
    protected NodeSerializerDispatcher<String> getNodeDispatcher() {
        return dispatcher;
    }
}