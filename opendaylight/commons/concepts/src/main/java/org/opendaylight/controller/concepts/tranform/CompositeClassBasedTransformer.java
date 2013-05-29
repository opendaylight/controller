/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.concepts.tranform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Transformer which aggregates multiple implementations of
 * {@link InputClassBasedTransformer}.
 * 
 * The transformation process is driven by {@link Class} of input. The selection
 * of used {@link InputClassBasedTransformer} is done by using the {@link Class}
 * of input as a key to select the transformer.
 * 
 * This approach provides quick resolution of transformer, but does not support
 * registering a super type of input to provide transformation support for all
 * subclasses, one must register a new instance of transformer for each valid
 * input class.
 * 
 * If you need more flexible selection of transformation consider using
 * {@link CompositeConditionalTransformer} which is slower but most flexible or
 * {@link RuleBasedTransformer} which provides declarative approach for
 * transformation.
 * 
 * See {@link #transform(Object)} for more information about tranformation
 * process.
 * 
 * @author Tony Tkacik <ttkacik@cisco.com>
 * 
 * @param <I>
 *            Input super-type
 * @param <P>
 *            Product
 */
public abstract class CompositeClassBasedTransformer<I, P> implements
        InputClassBasedTransformer<I, I, P>,
        AggregateTransformer<I, P> {

    private Map<Class<? extends I>, InputClassBasedTransformer<I, ? extends I, P>> transformers = new ConcurrentHashMap<Class<? extends I>, InputClassBasedTransformer<I, ? extends I, P>>();

    /**
     * Transforms an input into instance of Product class.
     * 
     * The final registered transformer is the one which match following
     * condition:
     * 
     * <code>input.getClass() == transformer.getInputClass()</code>
     * 
     * This means that transformers are not resolved by class hierarchy, only
     * selected based on final class of the input. If you need more flexible
     * selection of transformation consider using
     * {@link CompositeConditionalTransformer} which is slower but more
     * flexible.
     * 
     */
    @Override
    public P transform(I input) {
        @SuppressWarnings("unchecked")
        InputClassBasedTransformer<I, I, P> transformer = (InputClassBasedTransformer<I, I, P>) transformers
                .get(input.getClass());
        if (transformer == null)
            throw new IllegalArgumentException("Transformation of: " + input
                    + " is not supported");
        return transformer.transform(input);
    }

    /**
     * Registers a new transformer.
     * 
     * The transformer is registered for class returned by
     * {@link InputClassBasedTransformer#getInputClass()}. Only one transformer
     * can be registered for particular input class.
     * 
     */
    public void addTransformer(
            InputClassBasedTransformer<I, ? extends I, P> transformer)
            throws IllegalStateException {
        if (transformer == null)
            throw new IllegalArgumentException("Transformer should not be null");
        if (transformer.getInputClass() == null)
            throw new IllegalArgumentException(
                    "Transformer should specify input class.");
        transformers.put(transformer.getInputClass(), transformer);
    }

    /**
     * Removes an registered transformer.
     * 
     * Note: Removal is currently unsupported.
     * 
     * @param transformer
     *            Tranformer to be removed.
     * @throws IllegalArgumentException
     *             If the provided transformer is null or is not registered.
     */
    public void removeTransformer(
            InputClassBasedTransformer<I, ? extends I, P> transformer)
            throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Collection<P> transformAll(Collection<? extends I> inputs) {
        Collection<P> ret = new ArrayList<P>();
        for (I i : inputs) {
            ret.add(transform(i));
        }
        return ret;
    }

}
