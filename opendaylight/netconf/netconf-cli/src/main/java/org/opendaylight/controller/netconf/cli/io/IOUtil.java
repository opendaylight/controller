package org.opendaylight.controller.netconf.cli.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

import com.google.common.base.Preconditions;

public class IOUtil {

    public static final String SKIP = "skip";
    public static final String PROMPT_PREFIX = "netconf";
    public static final String PROMPT_SUFIX = ">";
    public static final String PROMPT = PROMPT_PREFIX + PROMPT_SUFIX;
    public static final String PATH_SEPARATOR = "/";

    private IOUtil() {
    }

    public static boolean isQName(final String qName) {
        final Matcher matcher = patternNew.matcher(qName);
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

    public static String qNameToKeyString(final QName qName, final String moduleName) {
        return String.format("%s(%s)", qName.getLocalName(), moduleName);
    }

    // TODO test and check regex + review format of string for QName
    final static Pattern patternNew = Pattern.compile("([^\\)]+)\\(([^\\)]+)\\)");

    public static QName qNameFromKeyString(final String qName, final Map<String, QName> mappedModules) {
        final Matcher matcher = patternNew.matcher(qName);
        Preconditions.checkState(matcher.matches(), "QName in wrong format: %s should be: %s", qName, patternNew);
        final QName base = mappedModules.get(matcher.group(2));
        Preconditions.checkNotNull(base, "Module %s cannot be found", matcher.group(2));
        return QName.create(base, matcher.group(1));
    }
    
    public static boolean isSkipInput(final String rawValue) {
        return rawValue.equals(SKIP);
    }
    
}
