/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Registry of {@link CloseTracked} instances.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
public class CloseTrackedRegistry<T extends CloseTracked<T>> {

    // unused OK for now, at least we'll be able to see this in HPROF heap dumps and know what is which
    private final @SuppressWarnings("unused") Object anchor;
    private final @SuppressWarnings("unused") String createDescription;

    private final Set<CloseTracked<T>> tracked = new ConcurrentSkipListSet<>(
        (o1, o2) -> o1.getObjectCreated().compareTo(o2.getObjectCreated()));

    private final boolean isDebugContextEnabled;

    /**
     * Constructor.
     *
     * @param anchor
     *            object where this registry is stored in, used for human output in
     *            logging and other output
     * @param createDescription
     *            description of creator of instances of this registry, typically
     *            e.g. name of method in the anchor class
     * @param isDebugContextEnabled
     *            whether or not the call stack should be preserved; this is (of
     *            course) an expensive operation, and should only be used during
     *            troubleshooting
     */
    public CloseTrackedRegistry(Object anchor, String createDescription, boolean isDebugContextEnabled) {
        this.anchor = anchor;
        this.createDescription = createDescription;
        this.isDebugContextEnabled = isDebugContextEnabled;
    }

    public boolean isDebugContextEnabled() {
        return isDebugContextEnabled;
    }

    // package protected, not public; only CloseTrackedTrait invokes this
    void add(CloseTracked<T> closeTracked) {
        tracked.add(closeTracked);
    }

    // package protected, not public; only CloseTrackedTrait invokes this
    void remove(CloseTracked<T> closeTracked) {
        tracked.remove(closeTracked);
    }

    // TODO Later add methods to dump & query what's not closed, by creation time, incl. creation stack trace

    // TODO we could even support add/close of Object instead of CloseTracked, by creating a wrapper?

}
