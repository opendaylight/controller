package org.opendaylight.controller.sal.common;

public interface ExtensibleTransformer<I,P> extends Transformer<I,P> {

    void addTransformer(Transformer<I,P> transformer) throws IllegalStateException;
    void removeTransformer(Transformer<I,P> transformer) throws IllegalArgumentException;
}
