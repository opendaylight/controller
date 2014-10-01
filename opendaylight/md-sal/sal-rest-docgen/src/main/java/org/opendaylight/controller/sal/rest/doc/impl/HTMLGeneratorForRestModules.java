/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.ws.rs.core.UriBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Generates HTML document which contains table of all YANG modules for which exists REST links for manipulating with
 * datastore.
 *
 * Output document contains table with tree columns (module name, revision, namespace). Table is sortable and also
 * searching is possible. For sorting and searching was used (see <a
 * href="http://cdn.datatables.net/1.10.2/js/jquery.dataTables.js">dataTables</a>)
 *
 */
public class HTMLGeneratorForRestModules {

    private static final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Generate HTML document according to specified uri and from specified module list
     *
     * @param uriBuilder
     * @param modulesWithRestLinks
     * @return
     */
    static public String generate(UriBuilder uriBuilder, final List<Module> modulesWithRestLinks) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<html>\n");
        writeHead(builder);
        builder.append("\t<body>\n");

        String baseUriValue = uriBuilder.path("../explorer/").build().toString();
        writeTableHeader(builder, baseUriValue);

        builder.append("\t\t<table id=\"table_id\" class=\"display\">\n");
        builder.append("\t\t\t<thead>\n");
        builder.append("\t\t\t<tr>\n");
        builder.append("\t\t\t\t<th>Module</th><th>Revision</th><th>Namespace</th>\n");
        builder.append("\t\t\t</tr>\n");
        builder.append("\t\t\t</thead>\n");
        builder.append("\t\t\t<tbody>\n");
        for (Module module : modulesWithRestLinks) {
            final String uriValue = baseUriValue + "?moduleName=" + module.getName() + "&revision="
                    + SIMPLE_DATE_FORMAT.format(module.getRevision());
            writeRow(uriValue, module, builder);
        }
        builder.append("\t\t\t<tbody>\n");
        builder.append("\t\t</table>");
        builder.append("\t</body>\n");
        builder.append("</html>");
        return builder.toString();
    }

    private static void writeHead(StringBuilder builder) {
        builder.append("\t<head><title>List of modules with REST links</title>\n");
        builder.append("<!-- DataTables CSS -->\n");
        builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"http://cdn.datatables.net/1.10.2/css/jquery.dataTables.css\">\n");

        builder.append("<!-- jQuery -->\n");
        builder.append("<script type=\"text/javascript\" charset=\"utf8\" src=\"http://code.jquery.com/jquery-1.10.2.min.js\"></script>\n");

        builder.append("<!-- DataTables -->\n");
        builder.append("<script type=\"text/javascript\" charset=\"utf8\" src=\"http://cdn.datatables.net/1.10.2/js/jquery.dataTables.js\"></script>\n");

        builder.append("<script type=\"text/javascript\">\n");
        builder.append("$(document).ready( function () {\n");
        builder.append("$('#table_id').dataTable({\n");
        builder.append("\"paging\":false } );\n");
        builder.append("} );\n");
        builder.append("</script>\n");
        builder.append("</head>\n");

    }

    private static void writeTableHeader(StringBuilder builder, String baseUri) {
        builder.append("<div>\n");
        builder.append("<table>\n");
        builder.append("<tr>\n");
        builder.append("<td><img src=\"" + baseUri + "images/logo_small.png\" /></td>\n");
        builder.append("<td><h1 width=\"100%\">OpenDaylight RestConf API Documentation</h1></td>\n");
        builder.append("</tr>\n");
        builder.append("</table>\n");
        builder.append("</div>\n");
    }

    private static void writeRow(String uri, Module module, StringBuilder builder) {
        builder.append("\t\t\t<tr>\n");
        writeCell("<a href=\"" + uri + "\">" + module.getName(), builder);
        writeCell(SIMPLE_DATE_FORMAT.format(module.getRevision()), builder);
        writeCell(module.getNamespace().toString(), builder);
    }

    private static void writeCell(String value, StringBuilder builder) {
        builder.append("\t\t\t\t<td>");
        builder.append(value);
        builder.append("</td>\n");
    }

}
