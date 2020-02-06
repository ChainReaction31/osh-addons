package org.sensorhub.impl.sensor.tvsrop;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

public class TvsropDescriptor extends JarModuleProvider implements IModuleProvider {
    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return TvsropDriver.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return TvsropConfig.class;
    }
}