package org.openflow.example.cli;

import java.io.PrintStream;

/**
 * Very basic command line interface
 * 
 * (really should be something in java.* for this; only implementing this to
 * remove external dependencies)
 * 
 * Modeled after org.apache.common.cli .
 * 
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 * 
 */

public class SimpleCLI {

    private static final String NAME_WIDTH = "-15";
    private static final String VALUE_WIDTH = "-20";
    private static final String FORMAT_STRING = "%1$" + NAME_WIDTH + "s%2$"
            + VALUE_WIDTH + "s%3$s\n";
    Options options;

    int optind;

    /**
     * Need to use SimpleCLI.parse() instead
     * 
     * @param options
     */

    private SimpleCLI(Options options) {
        this.options = options;
    }

    /**
     * @return the index of the last parsed option
     * 
     *         Useful for finding options that don't start with "-" or "--"
     */
    public int getOptind() {
        return optind;
    }

    /**
     * @param optind
     *            the optind to set
     */
    public void setOptind(int optind) {
        this.optind = optind;
    }

    public boolean hasOption(String shortName) {
        Option option = this.options.getOption(shortName);
        if (option == null)
            return false;
        return option.specified;
    }

    public String getOptionValue(String shortName) {
        Option option = this.options.getOption(shortName);
        if (option == null)
            return null;
        if (!option.specified)
            return option.defaultVal.toString();
        else
            return option.val;
    }

    public static SimpleCLI parse(Options options, String[] args)
            throws ParseException {
        SimpleCLI simpleCLI = new SimpleCLI(options);
        int i;
        for (i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-"))
                break; // not a short or long option
            String optName = args[i].replaceFirst("^-*", ""); // remove leading
            // "--"
            Option option;
            if (args[i].startsWith("--"))
                option = options.getOptionByLongName(optName);
            else
                option = options.getOption(optName);
            if (option == null)
                throw new ParseException("unknown option: " + optName);
            option.specified = true;
            if (option.needsArg()) {
                if ((i + 1) >= args.length)
                    throw new ParseException("option " + optName
                            + " requires an argument:: " + option.comment);
                option.val = args[i + 1];
                i++; // skip next element; we've parsed it
            }
        }
        simpleCLI.setOptind(i);
        return simpleCLI;
    }

    public static void printHelp(String canonicalName, Options options) {
        printHelp(canonicalName, options, System.err);
    }

    private static void printHelp(String helpString, Options options,
            PrintStream err) {
        err.println(helpString);
        err.format(FORMAT_STRING, "\toption", "type [default]", "usage");
        for (Option option : options.getOptions()) {
            String msg = "\t";
            if (option.shortOpt != null)
                msg += "-" + option.shortOpt;
            if (option.longOpt != null) {
                if (!msg.equals("\t"))
                    msg += "|";
                msg += "--" + option.longOpt;
            }
            String val = "";
            if (option.defaultVal != null)
                val += option.defaultVal.getClass().getSimpleName() + " ["
                        + option.defaultVal.toString() + "]";
            String comment;
            if (option.comment != null)
                comment = option.comment;
            else
                comment = "";

            err.format(FORMAT_STRING, msg, val, comment);
        }
        err.println(""); // print blank line at the end, to look pretty
    }

}
