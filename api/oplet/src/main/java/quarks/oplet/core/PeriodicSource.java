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
package quarks.oplet.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import quarks.execution.services.ControlService;
import quarks.oplet.OpletContext;
import quarks.oplet.core.mbeans.PeriodicMXBean;

public abstract class PeriodicSource<T> extends Source<T> implements Runnable, PeriodicMXBean {

    private long period;
    private TimeUnit unit;
    private ScheduledFuture<?> future;

    protected PeriodicSource(long period, TimeUnit unit) {
        this.period = period;
        this.unit = unit;
    }

    @Override
    public void initialize(OpletContext<Void, T> context) {
        super.initialize(context);
    }

    @Override
    public synchronized void start() {
        ControlService cs = getOpletContext().getService(ControlService.class);
        if (cs != null)
            cs.registerControl("periodic", getOpletContext().uniquify(getClass().getSimpleName()), null, PeriodicMXBean.class, this);
        schedule(false);
    }

    private synchronized void schedule(boolean delay) {
        future = getOpletContext().getService(ScheduledExecutorService.class).scheduleAtFixedRate(
                getRunnable(), delay ? getPeriod() : 0, getPeriod(), getUnit());
    }

    protected Runnable getRunnable() {
        return this;
    }

    protected abstract void fetchTuples() throws Exception;

    @Override
    public void run() {
        try {
            fetchTuples();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized long getPeriod() {
        return period;
    }

    @Override
    public synchronized TimeUnit getUnit() {
        return unit;
    }

    @Override
    public synchronized void setPeriod(long period) {
        if (period <= 0)
            throw new IllegalArgumentException();
        if (this.period != period) {
            future.cancel(false);
            this.period = period;
            schedule(true);
        }  
    }
}
