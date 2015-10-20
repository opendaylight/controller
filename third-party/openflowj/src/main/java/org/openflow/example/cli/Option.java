package org.openflow.example.cli;

public class Option {
    String shortOpt;
    String longOpt;
    Object defaultVal;
    String val; // current value of this option, string form
    boolean specified; // was this option found in the cmdline?
    String comment;

    /**
     * Option information storrage
     * 
     * @param shortOpt
     *            Short name for the option, e.g., "-p"
     * @param longOpt
     *            Long name for option, e.g., "--port"
     * @param defaultVal
     *            default value: "6633" or null if no default value
     * @param comment
     *            String to print to explain this option, e.g., a help message
     */
    public Option(String shortOpt, String longOpt, Object defaultVal,
            String comment) {
        super();
        this.shortOpt = shortOpt;
        this.longOpt = longOpt;
        this.defaultVal = defaultVal;
        this.comment = comment;
        this.specified = false;
    }

    public Option(String shortOpt, String longOpt, String comment) {
        this(shortOpt, longOpt, null, comment);
    }

    public boolean needsArg() {
        return this.defaultVal != null;
    }
}
