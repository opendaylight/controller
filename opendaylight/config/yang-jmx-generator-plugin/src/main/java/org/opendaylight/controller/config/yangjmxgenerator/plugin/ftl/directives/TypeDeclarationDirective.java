/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.TypeDeclaration;

import com.google.common.base.Preconditions;

import freemarker.core.Environment;
import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Add type declaration to freemarker template.
 */
public class TypeDeclarationDirective implements TemplateDirectiveModel {

    private static final String OBJECT = "object";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);
        Preconditions.checkNotNull(object, "Null type declaration");

        object = ((StringModel) object).getWrappedObject();
        Preconditions.checkArgument(
                object instanceof TypeDeclaration,
                "Type declaration should be instance of "
                        + TypeDeclaration.class + " but was "
                        + object.getClass());

        TypeDeclaration type = (TypeDeclaration) object;

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder("public ");
        if (type.isAbstract())
            build.append("abstract ");
        if (type.isFinal())
            build.append("final ");
        build.append(type.getType() + " ");
        build.append(type.getName() + " ");

        generateExtendOrImplement(build, "extends", type.getExtended());

        generateExtendOrImplement(build, "implements", type.getImplemented());

        build.append(System.lineSeparator());
        out.write(build.toString().toCharArray());
    }

    private void generateExtendOrImplement(StringBuilder build, String prefix,
            Collection<String> elements) {
        if (elements.isEmpty())
            return;

        build.append(prefix + " ");

        for (String extended : elements) {
            build.append(extended);
            build.append(", ");
        }
        build.deleteCharAt(build.length() - 1);
        build.deleteCharAt(build.length() - 1);
    }

}
