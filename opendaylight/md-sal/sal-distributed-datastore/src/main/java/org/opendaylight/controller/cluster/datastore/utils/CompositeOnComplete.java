/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import akka.dispatch.OnComplete;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An OnComplete implementation that aggrgates other OnComplete tasks.
 *
 * @author Thomas Pantelis
 *
 * @param <T> the result type
 */
public abstract class CompositeOnComplete<T> extends OnComplete<T> {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeOnComplete.class);

    private final List<OnComplete<T>> onCompleteTasks = new ArrayList<>();

    public void addOnComplete(OnComplete<T> task) {
        onCompleteTasks.add(task);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void notifyOnCompleteTasks(Throwable failure, T result) {
        for (OnComplete<T> task: onCompleteTasks) {
            try {
                task.onComplete(failure, result);
            } catch (Throwable e) {
                LOG.error("Caught unexpected exception", e);
            }
        }

        onCompleteTasks.clear();
    }
}
