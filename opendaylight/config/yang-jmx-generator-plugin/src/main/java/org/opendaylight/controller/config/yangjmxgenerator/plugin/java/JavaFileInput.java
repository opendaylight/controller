/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

import com.google.common.base.Optional;
import java.util.List;

public interface JavaFileInput {

    FullyQualifiedName getFQN();

    Optional<String> getCopyright();

    Optional<String> getHeader();

    TypeName getType();

    Optional<String> getClassJavaDoc();

    List<String> getClassAnnotations();

    List<FullyQualifiedName> getExtends();

    List<FullyQualifiedName> getImplements();

    List<String> getBodyElements();

}
