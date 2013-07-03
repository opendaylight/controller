/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import org.opendaylight.controller.yang.common.QName;

import java.util.Set;

/**
 * The ChoiceNode defines a set of alternatives. It consists of a number of
 * branches defined as ChoiceCaseNode objects.
 */
public interface ChoiceNode extends DataSchemaNode, AugmentationTarget {

    /**
     * @return ChoiceCaseNode objects defined in this node
     */
    Set<ChoiceCaseNode> getCases();

    /**
     * @param name
     *            QName of seeked Choice Case Node
     * @return child case node of this Choice if child with given name is
     *         present, <code>null</code> otherwise
     */
    ChoiceCaseNode getCaseNodeByName(QName name);

    /**
     * @param name
     *            name of seeked child as String
     * @return child case node (or local name of case node) of this Choice if child with given name is
     *         present, <code>null</code> otherwise
     */
    ChoiceCaseNode getCaseNodeByName(String name);

    String getDefaultCase();

}
