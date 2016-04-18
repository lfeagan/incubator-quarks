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

package quarks.samples.connectors.iotf;

import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import quarks.connectors.iot.IotDevice;
import quarks.connectors.iot.QoS;
import quarks.connectors.iotf.IotfDevice;
import quarks.providers.direct.DirectProvider;
import quarks.providers.direct.DirectTopology;
import quarks.samples.topology.SensorsAggregates;
import quarks.topology.TStream;

/**
 * Sample sending sensor device events to IBM Watson IoT Platform. <BR>
 * Simulates a couple of bursty sensors and sends the readings from the sensors
 * to IBM Watson IoT Platform as device events with id {@code sensors}. <BR>
 * Subscribes to device commands with identifier {@code display}.
 * <P>
 * In addition a device event with id {@code hearbeat} is sent
 * every minute. This ensure a connection attempt to IBM Watson IoT Platform
 * is made immediately rather than waiting for a bursty sensor to become
 * active.
 * <P>
 * This sample requires an IBM Watson IoT Platform service and a device configuration.
 * The device configuration is read from the file {@code device.cfg} in the
 * current directory. <BR>
 * In order to see commands send from IBM Watson IoT Platform
 * there must be an analytic application
 * that sends commands with the identifier {@code display}.
 * </P>
 */
public class IotfSensors {

    /**
     * Run the IotfSensors application.
     * 
     * Takes a single argument that is the path to the
     * device configuration file containing the connection
     * authentication information.
     * 
     * @param args Must contain the path to the device configuration file.
     * 
     * @see IotfDevice#IotfDevice(quarks.topology.Topology, File)
     */
    public static void main(String[] args) {
        
        String deviceCfg = args[0];

        DirectProvider tp = new DirectProvider();
        DirectTopology topology = tp.newTopology("IotfSensors");

        // Declare a connection to IoTF
        IotDevice device = new IotfDevice(topology, new File(deviceCfg));

        // Simulated sensors for this device.
        simulatedSensors(device, true);
        
        // Heartbeat
        heartBeat(device, true);

        // Subscribe to commands of id "display" for this
        // device and print them to standard out
        displayMessages(device, true);

        tp.submit(topology);
    }


    /**
     * Simulate two bursty sensors and send the readings as IoTF device events
     * with an identifier of {@code sensors}.
     * 
     * @param device
     *            IoT device
     * @param print
     *            True if the data submitted as events should also be printed to
     *            standard out.
     */
    public static void simulatedSensors(IotDevice device, boolean print) {

        TStream<JsonObject> sensors = SensorsAggregates.sensorsAB(device.topology());
        if (print)
            sensors.print();

        // Send the device streams as IoTF device events
        // with event identifier "sensors".
        device.events(sensors, "sensors", QoS.FIRE_AND_FORGET);
    }
    
    /**
     * Create a heart beat device event with
     * identifier {@code heartbeat} to
     * ensure there is some immediate output and
     * the connection to IoTF happens as soon as possible.
     * @param device IoT device
     */
    public static void heartBeat(IotDevice device, boolean print) {
        // In addition create a heart beat event to
        // ensure there is some immediate output and
        // the connection to IoTF happens as soon as possible.
        TStream<Date> hb = device.topology().poll(() -> new Date(), 1, TimeUnit.MINUTES);
        // Convert to JSON
        TStream<JsonObject> hbj = hb.map(d -> {
            JsonObject j = new  JsonObject();
            j.addProperty("when", d.toString());
            j.addProperty("hearbeat", d.getTime());
            return j;
        });
        if (print)
            hbj.print();
        device.events(hbj, "heartbeat", QoS.FIRE_AND_FORGET);
    }
    

    /**
     * Subscribe to IoTF device commands with identifier {@code display}.
     * Subscribing to device commands returns a stream of JSON objects that
     * include a timestamp ({@code tsms}), command identifier ({@code command})
     * and payload ({@code payload}). Payload is the application specific
     * portion of the command. <BR>
     * In this case the payload is expected to be a JSON object containing a
     * {@code msg} key with a string display message. <BR>
     * The returned stream consists of the display message string extracted from
     * the JSON payload.
     * <P>
     * Note to receive commands a analytic application must exist that generates
     * them through IBM Watson IoT Platform.
     * </P>
     * 
     * @see IotDevice#commands(String...)
     */
    public static TStream<String> displayMessages(IotDevice device, boolean print) {
        // Subscribe to commands of id "status" for this device
        TStream<JsonObject> statusMsgs = device.commands("display");

        // The returned JSON object includes several fields
        // tsms - Timestamp in milliseconds (this is generic to a command)
        // payload.msg - Status message (this is specific to this application)

        // Map to a String object containing the message
        TStream<String> messages = statusMsgs.map(j -> j.getAsJsonObject("payload").getAsJsonPrimitive("msg").getAsString());
        if (print)
            messages.print();
        return messages;
    }
}
