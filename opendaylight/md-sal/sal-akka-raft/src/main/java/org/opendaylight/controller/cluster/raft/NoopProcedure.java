/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;

/**
 * An akka Procedure that does nothing.
 *
 * @author Thomas Pantelis
 *
 * @param <T> the Procedure type
 */
public class NoopProcedure<T> implements Procedure<T> {

    private static final NoopProcedure<Object> INSTANCE = new NoopProcedure<>();

    private NoopProcedure() {
    }

    @SuppressWarnings("unchecked")
    public static <T> NoopProcedure<T> instance() {
        return (NoopProcedure<T>) INSTANCE;
    }

    @Override
    public void apply(Object notUsed) {
        // nothing to do
    }
}
