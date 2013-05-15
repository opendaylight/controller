
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.common;

/**
 * 
 * @author ttkacik
 *
 * @param <I> Input class for transformation
 * @param <P> Product of transformation
 */
public interface Transformer<I,P> {
    /**
     * 
     * @param input
     * @return true if the input is acceptable for transformation
     */
    boolean isAcceptable(I input);
    /**
     * Transforms input into instance of product.
     * 
     * @param input 
     * @return Instance of product which was created from supplied input.
     */
    P transform(I input);
}
