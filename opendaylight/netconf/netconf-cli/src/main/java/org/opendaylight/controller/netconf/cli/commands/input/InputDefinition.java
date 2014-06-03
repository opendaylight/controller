/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.input;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import java.util.SortedSet;
import java.util.TreeSet;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import com.google.common.base.Preconditions;

/**
 * The definition of input arguments represented by schema nodes parsed from yang rpc definition
 */
public class InputDefinition implements Iterable<DataSchemaNode> {

    private final SortedSet<DataSchemaNode> childNodes;

    public InputDefinition(final Set<DataSchemaNode> childNodes) {
        // Keep arguments ordered
        // at least by name, TODO yangtools do not keep arguments order
        this.childNodes = new TreeSet<>(new InputArgsLocalNameComparator());
        this.childNodes.addAll(childNodes);
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

    private static class InputArgsLocalNameComparator implements Comparator<DataSchemaNode> {
        @Override
        public int compare(final DataSchemaNode o1, final DataSchemaNode o2) {
            return o1.getQName().getLocalName().compareTo(o2.getQName().getLocalName());
        }
    }
}
