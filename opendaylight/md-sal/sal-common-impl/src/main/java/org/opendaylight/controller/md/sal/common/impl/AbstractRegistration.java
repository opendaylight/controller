package org.opendaylight.controller.md.sal.common.impl;

import org.opendaylight.yangtools.concepts.Registration;

public abstract class AbstractRegistration<T> implements Registration<T> {


    private final T instance;

    public AbstractRegistration(T instance) {
        super();
        this.instance = instance;
    }

    @Override
    public final T getInstance() {
        return instance;
    }

}
