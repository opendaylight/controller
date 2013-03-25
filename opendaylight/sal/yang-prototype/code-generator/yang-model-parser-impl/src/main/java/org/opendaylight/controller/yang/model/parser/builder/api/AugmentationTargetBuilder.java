/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.api;

import org.opendaylight.controller.yang.model.api.AugmentationSchema;

/**
 * Interface for builders of those nodes, which can be augmentation targets.
 */
public interface AugmentationTargetBuilder {

	/**
	 * Add augment, which points to this node.
	 * @param augment augment which points to this node
	 */
	void addAugmentation(AugmentationSchema augment);

}
