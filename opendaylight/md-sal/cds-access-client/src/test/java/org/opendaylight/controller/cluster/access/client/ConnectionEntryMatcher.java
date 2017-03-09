/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.opendaylight.controller.cluster.access.concepts.Request;

/**
 * Matcher checks, whether matched {@link ConnectionEntry} tracks provided {@link Request}.
 */
class ConnectionEntryMatcher extends BaseMatcher<ConnectionEntry> {

    private final Request request;

    /**
     * Creates a matcher that matches if the examined {@link ConnectionEntry} contains specified request.
     *
     * @param request request
     * @return matcher
     */
    public static ConnectionEntryMatcher entryWithRequest(final Request<?, ?> request) {
        return new ConnectionEntryMatcher(request);
    }

    private ConnectionEntryMatcher(final Request request) {
        this.request = request;
    }

    @Override
    public boolean matches(final Object item) {
        if (!(item instanceof ConnectionEntry)) {
            return false;
        }
        final ConnectionEntry entry = (ConnectionEntry) item;
        return this.request.equals(entry.getRequest());
    }

    @Override
    public void describeMismatch(final Object item, final Description description) {
        final ConnectionEntry entry = (ConnectionEntry) item;
        super.describeMismatch(entry.getRequest(), description);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendValue(request);
    }
}
