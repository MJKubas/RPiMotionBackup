package com.company;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {
    public static void moveFile(String cameraName) throws IOException {
        File directory = new File(cameraName);
        String workingDir = System.getProperty("user.dir");
        if(!directory.exists())
        {
            directory.mkdir();
        }
        File f = new File(workingDir);
        String[] files = f.list();
        if(files != null){
            for (String file:files) {
                if(file.contains("_" + cameraName + ".avi")){
                    Files.move(Paths.get(file), Paths.get(cameraName + "/" + file));
                }
            }
        }
    }

    public static void loadLibrary() {
        try {
            System.load(System.getProperty("user.dir")+"/libopencv_java440.so"); //FOR LINUX
            //System.load(System.getProperty("user.dir")+"/opencv_videoio_ffmpeg440_64.dll"); //
            //System.load(System.getProperty("user.dir")+"/opencv_java440.dll");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load opencv native library", e);
        }
    }
}
