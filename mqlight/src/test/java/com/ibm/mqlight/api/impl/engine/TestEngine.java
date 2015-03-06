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

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.BaseHandler;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Handler;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.junit.Test;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.MockComponent;
import com.ibm.mqlight.api.impl.network.ConnectionError;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;
import com.ibm.mqlight.api.network.NetworkService;
import com.ibm.mqlight.api.timer.TimerService;

public class TestEngine {

    private class MockHandler extends BaseHandler {
        private Delivery delivery = null;
        private boolean closeConnection = false;

        @Override
        public void onConnectionRemoteOpen(Event e) {
            System.out.println("onConnectionRemoteOpen");
            e.getConnection().open();
        }

        @Override
        public void onConnectionRemoteClose(Event e) {
            System.out.println("onConnectionRemoteClose");
            e.getConnection().close();
        }

        @Override
        public void onSessionRemoteOpen(Event e) {
            System.out.println("onSessionRemoteOpen");
            e.getSession().open();
        }

        @Override
        public void onSessionRemoteClose(Event e) {
            System.out.println("onSessionRemoteClose");
            e.getSession().close();
        }

        @Override
        public void onLinkRemoteOpen(Event e) {
            System.out.println("onLinkRemoteOpen");
            e.getLink().open();
            if (e.getLink() instanceof Sender) {
                Sender sender = (Sender)e.getLink();
                delivery = sender.delivery(new byte[]{1});
                sender.send(new byte[]{1, 2, 3}, 0, 3);
                sender.advance();
            } else {
                Receiver receiver = (Receiver)e.getLink();
                receiver.flow(1024);
            }
            if (closeConnection) {
                e.getConnection().setCondition(new ErrorCondition(Symbol.getSymbol("symbol"), "Something went wrong!"));
                e.getConnection().close();
            }
        }

        @Override
        public void onLinkRemoteDetach(Event e) {
            System.out.println("onLinkRemoteDetach");
        }

        @Override
        public void onLinkRemoteClose(Event e) {
            System.out.println("onLinkRemoteClose");
            e.getLink().close();
        }

        @Override
        public void onDelivery(Event e) {
            System.out.println("onDelivery");
        }

        @Override
        public void onUnhandled(Event e) {
            System.out.println("onUnhandled: " + e);
        }

    }

    private class MockNetworkService implements NetworkService {
        private final Handler handler;
        private MockNetworkChannel channel = null;
        private MockNetworkService(Handler handler) {
            this.handler = handler;
        }
        @Override
        public void connect(Endpoint endpoint, NetworkListener listener, Promise<NetworkChannel> promise) {
            if (handler != null) {
                channel = new MockNetworkChannel(listener, handler);
                promise.setSuccess(channel);
            } else {
                promise.setFailure(new IOException("Couldn't connect!"));
            }
        }
    }

    private class MockTimerService implements TimerService {

        @Override
        public void schedule(long delay, Promise<Void> promise) {
            // TODO Auto-generated method stub

        }

        @Override
        public void cancel(Promise<Void> promise) {
            // TODO Auto-generated method stub

        }

    }

    protected class StubEndpoint implements Endpoint {
        @Override public String getHost() { return null; }
        @Override public int getPort() { return 0; }
        @Override public boolean useSsl() { return false; }
        @Override public File getCertChainFile() { return null; }
        @Override public boolean getVerifyName() { return false; }
        @Override public String getUser() { return null; }
        @Override public String getPassword() { return null; }
    }

    @Test
    public void openClose() {
        NetworkService network = new MockNetworkService(new MockHandler());
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);

        assertEquals("Expected one message to have been sent to component", 1, component.getMessages().size());
        assertTrue("Expected message to be of type OpenResponse", component.getMessages().get(0) instanceof OpenResponse);
        OpenResponse openResponse = (OpenResponse)component.getMessages().get(0);
        assertNull("Expected no exception in openResponse", openResponse.exception);
        assertNotNull("Expected an engine connection in openResponse", openResponse.connection);
        assertSame("Expected request to be linked in openResponse", expectedOpenRequest, openResponse.request);

        CloseRequest expectedCloseRequest = new CloseRequest(openResponse.connection);
        engine.tell(expectedCloseRequest, component);
        assertEquals("Expected one more message to have been sent to component", 2, component.getMessages().size());
        assertTrue("Expected 2nd message to be of type CloseResponse", component.getMessages().get(1) instanceof CloseResponse);
        CloseResponse closeReponse = (CloseResponse)component.getMessages().get(1);
        assertSame("Expected request to be linked in closeResponse", expectedCloseRequest, closeReponse.request);
    }

    @Test
    public void openFails() {
        NetworkService network = new MockNetworkService(null);
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);

        assertEquals("Expected one message to have been sent to component", 1, component.getMessages().size());
        assertTrue("Expected message to be of type OpenResponse", component.getMessages().get(0) instanceof OpenResponse);
        OpenResponse openResponse = (OpenResponse)component.getMessages().get(0);
        assertNotNull("Expected open response to contain an exception", openResponse.exception);
        assertNull("Expected open response not to contain an engine connection", openResponse.connection);
    }


    @Test
    public void send() {
        NetworkService network = new MockNetworkService(new MockHandler());
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);
        OpenResponse openResponse = (OpenResponse)component.getMessages().get(0);

        engine.tell(new SendRequest(openResponse.connection, "topic1", wrappedBuffer(new byte[]{1, 2, 3}), 3, QOS.AT_MOST_ONCE), component);
        assertEquals("Expected two more messages to have been sent to component", 3, component.getMessages().size());
        assertTrue("Expected message 2 to be of type DrainNotification", component.getMessages().get(1) instanceof DrainNotification);
        assertTrue("Expected message 3 to be of type SendResponse", component.getMessages().get(2) instanceof SendResponse);
    }

    @Test
    public void receiveQos0() {
        NetworkService network = new MockNetworkService(new MockHandler());
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);
        OpenResponse openResponse = (OpenResponse)component.getMessages().get(0);

        engine.tell(new SubscribeRequest(openResponse.connection, "topic1", QOS.AT_MOST_ONCE, 10, 0), component);
        assertEquals("Expected two more messages to have been sent to component", 3, component.getMessages().size());
        assertTrue("Expected message 2 to be of type SubscribeResponse", component.getMessages().get(1) instanceof SubscribeResponse);
        assertTrue("Expected message 3 to be of type DeliveryRequest", component.getMessages().get(2) instanceof DeliveryRequest);
    }

    @Test
    public void receiveQos1() {
        MockHandler handler = new MockHandler();
        MockNetworkService network = new MockNetworkService(handler);
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);
        OpenResponse openResponse = (OpenResponse)component.getMessages().get(0);

        engine.tell(new SubscribeRequest(openResponse.connection, "topic1", QOS.AT_LEAST_ONCE, 10, 0), component);
        assertEquals("Expected two more messages to have been sent to component", 3, component.getMessages().size());
        assertTrue("Expected message 2 to be of type SubscribeResponse", component.getMessages().get(1) instanceof SubscribeResponse);
        assertTrue("Expected message 3 to be of type DeliveryRequest", component.getMessages().get(2) instanceof DeliveryRequest);

        engine.tell(new DeliveryResponse((DeliveryRequest)component.getMessages().get(2)), component);
        assertTrue("Delivery should have been marked as settled", handler.delivery.remotelySettled());
    }

    @Test
    public void unsubscribe() {
        MockHandler handler = new MockHandler();
        MockNetworkService network = new MockNetworkService(handler);
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);
        OpenResponse openResponse = (OpenResponse)component.getMessages().get(0);

        engine.tell(new SubscribeRequest(openResponse.connection, "topic1", QOS.AT_MOST_ONCE, 10, 0), component);

        engine.tell(new UnsubscribeRequest(openResponse.connection, "topic1", true), component);
        assertEquals("Expected to have received 4 messages to the component", 4, component.getMessages().size());
        assertTrue("Expected 4th message to be of type unsubscribe response", component.getMessages().get(3) instanceof UnsubscribeResponse);
    }

    @Test
    public void connectionError() {
        MockHandler handler = new MockHandler();
        MockNetworkService network = new MockNetworkService(handler);
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);
        engine.tell(new ConnectionError(network.channel, new IOException()), Component.NOBODY);

        assertEquals("Expected to have received 1 more message to the component", 2, component.getMessages().size());
    }

    @Test
    public void remoteClose() {
        MockHandler handler = new MockHandler();
        handler.closeConnection = true;
        MockNetworkService network = new MockNetworkService(handler);
        TimerService timer = new MockTimerService();
        Endpoint endpoint = new StubEndpoint();
        MockComponent component = new MockComponent();

        Engine engine = new Engine(network, timer);
        OpenRequest expectedOpenRequest = new OpenRequest(endpoint, "client-id");
        engine.tell(expectedOpenRequest, component);
        OpenResponse openResponse = (OpenResponse)component.getMessages().get(0);

        engine.tell(new SubscribeRequest(openResponse.connection, "topic1", QOS.AT_MOST_ONCE, 10, 0), component);
        System.out.println(component.getMessages());
    }
}
