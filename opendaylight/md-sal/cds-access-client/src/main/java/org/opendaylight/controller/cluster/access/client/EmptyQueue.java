/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

/**
 * A specialized always-empty implementation of {@link java.util.Queue}. This implementation will always refuse new
 * elements in its {@link #offer(Object)} method.

 * @author Robert Varga
 *
 * @param <E> the type of elements held in this collection
 */
// TODO: move this class into yangtools.util
@Beta
public final class EmptyQueue<E> extends AbstractQueue<E> {
    private static final EmptyQueue<?> INSTANCE = new EmptyQueue<>();

    private EmptyQueue() {
        // No instances
    }

    @SuppressWarnings("unchecked")
    public static <T> Queue<T> getInstance() {
        return (Queue<T>) INSTANCE;
    }

    @Override
    public boolean offer(final E e) {
        return false;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public int size() {
        return 0;
    }
}