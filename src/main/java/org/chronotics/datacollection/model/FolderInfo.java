package org.chronotics.datacollection.model;

public class FolderInfo {
    private String id;
    private String path; // path of parent folder
    private String name; // folder name
    private long size;
    private long numOfSubFiles;

    public FolderInfo(
            String path,
            String name,
            long size,
            long numOfSubFiles
    ) {
        setPath(path);
        setName(name);
        setSize(size);
        setNumOfSubFiles(numOfSubFiles);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getNumOfSubFiles() {
        return numOfSubFiles;
    }

    public void setNumOfSubFiles(long numOfSubFiles) {
        this.numOfSubFiles = numOfSubFiles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    @Override
    public String toString() {
        return "FolderInfo{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", numOfSubFiles=" + numOfSubFiles +
                '}';
    }

}
