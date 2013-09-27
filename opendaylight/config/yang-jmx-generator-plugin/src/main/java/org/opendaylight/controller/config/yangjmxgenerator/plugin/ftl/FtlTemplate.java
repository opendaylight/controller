/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.io.File;
import java.util.List;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Method;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.TypeDeclaration;

public interface FtlTemplate {

    Header getHeader();

    String getPackageName();

    String getJavadoc();

    public List<Annotation> getAnnotations();

    TypeDeclaration getTypeDeclaration();

    public String getFullyQualifiedName();

    public List<Field> getFields();

    List<? extends Method> getMethods();

    /**
     * @return relative path to file to be created.
     */
    public File getRelativeFile();

    /**
     *
     * @return ftl template location
     */
    public String getFtlTempleteLocation();
}
