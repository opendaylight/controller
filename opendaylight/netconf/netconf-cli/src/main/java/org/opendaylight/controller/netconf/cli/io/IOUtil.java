package org.opendaylight.controller.netconf.cli.io;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.completer.Completer;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

import com.google.common.base.Preconditions;

public class IOUtil {

    // FIXME clean up
    // FIXME move error messages to exceptions

    public static final String ERROR_PREFIX = "ERROR: ";
    public static final String ER_INCORRECT_ARGUMENT = ERROR_PREFIX
            + "Incorrect argument was specified. Use TAB key to see proposals.";

    public static final String INFO_PREFIX = "INFO: ";
    public static final String INF_NO_KEY_IN_LIST = INFO_PREFIX + "List contains no keys.";
    public static final String INF_NO_COMPOSITE_NODE = INFO_PREFIX
            + "No data are specified. First specify data (e. g. via edit-config).";

    // TODO merge with constants in sal-netconf-connector
    public static URI NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    public static QName NETCONF_QNAME = QName.create(NETCONF_URI, null, "netconf");
    public static QName NETCONF_GET_CONFIG_QNAME = QName.create(NETCONF_QNAME, "get-config");
    public static QName NETCONF_EDIT_CONFIG_QNAME = QName.create(NETCONF_QNAME, "edit-config");

    public static final String SKIP = "skip";

    public static final String PROMPT_PREFIX = "netconf-client";
    public static final String PROMPT_SUFIX = ">";
    public static final String PROMPT = PROMPT_PREFIX + PROMPT_SUFIX;

    private static final String PARAMETER_SEPARATOR = " ";

    public static final String PATH_SEPARATOR = "/";

    public static final String EMPTY_STRING = "";
    public static final String INDENT = "  ";

    protected Completer completerForMenu = null;
    



    private IOUtil() {
    }

    public static String qNameToKeyString(final QName qName) {
        final Date date = qName.getRevision();
        return String.format("%s(%s,%tY/%tm/%td)", qName.getLocalName(), qName.getNamespace(), date, date, date);
    }

    // TODO test and check regex + review format of string for QName
    final static Pattern pattern = Pattern.compile("([^\\(]+)\\(([^,]+),([0-9]+)/([0-9]+)/([0-9]+)\\)");

    public static QName qNameFromKeyString(final String qName) {
        final Matcher matcher = pattern.matcher(qName);
        Preconditions.checkState(matcher.matches(), "Qname in wrong format: %s should be: %s", qName, pattern);
        return new QName(URI.create(matcher.group(2)), parseDate(matcher.group(3) + "-" + matcher.group(4) + "-"
                + matcher.group(5)), matcher.group(1));
    }

    public static boolean isQName(final String qName) {
        final Matcher matcher = pattern.matcher(qName);
        return matcher.matches();
    }

    public static Date parseDate(final String revision) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(revision);
        } catch (final ParseException e) {
            throw new IllegalArgumentException("Date not valid", e);
        }
    }

    public static String listType(final SchemaNode schemaNode) {
        if (schemaNode instanceof LeafListSchemaNode) {
            return "Leaf-list";
        } else if (schemaNode instanceof ListSchemaNode) {
            return "List";
        } else if (schemaNode instanceof LeafSchemaNode) {
            return "Leaf";
        }
        // FIXME throw exception on unexpected state, not null/emptyString
        return "";
    }

}
