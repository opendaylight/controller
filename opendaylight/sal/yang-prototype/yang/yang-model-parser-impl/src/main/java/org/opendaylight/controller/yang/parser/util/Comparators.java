/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.util.Comparator;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaNode;

public class Comparators {

    public static final QNameComparator QNAME_COMP = new QNameComparator();
    public static final SchemaNodeComparator SCHEMA_NODE_COMP = new SchemaNodeComparator();

    private Comparators() {
    }

    private static final class QNameComparator implements Comparator<QName> {
        @Override
        public int compare(QName o1, QName o2) {
            return o1.getLocalName().compareTo(o2.getLocalName());
        }
    }

    private static final class SchemaNodeComparator implements Comparator<SchemaNode> {
        @Override
        public int compare(SchemaNode o1, SchemaNode o2) {
            return o1.getQName().getLocalName().compareTo(o2.getQName().getLocalName());
        }
    }

}
