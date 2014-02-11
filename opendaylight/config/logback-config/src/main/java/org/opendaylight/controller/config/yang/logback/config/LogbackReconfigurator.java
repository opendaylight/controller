/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Based on appender XML elements and Logger TOs create in-memory logback.xml and
 * push it to Joran APIs
 */
public class LogbackReconfigurator implements AutoCloseable {
    private final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    private final LogbackStatusListener runtimeBean;
    private final Document logbackDocument;

    public LogbackReconfigurator(Collection<Element> appenderElements, Collection<LoggerTO> loggerTOs, LogbackStatusListener runtimeBean) {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);

        logbackDocument = createLogbackDocument(appenderElements, loggerTOs);

        String documentString = logbackDocument.toXML();
        InputStream documentIS = new ByteArrayInputStream(documentString.getBytes(Charsets.UTF_8));
        lc.reset();
        try {
            configurator.doConfigure(documentIS);
        } catch (JoranException e) {
            throw new RuntimeException(e);
        }
        this.runtimeBean = runtimeBean;
    }

    @VisibleForTesting
    public Document getLogbackDocument() {
        return logbackDocument;
    }

    static Document createLogbackDocument(Collection<Element> appenderElements, Collection<LoggerTO> loggerTOs) {

        Element configuration = new Element("configuration");
        for (Element appenderElement : appenderElements) {
            checkArgument("appender".equals(appenderElement.getLocalName()), "Wrong node name, expected appender, got " +
                    appenderElement);
            configuration.appendChild(appenderElement);
        }
        for (LoggerTO loggerTO : loggerTOs) {
            configuration.appendChild(fromLoggerTO(loggerTO));
        }
        return new Document(configuration);
    }

    private static Element fromLoggerTO(LoggerTO loggerTO) {
        Element element;
        if (org.slf4j.Logger.ROOT_LOGGER_NAME.equals(loggerTO.getLoggerName())) {
            element = new Element("root");
        } else {
            element = new Element("logger");
            element.addAttribute(new Attribute("name", loggerTO.getLoggerName()));
        }
        element.addAttribute(new Attribute("level", loggerTO.getLevel()));
        if (loggerTO.getAdditivity() != null) {
            element.addAttribute(new Attribute("additivity", loggerTO.getAdditivity().toString()));
        }

        if (loggerTO.getAppenders() != null) {
            for (String appenderName : loggerTO.getAppenders()) {
                Element appenderRef = new Element("appender-ref");
                element.appendChild(appenderRef);
                appenderRef.addAttribute(new Attribute("ref", appenderName));
            }
        }
        return element;
    }

    @Override
    public void close() {
        runtimeBean.close();
    }
}
