package com.company;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
	    Utils.loadLibrary();
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("google.com", 80));
        System.out.println(socket.getLocalAddress());

        if(new File("/home/pi/NodeServer/save").exists()){
            List<String> load = null;
            boolean loaded;
            do {
                loaded = true;
                try {
                    //load = Files.readAllLines(Paths.get("save"));
                    load = Files.readAllLines(Paths.get("/home/pi/NodeServer/save"));
                } catch (IOException e) {
                    e.printStackTrace();
                    loaded = false;
                }
                if(load != null){
                    for (String stream : load){
                        String[] split =  stream.split(">");
                        //StreamGet streamGet = new StreamGet(split[0], split[1]);
                        StreamGet streamGet = new StreamGet(split[1], "http:/"+socket.getLocalAddress()+":8080/"+split[1]+"/mjpg");
                        streamGet.runStream();
                        streamGet.motionDetect();
                    }
                }
            }while(!loaded);
        }
    }
}
