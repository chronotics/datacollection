package org.chronotics.datacollection.collector;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.chronotics.datacollection.model.FileInfo;
import org.chronotics.datacollection.model.FileInfo.FILE_STATUS;

import org.chronotics.datacollection.model.FolderInfo;
import org.chronotics.pandora.java.log.Logger;
import org.chronotics.pandora.java.log.LoggerFactory;

import java.io.*;
import java.util.*;

public class FtpClient implements Agent {
    static Logger logger = LoggerFactory.getLogger(FtpClient.class);

    private boolean commandListener;

    private String ip;
    private int port;
    private String user;
    private String pwd;
    private FTPClient ftp;

    public FtpClient(String ip,
                     int port,
                     String user,
                     String pwd,
                     boolean commandListener) {
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
        ftp = new FTPClient();
        this.commandListener = commandListener;
    }

    public boolean connect() {
        try {
            ftp.connect(ip, port);
            ftp.login(user, pwd);
            ftp.enterLocalPassiveMode();
            if (commandListener) {
                ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            }
        } catch (IOException ex) {
            logger.error("cannot login..." + showServerReply(ftp));
            return false;
        }

        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {  //when connection failed
            logger.error("FTP server Connection Failed...");
            return false;
        } else {//connection succeed
            if (ftp.isConnected()) {
                logger.info("FTP Server Connection Success : " + showServerReply(ftp));
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isConnected() {
        boolean chk = false;
        try {
            chk = ftp.sendNoOp();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return chk;
    }

    /**
     * set before connection
     * @param connectTimeOut
     */
    public void setConnectTimeOut(int connectTimeOut) {
        ftp.setConnectTimeout(connectTimeOut);
    }

    /**
     * set before connection
     * @param encoding
     */
    public void setEncoding(String encoding) {
        ftp.setControlEncoding(encoding);
    }


    /**
     * @param fileType ASCII(0), EBCDIC(1), BINARY(2), LOCAL(3)
     * Connect first before setting file type
     */
    public void setFileType(int fileType) {
        //default type is ASCII(0)
        if (fileType > 3 || fileType < 0) {
            logger.error("fileType should be 0~3");
            return;
        }
        if (!isConnected()) {
            logger.error("Connect first before setting file type");
            return;
        }
        boolean setFile = false;
        try {
            setFile = ftp.setFileType(fileType);
        } catch (IOException e) {
            logger.error("cannot set the file type");
        }
        if (!setFile) {
            logger.error("fail to change the file type");
        }
    }

    public Map<String, FolderInfo> getSubFolderInfo(String path) {
        if (!isConnected()) {
            boolean connection = connect();
            if (!connection) {
                return null;
            }
        }
        FTPFile[] fileList = null;

        try {
            fileList = ftp.listFiles(path);
        } catch (IOException e) {
            logger.error("cannot get the file list from FTP server. path : {}", path);
        }

        Map<String, FolderInfo> folderInfoMap =
                new HashMap<>();

        for (FTPFile f : fileList) {
            if (f.isFile()) {

            } else {
                String targetPath = String.format("%s/%s", path, f.getName());
                long[] sizeAndNumFiles = {0, 0};
                getSubFileInfo(
                        null,
                        targetPath,
                        sizeAndNumFiles);
                FolderInfo folderInfo =
                        new FolderInfo(
                                path,
                                f.getName(),
                                sizeAndNumFiles[0],
                                sizeAndNumFiles[1]);
                folderInfoMap.put(targetPath, folderInfo);
            }
        }
        return folderInfoMap;
    }

    public long[] getSubFileInfo(Map<String, FileInfo> fileInfoMap,
                                 String path,
                                 long[] sizeAndNumFiles) {
        if (!isConnected()) {
            boolean connection = connect();
            if (!connection) {
                return null;
            }
        }
        FTPFile[] fileList = null;
        try {
            fileList = ftp.listFiles(path);
        } catch (IOException e) {
            logger.error("cannot get the file list from FTP server. path : {}", path);
        }

        for (FTPFile f : fileList) {
            if (f.isFile()) {
                f.getName();
                f.getSize();

                if (fileInfoMap != null) {
                    String targetPath = String.format("%s/%s", path, f.getName());
                    FileInfo fileInfo =
                            new FileInfo(path,
                                    f.getName(),
                                    f.getSize(),
                                    FILE_STATUS.CREATING);
                    fileInfoMap.put(targetPath, fileInfo);
                }
                sizeAndNumFiles[0] += f.getSize();
                sizeAndNumFiles[1] += 1;
            } else {
                getSubFileInfo(fileInfoMap,
                        String.format("%s/%s", path, f.getName()),
                        sizeAndNumFiles);
            }
        }
        return sizeAndNumFiles;
    }

    public List<FileInfo> listFiles(String path) {
        if (!isConnected()) {
            boolean connection = connect();
            if (!connection) {
                return null;
            }
        }
        List<FileInfo> fileInfoList = new ArrayList<>();
        try {
            FTPFile[] ftpFiles = ftp.listFiles(path);
            for (FTPFile f : ftpFiles) {
                FileInfo fileInfo =
                        new FileInfo(path,
                                f.getName(),
                                f.getSize(),
                                FILE_STATUS.CREATING);
                fileInfoList.add(fileInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileInfoList;
    }

    public File downLoadFile(FileInfo fileInfo, String downFilePath,Integer fileType) {
        if (!isConnected()) {
            boolean connection = connect();
            if (!connection) {
                return null;
            }
        }
        if(fileType!=null){
            setFileType(fileType);
        }
        String target = String.format("%s/%s", fileInfo.getPath(), fileInfo.getName());
        File dir = new File(downFilePath);
        File f = new File(String.format("%s/%s", downFilePath, fileInfo.getName()));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
            boolean result = ftp.retrieveFile(target, fos);
            if (!result || !FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                logger.error("download fail");
                // The ftpClient is an inconsistent state. Must close the stream
                // which in turn will logout and disconnect from FTP server
                fos.close();
            } else {
                fileInfo.setStatus(FILE_STATUS.DOWNLOADED.toString());
                logger.info(fileInfo.getName() + ": download success!");
            }
            fos.close();
        } catch (FileNotFoundException fn) {
            logger.error("Cannot set the downloadLocation ");
        } catch (IOException ex) {
            logger.error("Cannot download the file from FTP Server ");
        }
        disconnect();
        return f;
    }

    public void disconnect() {
        try {
            ftp.disconnect();
        } catch (IOException e) {
            logger.error("there is an error in disconnecting");
        }
    }

    private static String showServerReply(FTPClient ftp) {
        String[] replies = ftp.getReplyStrings();
        String ftpReply = "FTP Server : ";
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                ftpReply += aReply + " ";
            }
        }
        return ftpReply;
    }

}

