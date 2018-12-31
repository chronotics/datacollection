package org.chronotics.datacollection.parser;

import org.chronotics.datacollection.model.FileInfo;

import java.io.File;

public interface Parser {
    public void parse();
    public boolean parse(File file);
    public boolean parse(FileInfo fileInfo);

}
