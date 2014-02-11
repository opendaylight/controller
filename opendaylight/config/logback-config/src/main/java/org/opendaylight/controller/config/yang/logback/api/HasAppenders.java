/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.api;

import nu.xom.Element;

import java.util.Map;


/**
 * Interface each appender instance must implement as described in service interface contract
 */
public interface HasAppenders extends AutoCloseable {

    /**
     * Get dom element representing <appender /> tag for each appender indexed by name
     */
    Map<String, Element> getXmlRepresentationOfAppenders();
}
