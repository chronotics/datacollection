package org.chronotics.datacollection.parser;

import java.io.File;

public interface Parser {
    public void parse();
    public void parse(File file);
}
