/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;

public interface Method {
    Optional<Modifier> getVisibility();

    List<Modifier> getModifiers();

    String getReturnType();

    String getName();

    List<Field> getParameters();

    String getJavadoc();

    List<Annotation> getAnnotations();

    List<String> getThrowsExceptions();

    Optional<String> getBody();
}
