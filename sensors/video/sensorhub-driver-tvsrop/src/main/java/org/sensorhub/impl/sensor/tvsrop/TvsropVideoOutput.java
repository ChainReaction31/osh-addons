package org.sensorhub.impl.sensor.tvsrop;

import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.rtpcam.RTPVideoOutput;
import org.sensorhub.impl.sensor.videocam.VideoResolution;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class TvsropVideoOutput extends RTPVideoOutput<TvsropDriver> {
    volatile long lastFrameTime;
    Timer watchdog;


    protected TvsropVideoOutput(TvsropDriver driver)
    {
        super(driver);
    }


    public void init() throws SensorException
    {
        VideoResolution res = parentSensor.getConfiguration().video.getResolution();
        super.init(res.getWidth(), res.getHeight());
    }


    public void start() throws SensorException
    {
        TvsropConfig config = parentSensor.getConfiguration();
        super.start(config.video, config.rtsp, config.connection.connectTimeout);

        // start watchdog thread to detect disconnections
        final long maxFramePeriod = (long)(config.maxFrameDelay * 1000);
        lastFrameTime = Long.MAX_VALUE;
        TimerTask checkFrameTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (lastFrameTime < System.currentTimeMillis() - maxFramePeriod)
                {
                    parentSensor.getLogger().warn("No frame received in more than {}ms. Reconnecting...", maxFramePeriod);
                    parentSensor.connection.reconnect();
                    cancel();
                }
            }
        };

        watchdog = new Timer();
        watchdog.scheduleAtFixedRate(checkFrameTask, 0L, 10000L);
    }


    @Override
    public void onFrame(long timeStamp, int seqNum, ByteBuffer frameBuf, boolean packetLost)
    {
        super.onFrame(timeStamp, seqNum, frameBuf, packetLost);
        lastFrameTime = System.currentTimeMillis();
    }


    @Override
    public void stop()
    {
        super.stop();
        watchdog.cancel();
    }
}
