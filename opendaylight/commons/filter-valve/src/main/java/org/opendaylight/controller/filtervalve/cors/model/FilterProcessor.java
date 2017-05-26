/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.model;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.opendaylight.controller.filtervalve.cors.jaxb.Context;
import org.opendaylight.controller.filtervalve.cors.jaxb.Filter;
import org.opendaylight.controller.filtervalve.cors.jaxb.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FilterProcessor.class);

    private final Host host;

    public FilterProcessor(Host host) {
        this.host = host;
    }

    public void process(Request request, Response response, FilterChain nextValveFilterChain)
            throws IOException, ServletException {

        String contextPath = request.getContext().getPath();
        String path = request.getDecodedRequestURI();

        Optional<Context> maybeContext = host.findContext(contextPath);
        logger.trace("Processing context {} path {}, found {}", contextPath, path, maybeContext);
        if (maybeContext.isPresent()) {
            // process filters
            Context context = maybeContext.get();
            List<Filter> matchingFilters = context.findMatchingFilters(path);
            FilterChain fromLast = nextValveFilterChain;
            ListIterator<Filter> it = matchingFilters.listIterator(matchingFilters.size());
            final boolean trace = logger.isTraceEnabled();
            while (it.hasPrevious()) {
                final Filter currentFilter = it.previous();
                final FilterChain copy = fromLast;
                fromLast = new FilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                        if (trace) {
                            logger.trace("Applying {}", currentFilter);
                        }
                        javax.servlet.Filter actualFilter = currentFilter.getActualFilter();
                        actualFilter.doFilter(request, response, copy);
                    }
                };
            }
            // call first filter
            fromLast.doFilter(request, response);
        } else {
            // move to next valve
            nextValveFilterChain.doFilter(request, response);
        }
    }
}
