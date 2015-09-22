/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.concepts.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Composite transformer which aggregates multiple implementation and selects
 * the one which accepts the input.
 *
 *
 * @author Tony Tkacik
 *
 * @param <I>
 *            Input class for transformation
 * @param <P>
 *            Product of transformation
 */
public class CompositeConditionalTransformer<I, P> implements
        SimpleConditionalTransformer<I, P>,
        AggregateTransformer<I,P> {

    private final Comparator<TransformerWithPriority<I, P>> comparator = new Comparator<TransformerWithPriority<I, P>>() {

        @Override
        public int compare(TransformerWithPriority<I, P> o1,
                TransformerWithPriority<I, P> o2) {
            return Integer.valueOf(o1.priority).compareTo(Integer.valueOf(o2.priority));
        }

    };
    private final Set<TransformerWithPriority<I, P>> transformers;

    public CompositeConditionalTransformer() {
        // FIXME: Add Ordering
        transformers = new TreeSet<TransformerWithPriority<I, P>>(comparator);
    }

    @Override
    public boolean isAcceptable(I input) {
        for (SimpleConditionalTransformer<I, P> trans : transformers) {
            if (trans.isAcceptable(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public P transform(I input) {
        for (SimpleConditionalTransformer<I, P> trans : transformers) {
            if (trans.isAcceptable(input)) {
                return trans.transform(input);
            }
        }
        throw new IllegalStateException(
                "Transformer for provided input is not available.");
    }

    public void addTransformer(SimpleConditionalTransformer<I, P> transformer,
            int priority) throws IllegalStateException {
        if (transformer == null) {
            throw new IllegalArgumentException(
                    "transformer should not be null.");
        }
        TransformerWithPriority<I, P> withPriority = new TransformerWithPriority<I, P>(
                transformer, priority);
        if (false == transformers.add(withPriority)) {
            throw new IllegalStateException("transformer " + transformer
                    + "already registered");
        }
    }

    public void removeTransformer(SimpleConditionalTransformer<I, P> transformer)
            throws IllegalArgumentException {
        if (transformer == null) {
            throw new IllegalArgumentException(
                    "transformer should not be null.");
        }
        if (false == transformers.remove(transformer)) {
            throw new IllegalStateException("transformer " + transformer
                    + "already registered");
        }
    }

    @Override
    public Collection<P> transformAll(Collection<? extends I> inputs) {
        Collection<P> ret = new ArrayList<P>();
        for (I i : inputs) {
            ret.add(transform(i));
        }
        return ret;
    }

    private static class TransformerWithPriority<I, P> implements
            SimpleConditionalTransformer<I, P> {
        final int priority;
        final SimpleConditionalTransformer<I, P> transformer;

        public TransformerWithPriority(
                SimpleConditionalTransformer<I, P> transformer, int priority) {
            this.priority = priority;
            this.transformer = transformer;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((transformer == null) ? 0 : transformer.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TransformerWithPriority<?,?> other = (TransformerWithPriority<?,?>) obj;
            if (transformer == null) {
                if (other.transformer != null)
                    return false;
            } else if (!transformer.equals(other.transformer))
                return false;
            return true;
        }

        @Override
        public boolean isAcceptable(I input) {
            return transformer.isAcceptable(input);
        }

        @Override
        public P transform(I input) {
            return transformer.transform(input);
        }





    }
}
