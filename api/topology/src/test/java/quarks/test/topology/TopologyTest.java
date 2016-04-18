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
package quarks.test.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.junit.Ignore;
import org.junit.Test;

import quarks.execution.services.RuntimeServices;
import quarks.function.Supplier;
import quarks.topology.TStream;
import quarks.topology.Topology;
import quarks.topology.tester.Condition;

@Ignore("abstract, provides common tests for concrete implementations")
public abstract class TopologyTest extends TopologyAbstractTest {

    @Test
    public void testBasics() {
        final Topology t = newTopology("T123");
        assertEquals("T123", t.getName());
        assertSame(t, t.topology());
    }

    @Test
    public void testDefaultName() {
        final Topology t = newTopology();
        assertSame(t, t.topology());
        assertNotNull(t.getName());
    }

    @Test
    public void testStringContants() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "c");
        assertStream(t, s);

        Condition<Long> tc = t.getTester().tupleCount(s, 3);
        Condition<List<String>> contents = t.getTester().streamContents(s, "a", "b", "c");
        complete(t, tc);
        assertTrue(contents.valid());
    }

    @Test
    public void testNoStringContants() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings();

        Condition<Long> tc = t.getTester().tupleCount(s, 0);

        complete(t, tc);
        
        assertTrue(tc.valid());
    }
    
    @Test
    public void testRuntimeServices() throws Exception {
        Topology t = newTopology();
        TStream<String> s = t.strings("A");
        
        Supplier<RuntimeServices> serviceGetter =
                t.getRuntimeServiceSupplier();
        
        TStream<Boolean> b = s.map(tuple -> 
            serviceGetter.get().getService(ThreadFactory.class) != null
            && serviceGetter.get().getService(ScheduledExecutorService.class) != null
        );
        
        Condition<List<Boolean>> tc = t.getTester().streamContents(b, Boolean.TRUE);
        complete(t, tc);
        
        assertTrue(tc.valid());
    }
}
