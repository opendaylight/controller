/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.jaxb;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.controller.filtervalve.cors.model.UrlMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement
public class Context {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    private String path;
    private List<Filter> filters = new ArrayList<>();
    private List<FilterMapping> filterMappings = new ArrayList<>();
    private boolean initialized;
    private UrlMatcher<Filter> urlMatcher;


    public synchronized void initialize(String fileName, Map<String, Filter> namesToTemplates) {
        checkState(initialized == false, "Already initialized");
        Map<String, Filter> namesToFilters = new HashMap<>();
        for (Filter filter : filters) {
            try {
                filter.initialize(fileName, Optional.fromNullable(namesToTemplates.get(filter.getFilterName())));
            } catch (Exception e) {
                throw new IllegalStateException(format("Error while processing filter %s of context %s, defined in %s",
                        filter.getFilterName(), path, fileName), e);
            }
            namesToFilters.put(filter.getFilterName(), filter);
        }
        filters = Collections.unmodifiableList(new ArrayList<>(filters));
        LinkedHashMap<String, Filter> patternMap = new LinkedHashMap<>();
        for (FilterMapping filterMapping : filterMappings) {
            filterMapping.initialize();
            Filter found = namesToFilters.get(filterMapping.getFilterName());
            if (found != null) {
                patternMap.put(filterMapping.getUrlPattern(), found);
            } else {
                logger.error("Cannot find matching filter for filter-mapping {} of context {}, defined in {}",
                        filterMapping.getFilterName(), path, fileName);
                throw new IllegalStateException(format(
                        "Cannot find filter for filter-mapping %s of context %s, defined in %s",
                        filterMapping.getFilterName(), path, fileName));
            }
        }
        filterMappings = Collections.unmodifiableList(new ArrayList<>(filterMappings));
        urlMatcher = new UrlMatcher<>(patternMap);
        initialized = true;
    }

    public List<Filter> findMatchingFilters(String path) {
        logger.trace("findMatchingFilters({})", path);
        checkState(initialized, "Not initialized");
        return urlMatcher.findMatchingFilters(path);
    }

    @XmlAttribute(name = "path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        checkArgument(initialized == false, "Already initialized");
        this.path = path;
    }

    @XmlElement(name = "filter")
    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        checkArgument(initialized == false, "Already initialized");
        this.filters = filters;
    }

    @XmlElement(name = "filter-mapping")
    public List<FilterMapping> getFilterMappings() {
        return filterMappings;
    }

    public void setFilterMappings(List<FilterMapping> filterMappings) {
        checkArgument(initialized == false, "Already initialized");
        this.filterMappings = filterMappings;
    }

    @Override
    public String toString() {
        return "Context{" +
                "path='" + path + '\'' +
                '}';
    }
}
