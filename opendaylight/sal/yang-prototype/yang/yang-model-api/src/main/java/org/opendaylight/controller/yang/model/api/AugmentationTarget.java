/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.util.Set;

/**
 * Interface for all nodes which are possible targets of augmentation. The
 * target node of augmentation MUST be either a container, list, choice, case,
 * input, output, or notification node.
 */
public interface AugmentationTarget {

    /**
     * @return set of augmentations targeting this element.
     */
    Set<AugmentationSchema> getAvailableAugmentations();

}
