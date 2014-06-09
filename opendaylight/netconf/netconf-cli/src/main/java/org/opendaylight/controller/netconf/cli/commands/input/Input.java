/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.input;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;

/**
 * Input arguments for and rpc/command execution
 */
public class Input {

    private final Map<QName, List<Node<?>>> args;

    public Input(final Map<QName, List<Node<?>>> args) {
        this.args = args;
    }

    public Map<QName, List<Node<?>>> getArgs() {
        return args;
    }

    public CompositeNode wrap(final QName rpcQName) {
        final List<Node<?>> flatArgs = Lists.newArrayList();

        for (final List<Node<?>> nodes : sortArgs(args).values()) {
            // TODO no null elements from read
            flatArgs.addAll(Collections2.filter(nodes, new Predicate<Node<?>>() {
                @Override
                public boolean apply(@Nullable final Node<?> input) {
                    return input != null;
                }
            }));
        }

        return new CompositeNodeTOImpl(rpcQName, null, flatArgs);
    }

    // FIXME, sort alphabetically arguments since schema nodes do not preserve order
    // For ODL we get operation mismatch based on argument ordering
    // ODL sorts arguments alphabetically
    // This should be fixed by yangtools preserving node ordering
    private Map<QName, List<Node<?>>> sortArgs(final Map<QName, List<Node<?>>> args) {
        final TreeMap<QName, List<Node<?>>> sortedMap = new TreeMap<>(new Comparator<QName>() {
            @Override
            public int compare(final QName o1, final QName o2) {
                return o1.getLocalName().compareTo(o2.getLocalName());
            }
        });

        for (final Entry<QName, List<Node<?>>> qNameListEntry : args.entrySet()) {
            sortedMap.put(qNameListEntry.getKey(), qNameListEntry.getValue());
        }

        return sortedMap;
    }
}
