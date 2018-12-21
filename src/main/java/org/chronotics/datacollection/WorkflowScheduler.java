package org.chronotics.datacollection;

import org.chronotics.datacollection.collector.FtpClient;
import org.chronotics.datacollection.collector.Workflow;
import org.chronotics.datacollection.model.FileInfo;
import org.chronotics.datacollection.model.FileInfo.FILE_STATUS;

import org.chronotics.pandora.java.log.Logger;
import org.chronotics.pandora.java.log.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowScheduler {
    static Logger logger = LoggerFactory.getLogger(FtpClient.class);
    private String ip = "106.249.235.178";
    private int port = 21;
    private String user = "smart";
    private String pwd = "smartwifi";
    private String path = "//01";
    private String downloadPath = "/home/sjl/projects/kitech/test";

    private Workflow workflow;
    private FtpClient scanFtpClient;


    /**
     *  running thread every "fixDelayString" millisecond
     *  if scanning is not finished, return the old-list(latest finished scanning output)
     */
//    @Scheduled(fixedDelayString = "1000")
    public void workflowScanner(){
        if (workflow == null) {
            workflow = new Workflow(path);
        }
        if (scanFtpClient == null) {
            scanFtpClient = new FtpClient(ip, port, user, pwd, false);
        }
        /**
         *  Scanning
         */
        Map<String, FileInfo> scanListMap = workflow.scan(scanFtpClient);
        if (scanListMap == null) {
            logger.info("scanning is not finished yet");
            return;
        }

        int i=1;
        for(String key : scanListMap.keySet()){
            System.out.println(scanListMap.get(key));
            if(i==10){break;}
            i++;
        }

    }
    @Scheduled(fixedDelayString = "1000")
    public void workflowFull() {
        if (workflow == null) {
            workflow = new Workflow(path);
        }
        if (scanFtpClient == null) {
            scanFtpClient = new FtpClient(ip, port, user, pwd, false);
        }
        /**
         *  Scanning
         */
        Map<String, FileInfo> scanListMap = workflow.scan(scanFtpClient);
        if (scanListMap == null) {
            logger.info("scanning is not finished yet");
            return;
        }
        /**
         *  Downloading
         *      - newly created FtpClient is needed
         */
        List<FileInfo> downloadList = downloadListSetting(scanListMap);
        for (FileInfo fileInfo : downloadList) {
            FtpClient downFtpClient = new FtpClient(ip, port, user, pwd, false);
            workflow.download(downFtpClient, fileInfo, downloadPath,2);
        }

        /**
         *  get the downloaded file list
         */
        List<FileInfo> downloadedList = workflow.getFileStatusList(FILE_STATUS.DOWNLOADED);
        System.out.println("downloaded List : ");
        for(FileInfo f : downloadedList){
            System.out.println(f);
        }

        System.out.println();
        System.out.println("scanList :");

        for (Map.Entry<String, FileInfo> entry : workflow.getScannedList(FILE_STATUS.DOWNLOADED).entrySet()) {
                System.out.println(entry.getValue());
        }
    }



    public List<FileInfo> downloadListSetting(Map<String, FileInfo> scanListMap) {
        List<FileInfo> downloadList = new ArrayList<>();
        System.out.println("downloadList : ");
        int i = 1;
        for (FileInfo fileInfo : scanListMap.values()) {
            if (!fileInfo.getStatus().equals(FILE_STATUS.DOWNLOADED.toString())
                    && !fileInfo.getStatus().equals(FILE_STATUS.DOWNLOADING.toString())) {
                downloadList.add(fileInfo);
                System.out.println(fileInfo);
                if (i == 10) {
                    break;
                }
                i++;
            }
        }
        return downloadList;
    }
}
