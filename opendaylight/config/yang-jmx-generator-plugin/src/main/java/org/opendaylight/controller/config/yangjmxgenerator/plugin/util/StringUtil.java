package org.opendaylight.controller.config.yangjmxgenerator.plugin.util;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtil {
    private static final Logger LOG = LoggerFactory.getLogger(StringUtil.class);

    /**
     * @param list   of strings to be joined by ','
     * @param prefix e.g. 'extends' or 'implements'
     */
    public static String prefixAndJoin(List<FullyQualifiedName> list, String prefix) {
        if (list.isEmpty()) {
            return "";
        }
        Joiner joiner = Joiner.on(",");
        return " " + prefix + " " + joiner.join(list);
    }

    public static String addAsterixAtEachLineStart(String input) {
        String s = Pattern.compile("^", Pattern.MULTILINE).matcher(input).replaceAll("* ");
        // remove trailing spaces
        s = Pattern.compile("\\s+$", Pattern.MULTILINE).matcher(s).replaceAll("");
        s = ensureEndsWithSingleNewLine(s);
        return s;
    }

    private static String ensureEndsWithSingleNewLine(String s) {
        // .split Only trailing empty strings are skipped.
        String[] split = s.split("\n");
        s = Joiner.on("\n").join(split);
        s = s + "\n";
        return s;
    }

    public static String writeComment(String input, boolean isJavadoc) {
        StringBuilder content = new StringBuilder();
        content.append("/*");
        if (isJavadoc) {
            content.append("*");
        }
        content.append("\n");

        content.append(addAsterixAtEachLineStart(input));
        content.append("*/\n");
        return content.toString();
    }


    public static Optional<String> loadCopyright() {
        /*
         * FIXME: BUG-980: this is a nice feature, but the copyright needs to come
         *        from the project being processed, not this one.
            try (InputStream in = StringUtil.class.getResourceAsStream("/copyright.txt")) {
                if (in != null) {
                    return Optional.of(IOUtils.toString(in));
                }
            } catch (IOException e) {
                LOG.warn("Cannot load copyright.txt", e);
            }

        */
        return Optional.absent();
    }

    public static String formatJavaSource(String input) {
        Iterable<String> split = Splitter.on("\n").trimResults().split(input);

        int basicIndent = 4;
        StringBuilder sb = new StringBuilder();
        int intends = 0, empty = 0;
        for (String line : split) {
            intends -= StringUtils.countMatches(line, "}");
            if (intends < 0) {
                intends = 0;
            }
            if (line.isEmpty() == false) {
                sb.append(Strings.repeat(" ", basicIndent * intends));
                sb.append(line);
                sb.append("\n");
                empty = 0;
            } else {
                empty++; // one empty line is allowed
                if (empty < 2) {
                    sb.append("\n");
                }
            }
            intends += StringUtils.countMatches(line, "{");
        }
        return ensureEndsWithSingleNewLine(sb.toString());
    }

}
