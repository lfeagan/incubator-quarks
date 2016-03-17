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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import quarks.execution.services.ControlService;
import quarks.providers.direct.DirectProvider;
import quarks.runtime.appservice.AppService;
import quarks.runtime.jsoncontrol.JsonControlService;
import quarks.topology.mbeans.ApplicationServiceMXBean;
import quarks.topology.services.ApplicationService;

public class IotAppServiceTest {
    
    @Test
    public void testAppService() throws Exception {
        
        DirectProvider provider = new DirectProvider();
        
        JsonControlService control = new JsonControlService();
        provider.getServices().addService(ControlService.class, control);
        
        ApplicationService apps = AppService.createAndRegister(provider, provider);
        provider.getServices().addService(ApplicationService.class, apps);
        
        IotTestApps.registerApplications(provider);       
        
        JsonObject submitAppOne = newSubmitRequest("AppOne");
        
        JsonElement crr = control.controlRequest(submitAppOne);
        
        assertTrue(crr.getAsBoolean());
    }
    
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
}
