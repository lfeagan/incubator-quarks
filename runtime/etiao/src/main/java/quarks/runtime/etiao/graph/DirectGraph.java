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
package quarks.runtime.etiao.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import quarks.execution.services.ServiceContainer;
import quarks.graph.Edge;
import quarks.graph.Graph;
import quarks.graph.Vertex;
import quarks.graph.spi.AbstractGraph;
import quarks.oplet.Oplet;
import quarks.runtime.etiao.EtiaoJob;
import quarks.runtime.etiao.Executable;
import quarks.runtime.etiao.Invocation;

/**
 * {@code DirectGraph} is a {@link Graph} that
 * is executed in the current virtual machine.
 * 
 */
public class DirectGraph extends AbstractGraph<Executable> {

    private final EtiaoJob job;
    private final Executable executable;
    private final List<ExecutableVertex<? extends Oplet<?, ?>, ?, ?>> vertices = new ArrayList<>();

    /**
     * Creates a new {@code DirectGraph} instance underlying the specified 
     * topology.
     * 
     * @param topologyName name of the topology
     * @param container service container
     */
    public DirectGraph(String topologyName, ServiceContainer container) {
        this.job = new EtiaoJob(this, topologyName, container);
        this.executable = new Executable(job);
    }

    /**
     * Returns the {@code Executable} running this graph.
     * @return the executable
     */
    public Executable executable() {
        return executable;
    }

    /**
     * Returns the {@code EtiaoJob} controlling the execution.
     * @return the executable
     */
    public EtiaoJob job() {
        return job;
    }

    @Override
	public <OP extends Oplet<C, P>, C, P> ExecutableVertex<OP, C, P> insert(OP oplet, int inputs, int outputs) {
        Invocation<OP, C, P> invocation = executable().addOpletInvocation(oplet, inputs, outputs);
        ExecutableVertex<OP, C, P> vertex = new ExecutableVertex<>(this, invocation);
        vertices.add(vertex);
        return vertex;
    }

    @Override
    public Collection<Vertex<? extends Oplet<?, ?>, ?, ?>> getVertices() {
        return Collections.unmodifiableList(vertices);
    }
    
    @Override
    public Collection<Edge> getEdges() {
        List<Edge> edges = new ArrayList<>();
        for (ExecutableVertex<? extends Oplet<?, ?>, ?, ?> ev : vertices) {
            for (Edge e : ev.getEdges()) {
                edges.add(e);
            }
        }
        return Collections.unmodifiableList(edges);
    }
}
