package org.opendaylight.controller.config.yang.doc.generator.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class DocGeneratorUtil {

    public static final String H2 = "h2";
    public static final String TD = "td";
    public static final String TR = "tr";
    public static final String A = "a";

    static Document loadModuleTemplate(final String templateName) throws IOException {
        final URL url = Resources.getResource(templateName);
        final String template = Resources.toString(url, Charsets.UTF_8);
        return Jsoup.parse(template, Charsets.UTF_8.name());
    }

    static File writeToFile(final File outputBaseDir, final String fileName, final Document doc)
            throws FileNotFoundException, UnsupportedEncodingException {
        final File file = new File(outputBaseDir.getAbsolutePath(), fileName);
        try (PrintWriter writer = new PrintWriter(file, Charsets.UTF_8.name())) {
            writer.write(doc.outerHtml());
            writer.flush();
        }
        return file;
    }

    public static void addSummary(final Module module, final IdentitySchemaNode identity, final Element summaryTable,
            final String type) {
        if (summaryTable != null) {
            addColumnWithLinkToTableRow(summaryTable, identity.getQName().getLocalName(), type);
            addColumnToTableRow(summaryTable, (identity.getDescription() != null) ? identity.getDescription() : "");
        }
    }

    public static Element addColumnToTableRow(final Element tableRow, final String text) {
        return tableRow.appendElement(TD).text((text != null) ? text : "");
    }

    public static Element addColumnWithLinkToTableRow(final Element tableRow, final String text, final String type) {
        return tableRow.appendElement(TD).appendElement(A).attr("href", text + "_" + type + ".html").text(text);
    }

}
