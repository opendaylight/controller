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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Root element, arbitrarily named Host to match tomcat-server.xml, but does not allow specifying which host
 * name to be matched.
 */
@XmlRootElement(name = "Host")
public class Host {
    private List<Context> contexts = new ArrayList<>();
    private List<Filter> filterTemplates = new ArrayList<>();
    private boolean initialized;
    private Map<String, Context> contextMap;


    public synchronized void initialize(String fileName) {
        checkState(initialized == false, "Already initialized");
        Map<String, Filter> namesToTemplates = new HashMap<>();
        for (Filter template : filterTemplates) {
            template.initializeTemplate();
            namesToTemplates.put(template.getFilterName(), template);
        }
        contextMap = new HashMap<>();
        for (Context context : getContexts()) {
            checkState(contextMap.containsKey(context.getPath()) == false,
                    "Context {} already defined in {}", context.getPath(), fileName);
            context.initialize(fileName, namesToTemplates);
            contextMap.put(context.getPath(), context);
        }
        contextMap = Collections.unmodifiableMap(new HashMap<>(contextMap));
        contexts = Collections.unmodifiableList(new ArrayList<>(contexts));
        initialized = true;
    }

    public Optional<Context> findContext(String contextPath) {
        checkState(initialized, "Not initialized");
        Context context = contextMap.get(contextPath);
        return Optional.fromNullable(context);
    }

    @XmlElement(name = "Context")
    public List<Context> getContexts() {
        return contexts;
    }

    public void setContexts(List<Context> contexts) {
        checkArgument(initialized == false, "Already initialized");
        this.contexts = contexts;
    }

    @XmlElement(name = "filter-template")
    public List<Filter> getFilterTemplates() {
        return filterTemplates;
    }

    public void setFilterTemplates(List<Filter> filterTemplates) {
        checkArgument(initialized == false, "Already initialized");
        this.filterTemplates = filterTemplates;
    }
}
