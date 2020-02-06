package org.sensorhub.impl.sensor.tvsrop;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPreset;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPresetsHandler;
import org.vast.data.DataChoiceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;

public class TvsropPtzControl extends AbstractSensorControl<TvsropDriver> {
    DataChoice commandData;

    // define and set default values
    double minPan = 0.0;
    double maxPan = 360.0;
    double minTilt = 0.0;
    double maxTilt = 90.0;
    double minZoom = 0.0;
    //TODO: Determine max zoom for TVS ROP cameras
    double maxZoom = 1.0;

    PtzPresetsHandler presetsHandler;
    boolean alwaysRequestPtzStatus;
    long lastCommandReceived = 0;

    protected TvsropPtzControl(TvsropDriver driver){
        super(driver);
        this.alwaysRequestPtzStatus = !driver.getConfiguration().exclusiveControl;
    }

    @Override
    public String getName()
    {
        return "ptzControl";
    }

    protected void init() throws SensorException
    {
        TvsropConfig config = parentSensor.getConfiguration();
        presetsHandler = new PtzPresetsHandler(config.ptz);

        // get PTZ limits
        try
        {
            URL optionsURL = new URL(parentSensor.getHostUrl() + "/ptz.cgi?action=getCurrentProtocolCaps&channel=0");
            InputStream is = optionsURL.openStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(is));

            // get limit values from IP stream
            String line;
            while ((line = bReader.readLine()) != null)
            {
                // parse response
                String[] tokens = line.split("=");

                if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.HorizontalAngle[0]"))
                    minPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.HorizontalAngle[1]"))
                    maxPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.VerticalAngle[0]"))
                    minTilt = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.VerticalAngle[1]"))
                    maxTilt = Double.parseDouble(tokens[1]);
                //else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxZoom"))
                //    maxZoom = Double.parseDouble(tokens[1]);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // build SWE data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();
        Collection<String> presetList = presetsHandler.getPresetNames();
        commandData = videoHelper.getPtzTaskParameters(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom, presetList);
    }

    protected void start() throws SensorException
    {
        // reset to Pan=0, Tilt=0, Zoom=0
        DataBlock initCmd;
        commandData.setSelectedItem(7);
        initCmd = commandData.createDataBlock();
        execCommand(initCmd);
    }


    protected void stop()
    {
    }


    @Override
    public DataComponent getCommandDescription()
    {
        return commandData;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        // reject if commands are too frequent because camera cannot handle
        // successive commands if they come too fast
        if (System.currentTimeMillis() - lastCommandReceived < 1000)
        {
            parentSensor.getLogger().warn("PTZ command rejected");
            CommandStatus cmdStatus = new CommandStatus();
            cmdStatus.status = CommandStatus.StatusCode.REJECTED;
            return cmdStatus;
        }
        lastCommandReceived = System.currentTimeMillis();

        // associate command data to msg structure definition
        DataChoice commandMsg = commandData.copy();
        commandMsg.setData(command);

        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String itemID = component.getName();
        DataBlock data = component.getData();

        // refresh PTZ status
        if (alwaysRequestPtzStatus)
            parentSensor.ptzDataInterface.requestPtzStatus();
        double pan = parentSensor.ptzDataInterface.pan;
        double tilt = parentSensor.ptzDataInterface.tilt;
        double zoom = parentSensor.ptzDataInterface.zoom;

        try
        {
            // preset position
            if (itemID.equals(VideoCamHelper.TASKING_PTZPRESET))
            {
                PtzPreset preset = presetsHandler.getPreset(data.getStringValue());
                pan = preset.pan;
                tilt = preset.tilt;
                zoom = preset.zoom;
            }

            // all 3 ptz components together
            else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PTZ_POS))
            {
//        		System.out.println("Got all 3 PTZ parameters...");
                pan = data.getDoubleValue(0);
                tilt = data.getDoubleValue(1);
                if (!Double.isNaN(data.getDoubleValue(2)))
                    zoom = data.getDoubleValue(2);
            }

            // individual component command
            else
            {
                if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PAN))
                    pan = data.getDoubleValue();
                else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_TILT))
                    tilt = data.getDoubleValue();
                else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_ZOOM))
                    zoom = data.getDoubleValue();
                else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_RPAN))
                    pan = data.getDoubleValue() + pan;
                else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_RTILT))
                    tilt = data.getDoubleValue() + tilt;
                else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_RZOOM))
                    zoom = data.getDoubleValue() + zoom;

                System.out.println("Going to pan=" + pan + ", tilt= " + tilt + ", zoom=" + zoom);
            }

            // limit values
            if (pan < 0)
                pan += 360.0;

            // send request to absolute pan/tilt/zoom positions
            URL optionsURL = new URL(parentSensor.getHostUrl() +
                    "/ptz.cgi?action=start&channel=0&code=PositionABS&arg1=" + pan + "&arg2=" + (-tilt) + "&arg3=" + zoom*120);
            if (!alwaysRequestPtzStatus)
            {
                parentSensor.ptzDataInterface.pan = (float)pan;
                parentSensor.ptzDataInterface.tilt = (float)tilt;
                parentSensor.ptzDataInterface.zoom = (float)zoom;
                parentSensor.ptzDataInterface.sendPtzStatus();
            }

            // add BufferReader and read first line; if "Error", read second line and log error
            InputStream is = optionsURL.openStream();
            is.close();
        }
        catch (Exception e)
        {
            throw new SensorException("Error connecting to Dahua PTZ control", e);
        }

        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = CommandStatus.StatusCode.COMPLETED;

        return cmdStatus;
    }
}
