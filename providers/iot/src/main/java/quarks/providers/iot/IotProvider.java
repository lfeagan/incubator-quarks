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
package quarks.providers.iot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.gson.JsonObject;

import quarks.apps.iot.IotDevicePubSub;
import quarks.connectors.iot.Commands;
import quarks.connectors.iot.IotDevice;
import quarks.connectors.pubsub.service.ProviderPubSub;
import quarks.connectors.pubsub.service.PublishSubscribeService;
import quarks.execution.Configs;
import quarks.execution.DirectSubmitter;
import quarks.execution.Job;
import quarks.execution.services.ControlService;
import quarks.execution.services.ServiceContainer;
import quarks.function.Function;
import quarks.providers.direct.DirectProvider;
import quarks.runtime.appservice.AppService;
import quarks.runtime.jsoncontrol.JsonControlService;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.TopologyProvider;
import quarks.topology.services.ApplicationService;

/**
 * IoT provider supporting multiple topologies with a single connection to a
 * message hub. A provider that uses a single {@link IotDevice} to communicate
 * with an IoT scale message hub.
 * {@link quarks.connectors.pubsub.PublishSubscribe Publish-subscribe} is
 * used to allow multiple topologies to communicate through the single
 * connection.
 * <P>
 * This provider registers these services:
 * <UL>
 * <LI>{@link ControlService control} - An instance of {@link JsonControlService}.</LI>
 * <LI>{@link ApplicationService application} - An instance of {@link AppService}.</LI>
 * <LI>{@link PublishSubscribeService publish-subscribe} - An instance of {@link ProviderPubSub}</LI>
 * </UL>
 * System applications provide this functionality:
 * <UL>
 * <LI>
 * </LI>
 * </UL>
 * <UL>
 * <LI>Single connection to the message hub using an {@code IotDevice}
 * using {@link IotDevicePubSub}.
 * Applications using this provider that want to connect
 * to the message hub for device events and commands must create an instance of
 * {@code IotDevice} using {@link IotDevicePubSub#addIotDevice(quarks.topology.TopologyElement)}</LI>
 * <LI>Access to the control service through device commands from the message hub using command
 * identifier {@link Commands#CONTROL_SERVICE quarksControl}.
 * </UL>
 * </P>
 * <P>
 * An {@code IotProvider} is created with a provider and submitter that it delegates
 * the creation and submission of topologies to.
 * </P>
 * 
 * @see IotDevice
 * @see IotDevicePubSub
 */
public class IotProvider implements TopologyProvider,
 DirectSubmitter<Topology, Job> {
    
    private final TopologyProvider provider;
    private final Function<Topology, IotDevice> iotDeviceCreator;
    private final DirectSubmitter<Topology, Job> submitter;
    
    private final List<Topology> systemApps = new ArrayList<>();

    private JsonControlService controlService = new JsonControlService();
    
    /**
     * Create an {@code IotProvider} that uses its own {@code DirectProvider}.
     * @param iotDeviceCreator How the {@code IotDevice} is created.
     * 
     * @see DirectProvider
     */
    public IotProvider(Function<Topology, IotDevice> iotDeviceCreator) {   
        this(new DirectProvider(), iotDeviceCreator);
    }
    
    /**
     * Create an {@code IotProvider} that uses the passed in {@code DirectProvider}.
     * 
     * @param provider {@code DirectProvider} to use for topology creation and submission.
     * @param iotDeviceCreator How the {@code IotDevice} is created.
     * 
     * @see DirectProvider
     *
     */
    public IotProvider(DirectProvider provider, Function<Topology, IotDevice> iotDeviceCreator) {
        this(provider, provider, iotDeviceCreator);
    }

    /**
     * Create an {@code IotProvider}.
     * @param provider How topologies are created.
     * @param submitter How topologies will be submitted.
     * @param iotDeviceCreator How the {@code IotDevice} is created.
     * 
     */
    public IotProvider(TopologyProvider provider, DirectSubmitter<Topology, Job> submitter,
            Function<Topology, IotDevice> iotDeviceCreator) {
        this.provider = provider;
        this.submitter = submitter;
        this.iotDeviceCreator = iotDeviceCreator;
        
        registerControlService();
        registerApplicationService();
        registerPublishSubscribeService();
        
        createIotDeviceApp();
        createIotCommandToControlApp();
    }
    
    /**
     * Get the application service.
     * Callers may use this to register applications to
     * be executed by this provider.
     * @return application service.
     */
    public ApplicationService getApplicationService() {
        return getServices().getService(ApplicationService.class);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceContainer getServices() {
        return submitter.getServices();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final Topology newTopology() {
        return provider.newTopology();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public final Topology newTopology(String name) {
        return provider.newTopology(name);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public final Future<Job> submit(Topology topology) {
        return submitter.submit(topology);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public final Future<Job> submit(Topology topology, JsonObject config) {
        return submitter.submit(topology, config);
    }

    protected void registerControlService() {
        getServices().addService(ControlService.class, getControlService());
    }

    protected void registerApplicationService() {
        AppService.createAndRegister(this, this);
    }
    protected void registerPublishSubscribeService() {
        getServices().addService(PublishSubscribeService.class, 
                new ProviderPubSub());
    }

    protected JsonControlService getControlService() {
        return controlService;
    }
    
    /**
     * Create application that connects to the message hub.
     * Subscribes to device events and sends them to the messages hub.
     * Publishes device commands from the message hub.
     * @see IotDevicePubSub
     * @see #createMessageHubDevice(Topology)
     */
    protected void createIotDeviceApp() {
        Topology topology = newTopology("QuarksIotDevice");
             
        IotDevice msgHub = createMessageHubDevice(topology);
        IotDevicePubSub.createApplication(msgHub);
        systemApps.add(topology);
    }
    
    /**
     * Create application connects {@code quarksControl} device commands
     * to the control service.
     * 
     * Subscribes to device
     * commands of type {@link Commands#CONTROL_SERVICE}
     * and sends the payload into the JSON control service
     * to invoke the control operation.
     */
    protected void createIotCommandToControlApp() {
        Topology topology = newTopology("QuarksIotCommandsToControl");
        
        IotDevice publishedDevice = IotDevicePubSub.addIotDevice(topology);

        TStream<JsonObject> controlCommands = publishedDevice.commands(Commands.CONTROL_SERVICE);
        controlCommands.sink(cmd -> {
            try {
                getControlService().controlRequest(cmd.getAsJsonObject(IotDevice.CMD_PAYLOAD));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        systemApps.add(topology);
    }
    
    /**
     * Start this provider by starting its system applications.
     * 
     * @throws InterruptedException Interrupted exception starting applications.
     * @throws ExecutionException Exception starting applications.
     */
    public void start() throws InterruptedException, ExecutionException {
        for (Topology topology : systemApps) {
            JsonObject config = new JsonObject();
            config.addProperty(Configs.JOB_NAME, topology.getName());
            submit(topology, config).get();
        }
    }

    /**
     * Create the connection to the message hub.
     * 
     * Creates an instance of {@link IotDevice}
     * used to communicate with the message hub. This
     * provider creates and submits an application
     * that subscribes to published events to send
     * as device events and publishes device commands.
     * <BR>
     * The application is created using
     * {@link IotDevicePubSub#createApplication(IotDevice)}.
     * <BR>
     * The {@code IotDevice} is created using the function
     * passed into the constructor.
     * 
     * @param topology Topology the {@code IotDevice} will be contained in.
     * @return IotDevice device used to communicate with the message hub.
     * 
     * @see IotDevice
     * @see IotDevicePubSub
     */
    protected IotDevice createMessageHubDevice(Topology topology) {
        return iotDeviceCreator.apply(topology);
    }
}
