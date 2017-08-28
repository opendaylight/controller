/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl;

import static java.util.Objects.requireNonNull;

import java.util.List;

/**
 * Element of a "report" created by a {@link CloseTrackedRegistry}.
 *
 * @author Michael Vorburger.ch
 */
public class CloseTrackedRegistryReportEntry<T extends CloseTracked<T>> {

    private final CloseTracked<T> exampleCloseTracked;
    private final long numberAddedNotRemoved;
    private final List<StackTraceElement> stackTraceElements;

    public CloseTrackedRegistryReportEntry(CloseTracked<T> exampleCloseTracked, long numberAddedNotRemoved,
            List<StackTraceElement> stackTraceElements) {
        super();
        this.exampleCloseTracked = requireNonNull(exampleCloseTracked, "closeTracked");
        this.numberAddedNotRemoved = requireNonNull(numberAddedNotRemoved, "numberAddedNotRemoved");
        this.stackTraceElements = requireNonNull(stackTraceElements, "stackTraceElements");
    }

    public long getNumberAddedNotRemoved() {
        return numberAddedNotRemoved;
    }

    public CloseTracked<T> getExampleCloseTracked() {
        return exampleCloseTracked;
    }

    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }

    @Override
    public String toString() {
        return "CloseTrackedRegistryReportEntry [numberAddedNotRemoved=" + numberAddedNotRemoved + ", closeTracked="
                + exampleCloseTracked + ", stackTraceElements.size=" + stackTraceElements.size() + "]";
    }


}
