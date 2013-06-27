/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

/**
 * Marker interface for nodes which can be defined in grouping statement.
 * [anyxml, choice, container, grouping, leaf, leaf-list, list, typedef, uses]
 */
public interface GroupingMember extends Builder {

    boolean isAddedByUses();

    void setAddedByUses(boolean addedByUses);

}
