package org.openflow.example.cli;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Very basic CLI options listing
 * 
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 * 
 */

public class Options {
    Map<String, Option> shortOptionsMap;
    Map<String, Option> longOptionsMap;

    public Options() {
        this.shortOptionsMap = new HashMap<String, Option>();
        this.longOptionsMap = new HashMap<String, Option>();
    }

    public static Options make(Option opts[]) {
        Options options = new Options();
        for (int i = 0; i < opts.length; i++)
            options.addOption(opts[i]);
        return options;
    }

    private void addOption(Option option) {
        if (option.shortOpt != null)
            this.shortOptionsMap.put(option.shortOpt, option);
        if (option.longOpt != null)
            this.longOptionsMap.put(option.longOpt, option);
    }

    protected void addOption(String shortName, String longName, Object o,
            String comment) {
        Option option = new Option(shortName, longName, o, comment);
        addOption(option);
    }

    public void addOption(String shortName, String longName, boolean b,
            String comment) {
        this.addOption(shortName, longName, Boolean.valueOf(b), comment);
    }

    public void addOption(String shortName, String longName, int i,
            String comment) {
        this.addOption(shortName, longName, Integer.valueOf(i), comment);
    }

    public Option getOption(String shortName) {
        return this.shortOptionsMap.get(shortName);
    }

    public Option getOptionByLongName(String longName) {
        return this.longOptionsMap.get(longName);
    }

    public Collection<Option> getOptions() {
        return this.shortOptionsMap.values();
    }

    public void addOption(String shortName, String longName, String comment) {
        this.addOption(shortName, longName, null, comment);
    }

}
