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

import com.google.common.base.Optional;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement
public class Filter implements FilterConfig {
    private static final Logger logger = LoggerFactory.getLogger(Filter.class);

    private String filterName;
    private String filterClass;
    private List<InitParam> initParams = new ArrayList<>();
    private javax.servlet.Filter actualFilter;
    private boolean initialized, isTemplate;


    /**
     * Called in filter-template nodes defined in <Host/> node - do not actually initialize the filter.
     * In this case filter is only used to hold values of init params to be merged with
     * filter defined in <Context/>
     */
    public synchronized void initializeTemplate(){
        checkState(initialized == false, "Already initialized");
        for (InitParam initParam : initParams) {
            initParam.inititialize();
        }
        isTemplate = true;
        initialized = true;
    }


    public synchronized void initialize(String fileName, Optional<Filter> maybeTemplate) {
        checkState(initialized == false, "Already initialized");
        logger.trace("Initializing filter {} : {}", filterName, filterClass);
        for (InitParam initParam : initParams) {
            initParam.inititialize();
        }
        if (maybeTemplate.isPresent()) {
            // merge non conflicting init params
            Filter template = maybeTemplate.get();
            checkArgument(template.isTemplate);
            Map<String, InitParam> templateParams = template.getInitParamsMap();
            Map<String, InitParam> currentParams = getInitParamsMap();
            // add values of template that are not present in current
            MapDifference<String, InitParam> difference = Maps.difference(templateParams, currentParams);
            for (Entry<String, InitParam> templateUnique : difference.entriesOnlyOnLeft().entrySet()) {
                initParams.add(templateUnique.getValue());
            }
            // merge filterClass
            if (filterClass == null) {
                filterClass = template.filterClass;
            } else if (Objects.equals(filterClass, template.filterClass) == false) {
                logger.error("Conflict detected in filter-class of {} defined in {}, template class {}, child class {}" ,
                        filterName, fileName, template.filterClass, filterClass);
                throw new IllegalStateException("Conflict detected in template/filter filter-class definitions," +
                        " filter name: " + filterName + " in file " + fileName);
            }
        }
        initParams = Collections.unmodifiableList(new ArrayList<>(initParams));
        Class<?> clazz;
        try {
            clazz = Class.forName(filterClass);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate class defined in filter " + filterName
                    + " in file " + fileName, e);
        }
        try {
            actualFilter = (javax.servlet.Filter) clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate class defined in filter " + filterName
                    + " in file " + fileName, e);
        }
        logger.trace("Initializing {} with following init-params:{}", filterName, getInitParams());
        try {
            actualFilter.init(this);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize filter " + filterName
                    + " in file " + fileName, e);
        }
        initialized = true;
    }

    @Override
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException("Getting ServletContext is currently not supported");
    }

    @Override
    public String getInitParameter(String name) {
        for (InitParam initParam : initParams) {
            if (Objects.equals(name, initParam.getParamName())) {
                return initParam.getParamValue();
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        final Iterator<InitParam> iterator = initParams.iterator();
        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next().getParamName();
            }
        };
    }

    public javax.servlet.Filter getActualFilter() {
        checkState(initialized, "Not initialized");
        return actualFilter;
    }

    public boolean isInitialized() {
        return initialized;
    }


    @XmlElement(name = "filter-name")
    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    @XmlElement(name = "filter-class")
    public String getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }

    @XmlElement(name = "init-param")
    public List<InitParam> getInitParams() {
        return initParams;
    }

    public void setInitParams(List<InitParam> initParams) {
        this.initParams = initParams;
    }


    @Override
    public String toString() {
        return "Filter{" +
                "filterName='" + filterName + '\'' +
                '}';
    }

    public Map<String, InitParam> getInitParamsMap() {
        Map<String, InitParam> result = new HashMap<>();
        for (InitParam initParam : initParams) {
            checkState(initParam.isInitialized());
            result.put(initParam.getParamName(), initParam);
        }
        return result;
    }
}
