/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.input;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import com.google.common.base.Preconditions;

/**
 * The definition of input arguments represented by schema nodes parsed from yang rpc definition
 */
public class InputDefinition implements Iterable<DataSchemaNode> {

    private final Set<DataSchemaNode> childNodes;

    public InputDefinition(final Set<DataSchemaNode> childNodes) {
        this.childNodes = childNodes;
    }

    @Override
    public Iterator<DataSchemaNode> iterator() {
        return childNodes.iterator();
    }

    public static InputDefinition fromInput(final ContainerSchemaNode input) {
        Preconditions.checkNotNull(input);
        return new InputDefinition(input.getChildNodes());
    }

    public static InputDefinition empty() {
        return new InputDefinition(Collections.<DataSchemaNode> emptySet());
    }

}
