/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.io.FileUtils;
import org.opendaylight.controller.filtervalve.cors.jaxb.Host;
import org.opendaylight.controller.filtervalve.cors.jaxb.Parser;
import org.opendaylight.controller.filtervalve.cors.model.FilterProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Valve that allows adding filters per context. Each context can have its own filter definitions.
 * Main purpose is to allow externalizing security filters from application bundles to a single
 * file per OSGi distribution.
 */
public class FilterValve extends ValveBase {
    private static final Logger logger = LoggerFactory.getLogger(FilterValve.class);
    private FilterProcessor filterProcessor;

    public void invoke(final Request request, final Response response) throws IOException, ServletException {
        if (filterProcessor == null) {
            throw new IllegalStateException("Initialization error");
        }

        FilterChain nextValveFilterChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
                boolean reqEquals = Objects.equals(request, req);
                boolean respEquals = Objects.equals(response, resp);
                if (reqEquals == false || respEquals == false) {
                    logger.error("Illegal change was detected by valve - request {} or " +
                            "response {} was replaced by a filter. This is not supported by this valve",
                            reqEquals, respEquals);
                    throw new IllegalStateException("Request or response was replaced in a filter");
                }
                getNext().invoke(request, response);
            }
        };
        filterProcessor.process(request, response, nextValveFilterChain);
    }

    /**
     * Called by Tomcat when configurationFile attribute is set.
     * @param fileName path to xml file containing valve configuration
     * @throws Exception
     */
    public void setConfigurationFile(String fileName) throws Exception {
        File configurationFile = new File(fileName);
        if (configurationFile.exists() == false || configurationFile.canRead() == false) {
            throw new IllegalArgumentException(
                    "Cannot read 'configurationFile' of this valve defined in tomcat-server.xml: " + fileName);
        }
        String xmlContent;
        try {
            xmlContent = FileUtils.readFileToString(configurationFile);
        } catch (IOException e) {
            logger.error("Cannot read {} of this valve defined in tomcat-server.xml", fileName, e);
            throw new IllegalStateException("Cannot read " + fileName, e);
        }
        Host host;
        try {
            host = Parser.parse(xmlContent, fileName);
        } catch (Exception e) {
            logger.error("Cannot parse {} of this valve defined in tomcat-server.xml", fileName, e);
            throw new IllegalStateException("Error while parsing " + fileName, e);
        }
        filterProcessor = new FilterProcessor(host);
    }

    /**
     * @see org.apache.catalina.valves.ValveBase#getInfo()
     */
    public String getInfo() {
        return getClass() + "/1.0";
    }
}
