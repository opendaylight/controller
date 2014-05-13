/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.concepts.lang;
/**
 * Simple condition-based transformer
 *
 * The transformer provides {@link #isAcceptable(Object)} method,
 * which could be used to query transformer if the input will produce
 * result.
 *
 * This interface is simplified version of {@link RuleBasedTransformer} - does not
 * provide decoupling of Acceptance rule from transformer, and should be used only
 * for simple use-cases.
 *
 * @author Tony Tkacik
 *
 * @param <I> Input class for transformation
 * @param <P> Product of transformation
 */
public interface SimpleConditionalTransformer<I,P> extends Transformer<I, P>, Acceptor<I> {


    /**
     * Checks if the input is acceptable
     * for processing by the transformer.
     *
     * @return true it the input is acceptable for processing by transformer.
     */
    @Override
    public boolean isAcceptable(I input);
}
