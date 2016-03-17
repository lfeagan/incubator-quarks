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
package quarks.test.fvt.iot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import quarks.connectors.pubsub.PublishSubscribe;
import quarks.execution.DirectSubmitter;
import quarks.runtime.jsoncontrol.JsonControlService;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.mbeans.ApplicationServiceMXBean;
import quarks.topology.services.ApplicationService;

public class IotTestApps {
    
    public static JsonObject newSubmitRequest(String name) {
        JsonObject submitApp = new JsonObject();   
        submitApp.addProperty(JsonControlService.TYPE_KEY, ApplicationServiceMXBean.TYPE);
        submitApp.addProperty(JsonControlService.ALIAS_KEY, ApplicationService.ALIAS);
        JsonArray args = new JsonArray();
        args.add(new JsonPrimitive(name));
        args.add(new JsonObject());
        submitApp.addProperty(JsonControlService.OP_KEY, "submit");
        submitApp.add(JsonControlService.ARGS_KEY, args); 
        
        return submitApp;
    }
    
    public static void registerApplications(DirectSubmitter<?, ?> submitter) {
        ApplicationService apps = submitter.getServices().getService(ApplicationService.class);
        
        apps.registerTopology("AppOne", IotTestApps::createApplicationOne);
    }
    
    public static void createApplicationOne(Topology topology, JsonObject config) {
        TStream<String> out = topology.strings("APP1_A", "APP1_B", "APP1_C");
        PublishSubscribe.publish(out, "appOne", String.class);
    }
}
