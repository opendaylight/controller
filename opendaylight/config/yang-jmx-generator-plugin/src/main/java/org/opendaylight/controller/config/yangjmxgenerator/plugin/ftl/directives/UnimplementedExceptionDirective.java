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
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Add annotations to freemarker template.
 */
public class UnimplementedExceptionDirective implements TemplateDirectiveModel {

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        Writer out = env.getOut();
        StringBuilder build = new StringBuilder();
        build.append("        ");
        build.append("throw new "
                + UnsupportedOperationException.class.getCanonicalName()
                + "(\"Unimplemented stub method\");");
        build.append(System.lineSeparator());

        out.write(build.toString().toCharArray());
    }
}
