/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ibm.mqlight.api.impl.engine;

import java.nio.ByteBuffer;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Handler;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;

public class MockNetworkChannel implements NetworkChannel {

    private final NetworkListener listener;
    private final Handler handler;
    private final Connection connection;
    private final Transport transport;
    private final Collector collector;
    private Object context = null;
    
    public MockNetworkChannel(NetworkListener listener, Handler handler) {
        this.listener = listener;
        this.handler = handler;
        collector = Proton.collector();
        connection = Proton.connection();
        transport = Proton.transport();
        connection.collect(collector);
        connection.setHostname("localhost");
        connection.setContainer("some container");
        transport.bind(connection);
        Sasl sasl = transport.sasl();
        sasl.server();
        sasl.setMechanisms(new String[]{"ANONYMOUS"});
        sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
    }
    
    @Override
    public void close(Promise<Void> promise) {
        if (promise != null) promise.setSuccess(null);
    }

    @Override
    public void write(ByteBuffer buffer, Promise<Boolean> promise) {
        promise.setSuccess(true);
        
        ByteBuffer tail = transport.tail();
        while(buffer.remaining() > 0) {
            int amount = Math.min(buffer.remaining(), tail.capacity());
            tail.limit(tail.position() + amount);
            tail.put(buffer);
            transport.process();
            while(collector.peek() != null) {
                Event event = collector.peek();
                event.dispatch(handler);
                collector.pop();
            }
            
        }
        process();
    }

    public void process() {
        if (transport.pending() > 0) {
            ByteBuffer head = transport.head();
            int amount = head.remaining();
            ByteBuffer tmp = ByteBuffer.allocate(amount);
            tmp.put(head);
            tmp.flip();
            transport.pop(amount);
            listener.onRead(this, tmp);
        }
    }
    
    @Override
    public void setContext(Object context) {
        this.context = context;
    }

    @Override
    public Object getContext() {
        return context;
    }
}
