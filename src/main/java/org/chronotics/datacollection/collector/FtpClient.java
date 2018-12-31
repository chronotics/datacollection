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

    /**
     * @param ip              FTP server Ip
     * @param port            FTP server port
     * @param user            FTP user Id
     * @param pwd             FTP user password
     * @param commandListener true : commandListener mode (write every ftp command on console)
     */
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

    /**
     * make connection with FTP server
     *
     * @return
     */
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

    /**
     * check the connection is valid
     *
     * @return
     */
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
     * set the connection timeout of FTP Client (set before connection)
     *
     * @param connectTimeOut
     */
    public void setConnectTimeOut(int connectTimeOut) {
        ftp.setConnectTimeout(connectTimeOut);
    }

    /**
     * set the encoding type of FTP Client Connection (set before connection)
     *
     * @param encoding
     */
    public void setEncoding(String encoding) {
        ftp.setControlEncoding(encoding);
    }


    /**
     * set the file type of FTP Server (set after connection)
     *
     * @param fileType ASCII(0), EBCDIC(1), BINARY(2), LOCAL(3)
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

    /**
     * get sub-folders information one depth under the path(parameter)
     *
     * @param path target folder path
     * @return folder information map. key will be path + folderName
     */
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

            } else if (f.isDirectory()) {
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

    /**
     * get sub-files information under the path.
     * It contains all files in the sub-folders under the path.
     *
     * @param fileInfoMap     input is empty map. during the method, scanned file will be added.
     *                        key will be path + filename
     * @param path            target folder path
     * @param sizeAndNumFiles for getting the size and file counts of getSubFolderInfo()
     *                        long[0] : file size, long[1] : fileCount.
     * @return
     */
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
                if (sizeAndNumFiles != null) {
                    sizeAndNumFiles[0] += f.getSize();
                    sizeAndNumFiles[1] += 1;
                }
            } else {
                getSubFileInfo(fileInfoMap,
                        String.format("%s/%s", path, f.getName()),
                        sizeAndNumFiles);
            }
        }
        return sizeAndNumFiles;
    }

    /**
     * get the file list under the path (not all depth, only one depth)
     *
     * @param path target folder path
     * @return list of scanned file information
     */
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

    /**
     * download one file
     *
     * @param fileInfo
     * @param downFilePath
     * @param fileType     ASCII(0), EBCDIC(1), BINARY(2), LOCAL(3)
     * @return downloaded file information (as File instance)
     */
    public Boolean downLoadFile(FileInfo fileInfo, String downFilePath, Integer fileType) {
        if (!isConnected()) {
            boolean connection = connect();
            if (!connection) {
                return null;
            }
        }
        if (fileType != null) {
            setFileType(fileType);
        }
        String target = String.format("%s/%s", fileInfo.getPath(), fileInfo.getName());
        File dir = new File(downFilePath);
        File f = new File(String.format("%s/%s", downFilePath, fileInfo.getName()));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        boolean result=false;
        try {
            OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
            result = ftp.retrieveFile(target, fos);
            if (!result || !FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                logger.error("download fail");
                // The ftpClient is an inconsistent state. Must close the stream
                // which in turn will logout and disconnect from FTP server
            } else {
                logger.info(fileInfo.getName() + ": download success!");
            }
            fos.close();
        } catch (FileNotFoundException fn) {
            logger.error("Cannot set the downloadLocation ");
        } catch (IOException ex) {
            logger.error("Cannot download the file from FTP Server ");
        }

        disconnect();
        return result;
    }

    /**
     * disconnect the FTP Client
     */
    public void disconnect() {
        try {
            ftp.disconnect();
        } catch (IOException e) {
            logger.error("there is an error in disconnecting");
        }
    }

    /**
     * get FTP Server reply
     *
     * @param ftp
     * @return
     */
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

