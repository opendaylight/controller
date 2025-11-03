/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OnComplete implementation that aggregates other OnComplete tasks.
 *
 * @param <T> the result type
 * @author Thomas Pantelis
 */
public abstract class CompositeOnComplete<T> implements BiConsumer<T, Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeOnComplete.class);

    private final List<BiConsumer<T, Throwable>> onCompleteTasks = new ArrayList<>();

    public final void addOnComplete(final BiConsumer<T, Throwable> task) {
        onCompleteTasks.add(task);
    }

    @SuppressWarnings({ "checkstyle:IllegalCatch", "squid:S1181" /*  Throwable and Error should not be caught */ })
    protected final void notifyOnCompleteTasks(final Throwable failure, final T result) {
        for (var task : onCompleteTasks) {
            try {
                task.accept(result, failure);
            } catch (Throwable e) {
                LOG.error("Caught unexpected exception", e);
            }
        }
        onCompleteTasks.clear();
    }
}
