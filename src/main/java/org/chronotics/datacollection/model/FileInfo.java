package org.chronotics.datacollection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileInfo {

    public static enum FILE_STATUS {
        CREATING("CREATING"),
        DOWNLOADING("DOWNLOADING"),
        DOWNLOADED("DOWNLOADED"),
        IMPORTING("IMPORTING"),
        IMPORTED("IMPORTED"),
        ERROR("ERROR");

        private String name;

        FILE_STATUS(String str) {
            name = str;
        }

        String getName() {
            return name;
        }
    }

    private String id;
    private String path; // path of parent folder
    private String name;
    private Long size;
    private String status;

    public FileInfo() {
    }

    public FileInfo(String path,
                    String name,
                    long size,
                    FILE_STATUS status) {
        setPath(path);
        setName(name);
        setSize(size);
        setStatus(status.toString());
    }

    public FileInfo(String id, String path,
        String name,
        long size,
        String status) {
        setId(id);
        setPath(path);
        setName(name);
        setSize(size);
        setStatus(status);
    }

    public void updateStatus(FILE_STATUS status) {
        setStatus(status.toString());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "id='" + id + '\'' +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", status='" + status + '\'' +
                '}';
    }

}
