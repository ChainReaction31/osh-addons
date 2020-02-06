package org.sensorhub.impl.sensor.tvsrop;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;

public class TvsropVideoControl extends AbstractSensorControl<TvsropDriver> {
    public TvsropVideoControl(TvsropDriver parentSensor)
    {
        super(parentSensor);
        // TODO Auto-generated constructor stub
    }


    @Override
    public String getName()
    {
        return "videoControl";
    }


    @Override
    public DataComponent getCommandDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    public void init()
    {
        // TODO Auto-generated method stub

    }


    public void stop()
    {
        // TODO Auto-generated method stub

    }
}
