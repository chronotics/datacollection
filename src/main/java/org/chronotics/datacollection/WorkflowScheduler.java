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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
public class WorkflowScheduler {
    static Logger logger = LoggerFactory.getLogger(FtpClient.class);
    private String ip = "106.249.235.178";
    private int port = 21;
    private String user = "smart";
    private String pwd = "smartwifi";
    private String path = "//12";
    private String downloadPath = "/home/sjl/projects/kitech/test";
    private Future scanFuture;
    private Workflow workflow;

   @Scheduled(fixedDelayString = "1000")
    public void workflowScanFuture() {
        /** Scanning */
        if (workflow == null) {
            workflow = new Workflow(path);
        }

        FtpClient scanFtpClient = new FtpClient(ip, port, user, pwd, false);
        if (scanFuture == null || scanFuture.isDone()) {
            scanFuture = workflow.scan(scanFtpClient);
            try {
                scanFuture.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            return;
        }
        Map<String, FileInfo> scanList = workflow.getStatusList(null); //all status
        if (scanList != null) {
            System.out.println(scanList.size());

            System.out.println("Scan List - ");
            int k = 0;
            for (String key : scanList.keySet()) {
                System.out.println(scanList.get(key));
                if (k == 10) {
                    break;
                }
                k++;
            }
        }

    }

//    @Scheduled(fixedDelayString = "1000")
    public void workflowDownFuture() {
        /** Scanning */
        if (workflow == null) {
            workflow = new Workflow(path);
        }
        FtpClient scanFtpClient = new FtpClient(ip, port, user, pwd, false);
        if (scanFuture == null || scanFuture.isDone()) {
            scanFuture = workflow.scan(scanFtpClient);
            try {
                scanFuture.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            return;
        }

        Map<String, FileInfo> scanList = workflow.getStatusList(FILE_STATUS.CREATING); //creating list

        if (scanList != null) {
            System.out.println(scanList.size());
            for (int i = 0; i < 10; i++) {
                System.out.println(scanList.get(i));
            }
        }

        /**
         * downloadList : user setting..
         */
        System.out.println("downloadList : ");
        List<FileInfo> downloadList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            downloadList.add(scanList.get(i));
            System.out.println(downloadList.get(i));
        }


        /** Downloading */
        List<Future> downFutureList = new ArrayList<>();
        for (FileInfo fileInfo : downloadList) {
            FtpClient downFtpClient = new FtpClient(ip, port, user, pwd, false);
            downFutureList.add(workflow.download(downFtpClient, fileInfo, downloadPath, 2));
        }

        int count = 0;
        while (count == downFutureList.size()) {
            count = 0;
            for (Future future : downFutureList) {
                if (future.isDone()) {
                    count++;
                }
            }
        }


        Map<String, FileInfo> downList = workflow.getStatusList(FILE_STATUS.DOWNLOADED); //downloadedList

    }
//
//    /**
//     * <Scheduler example>
//     * running thread every "fixDelayString" millisecond
//     * if scanning is not finished, return the old-list(latest finished scanning output)
//     */
//    @Scheduled(fixedDelayString = "1000")
//    public void workflowScanner() {
//        if (workflow == null) {
//            workflow = new Workflow(path);
//        }
//        if (scanFtpClient == null) {
//            scanFtpClient = new FtpClient(ip, port, user, pwd, false);
//        }
//        /**
//         *  Scanning
//         */
//    //    System.out.println("scanner is completed : "+workflow.scannerStatusforTest());
//        workflow.scan(scanFtpClient);
//
//        System.out.println("!~~~~~~~~~~~~~~~~~~~");
//    }
//
//    /**
//     * <Scheduler example>
//     * running thread every "fixDelayString" millisecond
//     * Containing scanning, downloading process
//     */
//    //   @Scheduled(fixedDelayString = "1000")
//    public void workflowFull() {
//        if (workflow == null) {
//            workflow = new Workflow(path);
//        }
//        if (scanFtpClient == null) {
//            scanFtpClient = new FtpClient(ip, port, user, pwd, false);
//        }
//        /**
//         *  Scanning
//         */
//        workflow.scan(scanFtpClient);
//        if (scanListMap == null) {
//            logger.info("scanning is not finished yet");
//            return;
//        }
//        /**
//         *  Downloading(newly created FtpClient is needed)
//         */
//        List<FileInfo> downloadList = downloadListSetting(scanListMap);
//        for (FileInfo fileInfo : downloadList) {
//            FtpClient downFtpClient = new FtpClient(ip, port, user, pwd, false);
//            workflow.download(downFtpClient, fileInfo, downloadPath, 2);
//        }
//
//        /**
//         *  get the downloaded file list
//         */
//        Map<String, FileInfo> downloadedList = workflow.getStatusList(FILE_STATUS.DOWNLOADED);
//        System.out.println("downloaded list ~~");
//        for (String key : downloadedList.keySet()) {
//            System.out.println(downloadedList.get(key));
//        }
//
//    }
//
//    public List<FileInfo> downloadListSetting(Map<String, FileInfo> scanListMap) {
//        List<FileInfo> downloadList = new ArrayList<>();
//        System.out.println("download list ~");
//        int i = 1;
//        for (FileInfo fileInfo : scanListMap.values()) {
//            if (!fileInfo.getStatus().equals(FILE_STATUS.DOWNLOADED.toString())
//                    && !fileInfo.getStatus().equals(FILE_STATUS.DOWNLOADING.toString())) {
//                downloadList.add(fileInfo);
//                System.out.println(fileInfo);
//                if (i == 20) {
//                    break;
//                }
//                i++;
//            }
//        }
//        return downloadList;
//    }
}
