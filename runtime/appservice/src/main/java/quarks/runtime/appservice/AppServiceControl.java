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
package quarks.runtime.appservice;

import java.util.concurrent.ExecutionException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import quarks.execution.Configs;
import quarks.function.BiConsumer;
import quarks.topology.Topology;
import quarks.topology.mbeans.ApplicationServiceMXBean;

public class AppServiceControl implements ApplicationServiceMXBean {
    
    private final AppService service;
    
    AppServiceControl(AppService service) {
        this.service = service;
    }

    @Override
    public void submit(String applicationName, String jsonConfig) throws Exception {
        
        BiConsumer<Topology, JsonObject> builder = service.getBuilder(applicationName);
        if (builder == null)
            return;
        
        JsonObject config;
        
        if (jsonConfig != null && !jsonConfig.isEmpty())
            config = (JsonObject) new JsonParser().parse(jsonConfig);
        else
            config = new JsonObject();
        
        Topology topology = service.getProvider().newTopology(applicationName);
        
        // Fill in the topology
        builder.accept(topology, config);
        
        if (!config.has(Configs.JOB_NAME))
            config.addProperty(Configs.JOB_NAME, applicationName);
        
        try {
            service.getSubmitter().submit(topology, config).get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof Error)
                throw (Error) t;
            throw (Exception) t;
        }
    }
}
