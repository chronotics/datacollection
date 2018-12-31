package org.chronotics.datacollection.collector;


import org.chronotics.datacollection.model.FileInfo;
import org.chronotics.datacollection.model.FileInfo.FILE_STATUS;
import org.chronotics.datacollection.parser.Parser;
import org.chronotics.pandora.java.log.Logger;
import org.chronotics.pandora.java.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Workflow {
    static Logger logger = LoggerFactory.getLogger(FtpClient.class);

    private ConcurrentHashMap<String, FileInfo> fileInfoMap;
    private Map<Agent, FileInfo> agentMap;  //  Agent, FileInfo for downloading

    private String path;    // scanning start directory

    public Workflow(String path) {
        this.path = path;
    }

    /**
     * get scanned list in fileInfoMap. get all files in specific status.
     * if fileStatus is null, get the total fileInfoMap
     *
     * @param fileStatus FILE_STATUS like FILE_STATUS.ERROR,FILE_STATUS.IMPORTED,FILE_STATUS.DOWNLOADED
     * @return Key will be filepath + filename
     */
    public Map<String, FileInfo> getStatusList(FILE_STATUS fileStatus) {
        if (fileInfoMap == null) {
            logger.error("the first scan is not started");
            return null;
        }
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
     * Execute scanner Thread
     *
     * @param agent
     * @return
     */
    public Future scan(Agent agent) {
        if (scanner == null) {
            scanner = new Scanner(agent);
        }
        Future<Object> future = Executors.newSingleThreadExecutor().submit(scanner);
        return future;
    }

    /**
     * Execute download thread
     *
     * @param agent
     * @param targetFile
     * @param downloadPath
     * @param fileType     ASCII(0), EBCDIC(1), BINARY(2), LOCAL(3)
     */
    public Future download(Agent agent, FileInfo targetFile, String downloadPath, Integer fileType) {
        Downloader downloader = new Downloader(targetFile, agent, downloadPath, fileType);
        Future future = Executors.newSingleThreadExecutor().submit(downloader);
        return future;
    }
//    public void download(Agent agent, FileInfo targetFile, String downloadPath, Integer fileType) {
//        Downloader downloader = new Downloader(targetFile, agent, downloadPath, fileType);
//        Future future = Executors.newSingleThreadExecutor().submit(downloader);
//
//    }

    /**
     * Execute download thread, and running parsing after downloading end
     *
     * @param targetFile
     * @param agent
     * @param downloadPath
     * @param fileType     ASCII(0), EBCDIC(1), BINARY(2), LOCAL(3)
     * @param parser
     */
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
            agent.getSubFileInfo(WorkFileInfoMap, path, null);

            // update fileInfoList
            Map<String, FileInfo> tmpMap = new HashMap<>(fileInfoMap);
            for (String key : WorkFileInfoMap.keySet()) {
                if (!tmpMap.containsKey(key)) {
                    fileInfoMap.put(key, WorkFileInfoMap.get(key));
                }
            }
//            System.out.println("fileInfoMap");
//            int k=0;
//            for(String key : fileInfoMap.keySet()){
//                System.out.println(fileInfoMap.get(key));
//                if(k==10){break;}
//                k++;
//            }
            return 0;
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
            Boolean downChk = agent.downLoadFile(targetFile, downloadPath, fileType);
            if (downChk) {
                targetFile.setStatus(FILE_STATUS.DOWNLOADED.toString());
            } else {
                targetFile.setStatus(FILE_STATUS.ERROR.toString());
            }
            if (parser != null) {
                targetFile.setStatus(FILE_STATUS.IMPORTING.toString());
                boolean parseChk = parser.parse(targetFile);
                if (parseChk) {
                    targetFile.setStatus(FILE_STATUS.IMPORTED.toString());
                } else {
                    targetFile.setStatus(FILE_STATUS.ERROR.toString());
                }
            }
            agentMap.remove(agent);
            return targetFile;
        }
    }

}
