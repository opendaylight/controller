/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.api;

import ch.qos.logback.core.Appender;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Text;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public abstract class HasAppendersImpl<APPENDERTO> implements HasAppenders {
    protected Collection<APPENDERTO> appenderTOs;

    protected HasAppendersImpl(Collection<APPENDERTO> appenderTOs) {
        this.appenderTOs = appenderTOs;
    }

    @Override
    public Map<String, Element> getXmlRepresentationOfAppenders() {
        Map<String, Element> result = new HashMap<>();
        for (APPENDERTO appender : appenderTOs) {
            Element appenderElement = getElement(appender);
            checkState("appender".equals(appenderElement.getLocalName()), "Unexpected node name " + appenderElement);
            String name = appenderElement.getAttributeValue("name");
            checkState(name != null, "Attribute name is null");
            result.put(name, appenderElement);
        }
        return result;
    }

    protected abstract Element getElement(APPENDERTO appenderTO);

    public Collection<APPENDERTO> getAppenderTOs() {
        return appenderTOs;
    }

    protected Element fillElement(Class<? extends Appender> appenderClass, String name, String encoderPattern, String nullableThreshold) {

        checkArgument(name != null, "Name is null");
        checkArgument(encoderPattern != null, "Pattern is null");

        Element appenderElement = new Element("appender");
        appenderElement.addAttribute(new Attribute("name", name));

        appenderElement.addAttribute(new Attribute("class", appenderClass.getCanonicalName()));

        // add encoder
        Element encoder = new Element("encoder");
        appenderElement.appendChild(encoder);
        Element pattern = new Element("pattern");
        encoder.appendChild(pattern);
        pattern.appendChild(new Text(encoderPattern));

        if (nullableThreshold != null) {
            Element filter = new Element("filter");
            appenderElement.appendChild(filter);
            filter.addAttribute(new Attribute("class", ch.qos.logback.classic.filter.ThresholdFilter.class.getCanonicalName()));
            Element level = new Element("level");
            filter.appendChild(level);
            level.appendChild(new Text(nullableThreshold));
        }
        return appenderElement;
    }

    @Override
    public void close() throws Exception {

    }
}
