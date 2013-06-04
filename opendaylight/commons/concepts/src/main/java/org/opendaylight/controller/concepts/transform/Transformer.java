/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.concepts.transform;

/**
 * Factory which produces product based on input object
 * 
 * @author Tony Tkacik
 *
 * @param <I> Input
 * @param <P> Product
 */
public interface Transformer<I,P> {
    /**
     * Transforms input into instance of product.
     * 
     * @param input Input which drives transformation
     * @return Instance of product which was created from supplied input.
     */
    P transform(I input);
}
