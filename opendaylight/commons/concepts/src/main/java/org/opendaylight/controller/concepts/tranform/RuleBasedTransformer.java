/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.concepts.tranform;

import java.util.Set;

/**
 * Transformer with set of acceptance rules
 * 
 * The transformer provides a set of {@link Acceptor}s, which could be used to
 * verify if the input will produce result using the transformer.
 * 
 * The transormer is able to produce result if ANY of associated
 * {@link Acceptor}s accepted result.
 * 
 * @author Tony Tkacik
 * 
 * @param <I>
 *            Input class for transformation
 * @param <P>
 *            Product of transformation
 */
public interface RuleBasedTransformer<I, P> extends Transformer<I, P> {

    /**
     * Set of {@link Acceptor}, which could be used to verify if the input is
     * usable by transformer.
     * 
     * The transformer is able to produce result if ANY of associated
     * {@link Acceptor}s accepted result.
     * 
     * @return Set of input acceptance rules associated to this transformer.
     */
    Set<Acceptor<I>> getRules();

}
