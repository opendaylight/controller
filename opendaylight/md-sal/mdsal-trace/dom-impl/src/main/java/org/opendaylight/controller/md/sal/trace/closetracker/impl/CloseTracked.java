/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl;

import javax.annotation.Nullable;

/**
 * Object which can track where something has been created, and if it has been correctly "closed".
 *
 * <p>Includes preserving the context of the call stack which created an object, and the instant it was created.
 *
 * @author Michael Vorburger.ch
 */
public interface CloseTracked<T extends CloseTracked<T>> {

    /**
     * This returns the allocation context as {@link StackTraceElement}s. NB that
     * this is a relatively <b>EXPENSIVE</b> operation! You should only ever call
     * this when you really need to, e.g. when you actually produce output for
     * humans, but not too early.
     */
    // TODO When we're on Java 9, then instead return a StackWalker.StackFrame[] here?
    @Nullable StackTraceElement[] getAllocationContextStackTrace();

    CloseTracked<T> getRealCloseTracked();
}
