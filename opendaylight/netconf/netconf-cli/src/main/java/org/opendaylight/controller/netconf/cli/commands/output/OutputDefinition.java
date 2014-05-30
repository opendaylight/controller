/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.output;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import com.google.common.base.Preconditions;

public class OutputDefinition implements Iterable<DataSchemaNode> {

    private final Set<DataSchemaNode> childNodes;

    public OutputDefinition(final Set<DataSchemaNode> childNodes) {
        this.childNodes = childNodes;
    }

    @Override
    public Iterator<DataSchemaNode> iterator() {
        return childNodes.iterator();
    }

    public static OutputDefinition fromOutput(final ContainerSchemaNode output) {
        Preconditions.checkNotNull(output);
        return new OutputDefinition(output.getChildNodes());
    }

    public static OutputDefinition empty() {
        return new OutputDefinition(Collections.<DataSchemaNode> emptySet());
    }

}
