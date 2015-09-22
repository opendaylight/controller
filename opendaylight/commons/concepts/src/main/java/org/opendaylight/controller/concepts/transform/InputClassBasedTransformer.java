/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.concepts.transform;

/**
 * Input class based transformer
 *
 * {@link Transformer} which accepts / transforms only specific classes of
 * input, and is useful if the selection of transformer should be based on the
 * class of the input and there is one-to-one mapping between input class and
 * transformer.
 *
 *
 * @author Tony Tkacik
 *
 * @param <S>
 *            Common supertype of input
 * @param <I>
 *            Concrete type of input
 * @param <P>
 *            Product
 */
public interface InputClassBasedTransformer<S, I extends S, P> extends
        Transformer<I, P> {

    /**
     * Returns an {@link Class} of input which is acceptable for transformation.
     *
     * @return {@link Class} of input which is acceptable for transformation.
     */
    Class<? extends S> getInputClass();
}
