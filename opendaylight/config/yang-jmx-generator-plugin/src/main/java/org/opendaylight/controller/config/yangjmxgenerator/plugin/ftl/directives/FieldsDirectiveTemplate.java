/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.directives;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;

import com.google.common.collect.Maps;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Add fields to freemarker template.
 */
public class FieldsDirectiveTemplate implements TemplateDirectiveModel {

    private static final String OBJECT = "object";

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {
        Object object = params.get(OBJECT);

        // TODO check type

        String templateStr = "    <#list fields as field>"
                + "private <#if field.final==true>final </#if> <#if field.static==true>static </#if>"
                + "${field.type} ${field.name}<#if field.definition??> = ${field.definition}</#if>;"
                + System.lineSeparator() + " </#list>";
        Template t = new Template("name", new StringReader(templateStr),
                new Configuration());

        try {
            Map<String, Object> map = Maps.newHashMap();
            map.put("fields", object);
            Writer out = env.getOut();
            t.process(map, out);
        } catch (TemplateException e) {
            throw new IllegalStateException(
                    "Template error while generating fields" + e);
        }
    }

}
