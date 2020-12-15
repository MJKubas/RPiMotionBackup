package com.company;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamGet {
    StreamGet(String Name, String Path){
        this.cameraName = Name;
        this.streamPath = Path;
    }

    private final String cameraName;
    private final String streamPath;

    private final VideoCapture capture = new VideoCapture();
    private final Mat frame = new Mat();
    private int counterCapture = 0;
    int countdown = 750;

    //private ScheduledExecutorService detectRecordTimer;
    private final VideoWriter detectWriter = new VideoWriter();
    private final Size blurSize = new Size(8,8);
    private final Mat workImg = new Mat();
    private Mat movingAvgImg = null;
    private final Mat scaleImg = new Mat();
    private final Mat gray = new Mat();
    private final Mat diffImg = new Mat();
    private double motionPercent = 0.0;
    private String detectFileName;
    private boolean recordON = false;
    private Size frameSize;
    private LocalDateTime today = LocalDateTime.now();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public void runStream() throws Exception {

        this.capture.open(streamPath);

        if (this.capture.isOpened()) {
            System.out.println("Detecting started "+ streamPath + " " + cameraName);
            this.frameSize = new Size((int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) this.capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));

            Runnable frameGrabber = () -> {
                this.capture.read(frame);
                if(frame.width() == 0 && frame.height() == 0){
                    counterCapture++;
                    if(counterCapture > 10) {
                        this.capture.release();
                        this.capture.open(streamPath);
                        counterCapture = 0;
                    }
                }
            };
            ScheduledExecutorService captureTimer = Executors.newSingleThreadScheduledExecutor();
            captureTimer.scheduleAtFixedRate(frameGrabber, 0, 40, TimeUnit.MILLISECONDS);
        }
        else {
            // log the error
            throw new Exception("Cannot open the camera connection...");
        }
    }

    public void motionDetect(){
        final double totalPixels = frameSize.area();

        if(capture.isOpened()){
            Runnable detect = () -> {
                try{
                    // Generate work image by blurring
                    Imgproc.blur(frame, workImg, blurSize);
                    // Generate moving average image if needed
                    if (movingAvgImg == null) {
                        movingAvgImg = new Mat();
                        workImg.convertTo(movingAvgImg, CvType.CV_32F);
                    }
                    // Generate moving average image
                    Imgproc.accumulateWeighted(workImg, movingAvgImg, .03);
                    // Convert the scale of the moving average
                    Core.convertScaleAbs(movingAvgImg, scaleImg);
                    // Subtract the work image frame from the scaled image average
                    Core.absdiff(workImg, scaleImg, diffImg);
                    // Convert the image to grayscale
                    Imgproc.cvtColor(diffImg, gray, Imgproc.COLOR_BGR2GRAY);
                    // Convert to BW
                    Imgproc.threshold(gray, gray, 25, 255, Imgproc.THRESH_BINARY);
                    // Total number of changed motion pixels
                    motionPercent = 100.0 * Core.countNonZero(gray) / totalPixels;
                    // Detect if camera is adjusting and reset reference if more than 25%
                    if (motionPercent > 25.0) {
                        workImg.convertTo(movingAvgImg, CvType.CV_32F);
                    }
                    // Threshold trigger motion
                    if (motionPercent > 0.75 && !recordON) {
                        recordON = true;
                        countdown = 750;
//                        detectRecordTimer.schedule(() -> {
//                            recordON = false;
//                            detectWriter.release();
//                            try {
//                                Utils.moveFile(cameraName + "MD");
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }, 30, TimeUnit.SECONDS);
    
                        today = LocalDateTime.now();
                        detectFileName = today.format(formatter) + "_" + cameraName + "MD.avi";
                        detectWriter.open(detectFileName, VideoWriter.fourcc('H','2','6','4'), 25, frameSize, true); //FOR LINUX
                        //detectWriter.open(detectFileName, VideoWriter.fourcc('D','I','V','X'), 25, frameSize, true); //FOR WINDOWS
                    }
                    if(recordON){
                        if(countdown<=0){   //countdown = 750 (25fps * 30sec) workaround for killing upper task
                            recordON=false;
                            detectWriter.release();
                            try {
                                Utils.moveFile(cameraName + "MD");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        countdown--;
                        detectWriter.write(frame);
                    }
                }
                catch (Exception e){
                    System.err.println("Cannot perform motion detection..." + e);
                }
            };
            //this.detectRecordTimer = Executors.newSingleThreadScheduledExecutor();
            ScheduledExecutorService detectTimer = Executors.newSingleThreadScheduledExecutor();
            detectTimer.scheduleAtFixedRate(detect, 0, 40, TimeUnit.MILLISECONDS);
        }
    }
}
