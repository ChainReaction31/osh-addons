package org.sensorhub.impl.sensor.tvsrop;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.sensor.rtpcam.RTSPConfig;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.VideoResolution;
import org.sensorhub.impl.sensor.videocam.ptz.PtzConfig;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPreset;

public class TvsropConfig extends SensorConfig{
    @DisplayInfo(label="HTTP", desc="HTTP configuration")
    public HTTPConfig http = new HTTPConfig();

    @DisplayInfo(label="RTP/RTSP", desc="RTP/RTSP configuration (Remote host is obtained from HTTP configuration")
    public RTSPConfig rtsp = new RTSPConfig();

    @DisplayInfo(label="Connection Options")
    public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();

    @DisplayInfo(label="Video", desc="Video settings")
    public VideoConfig video = new VideoConfig();

    @DisplayInfo(label="PTZ", desc="Pan-Tilt-Zoom configuration")
    public PtzConfig ptz = new PtzConfig();

    @DisplayInfo(desc="Camera geographic position")
    public PositionConfig position = new PositionConfig();

    @DisplayInfo(desc="Maximum time without receiving frame before attempting to reconnect")
    public double maxFrameDelay = 1.0;

    @DisplayInfo(desc="Set to true if this OSH node has exclusive control of the camera")
    public boolean exclusiveControl = true;

    public class VideoConfig extends BasicVideoConfig
    {
        @DisplayInfo(desc="Resolution of video frames in pixels")
        public ResolutionEnum resolution;

        public VideoResolution getResolution()
        {
            return resolution;
        }
    }


    public enum ResolutionEnum implements VideoResolution
    {
        HD_720P("HD", 1280, 720),
        HD_1080P("Full HD", 1920, 1080);

        private String text;
        private int width, height;

        private ResolutionEnum(String text, int width, int height)
        {
            this.text = text;
            this.width = width;
            this.height = height;
        }

        public int getWidth() { return width; };
        public int getHeight() { return height; };
        public String toString() { return text + " (" + width + "x" + height + ")"; }
    };


    public TvsropConfig()
    {
        // default params for Dahua
        video.resolution = ResolutionEnum.HD_720P;
        video.frameRate = 30;

        rtsp.remotePort = 554;
        rtsp.videoPath = "/cam/realmonitor?channel=1&subtype=0";
        rtsp.localUdpPort = 20000;

        PtzPreset home = new PtzPreset();
        home.name = "Home";
        home.pan = 0;
        home.tilt = 0;
        home.zoom = 0;
        ptz.presets.add(home);
    }


    @Override
    public LLALocation getLocation()
    {
        return position.location;
    }


    @Override
    public EulerOrientation getOrientation()
    {
        return position.orientation;
    }
}
