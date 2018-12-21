package org.chronotics.datacollection.collector;


import org.chronotics.datacollection.model.FileInfo;
import org.chronotics.datacollection.model.FileInfo.FILE_STATUS;
import org.chronotics.datacollection.parser.Parser;
import org.chronotics.pandora.java.log.Logger;
import org.chronotics.pandora.java.log.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Workflow {
    static Logger logger = LoggerFactory.getLogger(FtpClient.class);

    private ConcurrentHashMap<String, FileInfo> fileInfoMap;
    private Map<Agent, FileInfo> agentMap;  //  Agent, FileInfo for downloading
    private CopyOnWriteArrayList<FileInfo> fileStatusList;
//    private ConcurrentHashMap<String, Workflow> workflowMap;

    private String path;    // scanning start directory

    public Workflow(String path) {
        this.path = path;
    }

    /**
     *  get scanned list in fileInfoMap.
     * @param fileStatus
     * @return
     */
    public Map<String, FileInfo> getScannedList(FILE_STATUS fileStatus) {
        Map<String, FileInfo> tmp = new HashMap<>(fileInfoMap);
        if (fileStatus == null) {
            return tmp;
        } else {
            Map<String, FileInfo> filterMap = new HashMap<>();
            for (Map.Entry<String, FileInfo> entry : tmp.entrySet()) {
                if (entry.getValue().getStatus().equals(fileStatus.toString())) {
                    filterMap.put(entry.getKey(), entry.getValue());
                }
            }
            return filterMap;
        }
    }

    /**
     * get the list of specific status. the elements will be removed from fileStatusList after called.
     *
     * @param fileStatus : FILE_STATUS like FILE_STATUS.ERROR,FILE_STATUS.PARSED,FILE_STATUS.DOWNLOADED
     * @return list of FileInfo which status is parameter 'fileStatus'
     */
    public List<FileInfo> getFileStatusList(FILE_STATUS fileStatus) {
        if (fileStatusList == null || fileStatusList.size() == 0) {
            logger.error("downloading is not started");
            return null;
        }
        List<FileInfo> ret = new ArrayList<>();
        List<FileInfo> tmp = new ArrayList<>(fileStatusList);
        for (FileInfo fileInfo : tmp) {
            if (fileInfo.getStatus().equals(fileStatus.toString())) {
                ret.add(fileInfo);
                fileStatusList.remove(fileInfo);
            }
        }
        return ret;
    }

    /**
     * get the scanned list from FTP Server (based on path)
     *
     * @param agent
     * @return String will be path of FileInfo
     */
    public Map<String, FileInfo> scan(Agent agent) {
        if (scanner == null) {
            scanner = new Scanner(agent);
            Executors.newSingleThreadExecutor().submit(scanner);
        } else if (scanner.isCompleted()) {
            Executors.newSingleThreadExecutor().submit(scanner);
        }
        if (fileInfoMap != null) {
//            Map<String, FileInfo> scanListMap = new HashMap<>(fileInfoMap);
            return fileInfoMap;
        } else {
            logger.error("cannot get the scanList");
            return null;
        }
    }

    public void download(Agent agent, FileInfo targetFile, String downloadPath, Integer fileType) {
        Downloader downloader = new Downloader(targetFile, agent, downloadPath, fileType);
        Executors.newSingleThreadExecutor().submit(downloader);
    }

    public void downAndParse(FileInfo targetFile, Agent agent, String downloadPath, Integer fileType, Parser parser) {
        Downloader downloader = new Downloader(targetFile, agent, downloadPath, fileType, parser);
        Executors.newSingleThreadExecutor().submit(downloader);
    }



    /**
     * Scanner class is running the thread for scanning FTP Server folder.
     */
    private Scanner scanner = null;

    private class Scanner implements Callable<Object> {
        private Agent agent;
        private boolean isCompleted = false;

        public Scanner(Agent agent) {
            this.agent = agent;
        }

        @Override
        public Object call() {
            if (fileInfoMap == null) {
                fileInfoMap = new ConcurrentHashMap<>();
            }
            if (!agent.isConnected()) {
                logger.info("Scanner : agent is not connected..connecting");
                agent.connect();
            } else {
                throw new IllegalStateException("Cannot CONNECT to FTP Server");
            }

            // scan the specific folder
            Map<String, FileInfo> WorkFileInfoMap = new HashMap<>();
            long[] sizeAndCount = {0, 0};
            agent.getSubFileInfo(WorkFileInfoMap, path, sizeAndCount);
            // update fileInfoList
            Map<String, FileInfo> tmpMap = new HashMap<>(fileInfoMap);
            for (String filePath : WorkFileInfoMap.keySet()) {
                if (!tmpMap.containsKey(filePath)) {
                    fileInfoMap.put(filePath, WorkFileInfoMap.get(filePath));
                }
            }
            isCompleted = true;
            return null;
        }

        public boolean isCompleted() {
            return isCompleted;
        }
    }

    /**
     * Downloader class is running the Thread to download one file
     * newly created agent is needed.
     */
    private class Downloader implements Callable<Object> {
        private Agent agent;
        private FileInfo targetFile;
        private String downloadPath;
        private Parser parser;
        private Integer fileType;

        public Downloader(FileInfo targetFile, Agent agent, String downloadPath, Integer fileType) {
            this.agent = agent;
            this.targetFile = targetFile;
            this.downloadPath = downloadPath;
            this.fileType = fileType;
        }

        public Downloader(FileInfo targetFile, Agent agent, String downloadPath, Integer fileType, Parser parser) {
            this.agent = agent;
            this.targetFile = targetFile;
            this.downloadPath = downloadPath;
            this.fileType = fileType;
            this.parser = parser;

        }

        @Override
        public Object call() throws Exception {
            if (fileStatusList == null) {
                fileStatusList = new CopyOnWriteArrayList<>();
            }
            fileStatusList.add(targetFile);
            if (!agent.isConnected()) {
                logger.info("Downloader : agent is not connected..connecting");
                agent.connect();
            } else {
                throw new IllegalStateException("Cannot CONNECT to FTP Server");
            }
            if (agentMap == null) {
                agentMap = new ConcurrentHashMap<>();
            }

            agentMap.put(agent, targetFile);
            targetFile.setStatus(FILE_STATUS.DOWNLOADING.toString());
            File file = agent.downLoadFile(targetFile, downloadPath, fileType);
            if (parser != null && file != null) {
                targetFile.setStatus(FILE_STATUS.PARSED.toString());
                parser.parse(file); //set file status - IMPORTED in parser class
            }
            agentMap.remove(agent);
            return null;
        }
    }

}
