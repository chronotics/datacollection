package datacollection;

import org.chronotics.datacollection.collector.FtpClient;
import org.chronotics.datacollection.model.FileInfo;
import org.chronotics.datacollection.model.FolderInfo;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FtpClientTestCase {
    private String ip = "106.249.235.178";
    private int port = 21;
    private String user = "smart";
    private String pwd = "smartwifi";
    private String path = "//01";
    private String downloadPath = "/home/sjl/projects/kitech/test";

    @Test
    public void ConnectionTest(){
        FtpClient ftpClient=new FtpClient(ip,port,user,pwd,false);
        ftpClient.connect();
        ftpClient.isConnected();

        ftpClient.disconnect();
    }

    @Test
    public void ConnectionSettingTest(){
        FtpClient ftpClient=new FtpClient(ip,port,user,pwd,true);
        ftpClient.setConnectTimeOut(1000);
        ftpClient.setEncoding("utf-8");
        ftpClient.connect();
        ftpClient.setFileType(2);

        ftpClient.disconnect();
    }

    @Test
    public void listFilesTest(){
        FtpClient ftpClient=new FtpClient(ip,port,user,pwd,false);
        ftpClient.setEncoding("utf-8");
        List<FileInfo> fileList=ftpClient.listFiles(path);

        System.out.println(fileList.size());
    }

    @Test
    public void getFolderInfoTest(){
        FtpClient ftpClient=new FtpClient(ip,port,user,pwd,false);
        ftpClient.setEncoding("utf-8");
        Map<String,FolderInfo> folderInfoMap=ftpClient.getSubFolderInfo("/");

        for(String key : folderInfoMap.keySet()){
            System.out.println(folderInfoMap.get(key));
        }
    }

    @Test
    public void getFileInfoTest(){
        FtpClient ftpClient=new FtpClient(ip,port,user,pwd,false);
        ftpClient.setEncoding("utf-8");
        Map<String,FileInfo> inputMap=new HashMap<>();
        long[] inputArr={0l,0l};
        inputArr=ftpClient.getSubFileInfo(inputMap,"/",inputArr);

        int i=1;
        for(String key : inputMap.keySet()){
            System.out.println(inputMap.get(key));
            if(i==20){break;}
            i++;
        }
    }
    @Test
    public void downloadTest(){
        FtpClient ftpClient=new FtpClient(ip,port,user,pwd,false);
        ftpClient.setEncoding("utf-8");
        List<FileInfo> fileList=ftpClient.listFiles(path);

        FtpClient downFtpClient=new FtpClient(ip,port,user,pwd,false);
        ftpClient.setEncoding("utf-8");
        ftpClient.downLoadFile(fileList.get(0),downloadPath,2);
    }
}
