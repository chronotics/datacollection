package org.chronotics.datacollection.collector;

import org.chronotics.datacollection.model.FileInfo;
import org.chronotics.datacollection.model.FolderInfo;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface Agent {
    public boolean connect();

    public boolean isConnected();

    public Map<String, FolderInfo> getSubFolderInfo(String path);

    public long[] getSubFileInfo(Map<String, FileInfo> fileInfoMap,
                                 String path,
                                 long[] sizeAndNumFiles);

    public List<FileInfo> listFiles(String path);

    public Boolean downLoadFile(FileInfo fileInfo, String downFilePath,Integer fileType);

    public void disconnect();
}
