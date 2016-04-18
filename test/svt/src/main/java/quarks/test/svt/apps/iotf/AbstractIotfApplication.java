/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package quarks.test.svt.apps.iotf;

import quarks.connectors.iot.IotDevice;
import quarks.connectors.iotf.IotfDevice;
import java.io.PrintWriter;
import java.util.Random;
import quarks.samples.apps.AbstractApplication;
import quarks.samples.apps.ApplicationUtilities;
import quarks.samples.apps.TopologyProviderFactory;
import quarks.topology.Topology;

/**
 * An IotF Application base class.
 * <p>
 * Application instances need to:
 * <ul>
 * <li>define an implementation for {@link #buildTopology(Topology)}</li>
 * <li>call {@link #run()} to build and submit the topology for execution.</li>
 * </ul>
 * <p>
 * The class provides some common processing needs:
 * <ul>
 * <li>Support for an external configuration file</li>
 * <li>Provides a {@link TopologyProviderFactory}</li>
 * <li>Provides a {@link ApplicationUtilities}</li>
 * <li>Provides a {@link IotDevice}</li>
 * </ul>
 */
public abstract class AbstractIotfApplication extends AbstractApplication {

    private IotDevice device;

    public AbstractIotfApplication(String propsPath) throws Exception {
        super(propsPath);
    }

    @Override
    protected void preBuildTopology(Topology topology) {
        // Add an Iotf device communication manager to the topology
        // Declare a connection to IoTF Quickstart service
        String deviceId = "qs" + Long.toHexString(new Random().nextLong());
        device = IotfDevice.quickstart(topology, deviceId);

        // TODO replace quickstart
        // iotfDevice = new IotfDevice(topology, new File("device.cfg"));

        System.out.println("Quickstart device type:" + IotfDevice.QUICKSTART_DEVICE_TYPE);
        System.out.println("Quickstart device id  :" + deviceId);
        System.out.println("https://quickstart.internetofthings.ibmcloud.com/#/device/" + deviceId);
        // Also write this information to file quickstartUrl.txt in case the
        // console scrolls too fast
        try {
            PrintWriter writer = new PrintWriter("iotfUrl.txt", "UTF-8");
            writer.println("Quickstart device type:" + IotfDevice.QUICKSTART_DEVICE_TYPE);
            writer.println("Quickstart device id  :" + deviceId);
            writer.println("https://quickstart.internetofthings.ibmcloud.com/#/device/" + deviceId);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the application's IotfDevice
     * 
     * @return the IotfDevice
     */
    public IotDevice iotDevice() {
        return device;
    }

    /**
     * Compose a IotfDevice eventId for the sensor.
     * 
     * @param sensorId
     *            the sensor id
     * @param eventId
     *            the sensor's eventId
     * @return the device eventId
     */
    public String sensorEventId(String sensorId, String eventId) {
        return sensorId + "." + eventId;
    }

    /**
     * Compose a IotfDevice commandId for the sensor
     * 
     * @param sensorId
     *            the sensor id
     * @param commandId
     *            the sensor's commandId
     * @return the device commandId
     */
    public String commandId(String sensorId, String commandId) {
        return sensorId + "." + commandId;
    }
}
