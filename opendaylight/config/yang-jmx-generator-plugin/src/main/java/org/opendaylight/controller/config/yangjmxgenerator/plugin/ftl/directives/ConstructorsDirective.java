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
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.GeneralClassTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;

import com.google.common.collect.Lists;

import freemarker.core.Environment;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Add annotations to freemarker template.
 */
public class ConstructorsDirective implements TemplateDirectiveModel {

    private static final String OBJECT = "object";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);
        List<Constructor> constructors = Lists.newArrayList();

        if (object != null) {
            if (object instanceof SimpleSequence)
                constructors = ((SimpleSequence) object).toList();
            else if (object instanceof GeneralClassTemplate) {
                constructors = ((GeneralClassTemplate) object)
                        .getConstructors();
            } else
                throw new IllegalArgumentException(
                        "Object must be a SimpleSequence or instance of "
                                + GeneralClassTemplate.class + "but was "
                                + object.getClass());
        }

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder();
        for (Constructor constr : constructors) {
            build.append("    ");
            if (constr.isPublic())
                build.append("public ");
            build.append(constr.getTypeName() + " ");
            build.append("() {");
            build.append(System.lineSeparator());
            build.append("    ");
            build.append("    ");
            build.append(constr.getBody());
            build.append(System.lineSeparator());
            build.append("    ");
            build.append("}");
            build.append(System.lineSeparator());
            build.append(System.lineSeparator());
        }

        if (!constructors.isEmpty())
            out.write(build.toString().toCharArray());
    }

}
