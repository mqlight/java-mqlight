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
package com.ibm.mqlight.api.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.netty.buffer.ByteBuf;

import java.io.File;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import junit.framework.AssertionFailedError;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.junit.Test;

import com.google.gson.GsonBuilder;
import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.ClientState;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.Delivery;
import com.ibm.mqlight.api.DestinationAdapter;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.MalformedDelivery;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientListener;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.SendOptions;
import com.ibm.mqlight.api.StoppedException;
import com.ibm.mqlight.api.SubscribeOptions;
import com.ibm.mqlight.api.SubscribedException;
import com.ibm.mqlight.api.UnsubscribedException;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.endpoint.EndpointService;
import com.ibm.mqlight.api.impl.callback.SameThreadCallbackService;
import com.ibm.mqlight.api.impl.engine.CloseRequest;
import com.ibm.mqlight.api.impl.engine.CloseResponse;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;
import com.ibm.mqlight.api.impl.engine.DisconnectNotification;
import com.ibm.mqlight.api.impl.engine.EngineConnection;
import com.ibm.mqlight.api.impl.engine.OpenRequest;
import com.ibm.mqlight.api.impl.engine.OpenResponse;
import com.ibm.mqlight.api.impl.engine.SendRequest;
import com.ibm.mqlight.api.impl.engine.SendResponse;
import com.ibm.mqlight.api.impl.engine.SubscribeRequest;
import com.ibm.mqlight.api.impl.engine.SubscribeResponse;
import com.ibm.mqlight.api.impl.engine.UnsubscribeRequest;
import com.ibm.mqlight.api.impl.engine.UnsubscribeResponse;
import com.ibm.mqlight.api.timer.TimerService;

public class TestNonBlockingClientImpl {

    private class StubEndpointService implements EndpointService {
        @Override public void lookup(EndpointPromise promise) {}
        @Override public void onSuccess(Endpoint endpoint) {}
    }

    private class StubCallbackService implements CallbackService {
        @Override public void run(Runnable runnable, Object orderingCtx, Promise<Void> promise) {}
    }

    private class StubTimerService implements TimerService {
        @Override public void schedule(long delay, Promise<Void> promise) {}
        @Override public void cancel(Promise<Void> promise) {}
    }

    private class StubEndpoint implements Endpoint {
        @Override public String getHost() { return null; }
        @Override public int getPort() { return 0; }
        @Override public boolean useSsl() { return false; }
        @Override public File getCertChainFile() { return null; }
        @Override public boolean getVerifyName() { return false; }
        @Override public String getUser() { return null; }
        @Override public String getPassword() {return null;}
    }

    private class MockEndpointService implements EndpointService {
        int count = 0;
        @Override
        public void lookup(EndpointPromise promise) {
            if (count++ % 2 == 0) {
                promise.setSuccess(new StubEndpoint());
            } else {
                promise.setWait(1000);
            }
        }
        @Override public void onSuccess(Endpoint endpoint) {}
    }

    private class MockTimerService implements TimerService {
        @Override
        public void schedule(long delay, Promise<Void> promise) {
            promise.setSuccess(null);
        }
        @Override
        public void cancel(Promise<Void> promise) {
            promise.setSuccess(null);
        }

    }


//    private class StubDestinationListener<T> implements DestinationListener<T> {
//        @Override public void onMessage(NonBlockingClient client, T context, Delivery delivery) {}
//        @Override public void onMalformed(NonBlockingClient client, T context, MalformedDelivery delivery) {}
//        @Override public void onUnsubscribed(NonBlockingClient client, T context, String topicPattern, String share) {}
//    }

    private class MockNonBlockingClientListener implements NonBlockingClientListener<Void> {
        private final boolean throwAssertionFailures;
        public MockNonBlockingClientListener(boolean throwAssertionFailures) {
            this.throwAssertionFailures = throwAssertionFailures;
        }
        @Override public void onStarted(NonBlockingClient client, Void context) {
            if (throwAssertionFailures) throw new AssertionFailedError("onStarted should not have been called");
        }
        @Override public void onStopped(NonBlockingClient client, Void context, ClientException exception) {
            if (throwAssertionFailures) throw new AssertionFailedError("onStopped should not have been called");
        }
        @Override public void onRestarted(NonBlockingClient client, Void context) {
            if (throwAssertionFailures) throw new AssertionFailedError("onRestarted should not have been called");
        }
        @Override public void onRetrying(NonBlockingClient client, Void context, ClientException exception) {
            if (throwAssertionFailures) throw new AssertionFailedError("onRetrying should not have been called");
        }
        @Override public void onDrain(NonBlockingClient client, Void context) {
            if (throwAssertionFailures) throw new AssertionFailedError("onDrain should not have been called");
        }
    }

    @Test public void testAutoGeneratedClientId() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, new GsonBuilder(), ClientOptions.builder().build(), null, null);
        assertTrue("Expected auto generated client ID to start with string 'AUTO_'", client.getId().startsWith("AUTO_"));
    }

    @Test public void testEndpointServiceReportsFatalFailure() {
        StubEndpointService endpointService = new StubEndpointService() {
            @Override public void lookup(EndpointPromise promise) {
                promise.setFailure(new Exception());
            }
        };
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, new GsonBuilder(), ClientOptions.builder().build(), null, null);
        assertEquals("Client should have transitioned into stopping state, ", ClientState.STOPPING, client.getState());
    }

    @Test
    public void testNullValuesIntoConstructor() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();

        // Specifying null options, listener and context object should not throw an exception
        new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null, null);

        // Specifying a null endpoint service should throw an exception
        try {
            new NonBlockingClientImpl(null, callbackService, component, timerService, null, null, null, null);
            throw new AssertionFailedError("Null endpoint service should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Specifying a null callback service should throw an exception
        try {
            new NonBlockingClientImpl(endpointService, null, component, timerService, null, null, null, null);
            throw new AssertionFailedError("Null callback service should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Specifying a null timer service should throw an exception
        try {
            new NonBlockingClientImpl(endpointService, callbackService, component, null, null, null, null, null);
            throw new AssertionFailedError("Null timer service should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testNullValuesIntoSend() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null, null);

        // Null properties, send options, listener and context object should be okay...
        client.send("topic", "data", (Map<String, Object>)null, null, null, null);
        client.send("topic", ByteBuffer.allocate(1), (Map<String, Object>)null, null, null, null);

        // Null topic should throw an exception
        try {
            client.send(null, "data", (Map<String, Object>)null, null, null, null);
            throw new AssertionFailedError("Null topic (send String) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            client.send(null, ByteBuffer.allocate(1), (Map<String, Object>)null, null, null, null);
            throw new AssertionFailedError("Null topic (send ByteBuffer) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Null data should throw an exception
        // Null topic should throw an exception
        try {
            client.send("topic", (String)null, (Map<String, Object>)null, null, null, null);
            throw new AssertionFailedError("Null data (send String) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            client.send("topic", (ByteBuffer)null, (Map<String, Object>)null, null, null, null);
            throw new AssertionFailedError("Null data (send ByteBuffer) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testMessageTtlValues() {
        SendOptions.builder().setTtl(1).build();

        try {
            SendOptions.builder().setTtl(0).build();
            throw new AssertionFailedError("Zero TTL should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        try {
            SendOptions.builder().setTtl(-1).build();
            throw new AssertionFailedError("-1 TTL should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testMessageTtlValuesIntoSubscribe() {
        class MockClient extends NonBlockingClientImpl {
            private final Queue<Message> messages = new LinkedList<>();

            protected <T> MockClient(EndpointService endpointService,
                    CallbackService callbackService, ComponentImpl engine,
                    TimerService timerService, GsonBuilder builder,
                    ClientOptions options,
                    NonBlockingClientListener<T> listener, T context) {
                super(endpointService, callbackService, engine, timerService,
                        builder, options, listener, context);
            }

            @Override
            public void tell(Message message, Component self) {
                messages.add(message);
            }

            protected java.util.Queue<Message> getMessages() {
                return messages;
            }
        }
        MockClient client = new MockClient(new StubEndpointService(),
                new StubCallbackService(),
                new MockComponent(),
                new StubTimerService(),
                null, null, null, null);

        // expect TTL to be rounded to the nearest second
        final int[] testInputs = {
                1, 500, 999, 1001
        };
        for (final int inputTtl : testInputs) {
            final SubscribeOptions subOptions = SubscribeOptions.builder()
                    .setTtl(inputTtl).build();
            client.subscribe("topicPattern", subOptions,
                    new DestinationAdapter<Object>() {
                    }, null, null);
            assertEquals(
                    "Expected a single message to have been sent to the mock engine component",
                    1, client.getMessages().size());
            final InternalSubscribe<?> subscribe = (InternalSubscribe<?>) client
                    .getMessages().remove();
            assertEquals(
                    "Expected input "
                            + inputTtl
                            + " milliseconds ttl to have been rounded to the nearest second for subscribe - ",
                    (int) Math.round(subOptions.getTtl() / 1000.0),
                    subscribe.ttl);
        }
    }

    @Test
    public void testNullValuesIntoSubscribe() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null, null);

        // Null subscription options, completion listener, and context should be fine...
        client.subscribe("topicPattern", null, new DestinationAdapter<Object>(){}, null, null);

        // Null topic pattern should throw an exception
        try {
            client.subscribe(null, null, new DestinationAdapter<Object>(){}, null, null);
            throw new AssertionFailedError("Null topic pattern should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Null destination listener should throw an exception
        try {
            client.subscribe("topicPattern", null, null, null, null);
            throw new AssertionFailedError("Null destination listener should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testNullValuesIntoUnsubscribe() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null, null);

        // Null share, listener and context object should be fine...
        client.unsubscribe("topicPattern", null, 0, null, null);
        client.unsubscribe("topicPattern", null, null, null);

        // Null topic pattern should throw an exception
        try {
            client.unsubscribe(null, null, 0, null, null);
            throw new AssertionFailedError("Null topic pattern should have thrown an exception (1)");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            client.unsubscribe(null, null, null, null);
            throw new AssertionFailedError("Null topic pattern should have thrown an exception (2)");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testInvalidShareNameIntoUnsubscribe() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null, null);

        // Null share, listener and context object should be fine...
        client.unsubscribe("topicPattern", null, 0, null, null);
        client.unsubscribe("topicPattern", null, null, null);

        // Share name containing colon should throw an exception
        try {
            client.unsubscribe("topic", "sha:re", 0, null, null);
            throw new AssertionFailedError("Null topic pattern should have thrown an exception (1)");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        // Null share name is still fine though...
        client.unsubscribe("topic", null, null, null);

    }

    @Test
    public void testNonzeroTtlIntoUnsubscribe() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null, null);

        try {
            client.unsubscribe("topicPattern", null, 7, null, null);
            throw new AssertionFailedError("Non-zero ttl should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testTopicEncoding() {
        String[][] testData = new String[][] {
            {"", "amqp:///"},
            {"/", "amqp:////"},
            {"/kittens", "amqp:////kittens"},
            {"kittens", "amqp:///kittens"},
            {"kittens/puppies", "amqp:///kittens/puppies"},
            {"kittens/puppies/", "amqp:///kittens/puppies/"},
            {"/kittens/puppies", "amqp:////kittens/puppies"},
            {"/kittens/puppies/", "amqp:////kittens/puppies/"},
            {"&", "amqp:///%26"},
            {"/&", "amqp:////%26"},
            {"&/", "amqp:///%26/"},
            {"/kittens&", "amqp:////kittens%26"},
            {"/kit&tens", "amqp:////kit%26tens"},
            {"/&kittens", "amqp:////%26kittens"},
            {"&/kittens", "amqp:///%26/kittens"},
            {"&/&kit&tens&/&pup&pies&/&", "amqp:///%26/%26kit%26tens%26/%26pup%26pies%26/%26"},
            {"!£$%^&*()_+-=|,./<> ?@~;'#{}[]", "amqp:///!%C2%A3%24%25%5E%26*()_%2B-%3D%7C%2C./%3C%3E%20%3F%40~%3B'%23%7B%7D%5B%5D"}
        };
        for (int i = 0; i < testData.length; ++i) {
            assertEquals("test case #"+i, testData[i][1], NonBlockingClientImpl.encodeTopic(testData[i][0]));
        }
    }

    @Test
    public void testRoundtripMessageProperties() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        class MockClient extends NonBlockingClientImpl {
            private final LinkedList<Message> messages = new LinkedList<>();
            protected <T> MockClient(EndpointService endpointService,
                    CallbackService callbackService, ComponentImpl engine,
                    TimerService timerService, GsonBuilder builder, ClientOptions options,
                    NonBlockingClientListener<T> listener, T context) {
                super(endpointService, callbackService, engine, timerService, builder, options,
                        listener, context);
            }
            @Override
            public void tell(Message message, Component self) {
                messages.addLast(message);
            }
            protected java.util.List<Message> getMessages() {
                return messages;
            }
        }
        class TestDestinationListener implements DestinationListener<Void> {
            protected Map<String, Object> properties = null;
            @Override
            public void onMessage(NonBlockingClient client, Void context, Delivery delivery) {
                properties = delivery.getProperties();
            }
            @Override
            public void onMalformed(NonBlockingClient client, Void context, MalformedDelivery delivery) {
            }
            @Override
            public void onUnsubscribed(NonBlockingClient client, Void context, String topicPattern, String share, Exception error) {
            }
        }

        MockClient client = new MockClient(endpointService, callbackService, component, timerService, null, null, null, null);
        HashMap<String, Object> props = new HashMap<>();
        //boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, byte[].class, String.class
        props.put("boolean", true);
        props.put("byte", (byte)0x01);
        props.put("short", (short)123);
        props.put("int", 4567);
        props.put("long", (long)121723);
        props.put("float", (float)0.1234);
        props.put("double", 543.1234);
        props.put("byte[]", new byte[]{1,2,3,4});
        props.put("Byte[]", new Byte[]{1,2,3,4});
        props.put("string", "this is a string");

        client.send("/kittens", "data", props);

        assertEquals("Expected a single message to have been sent to the mock engine component", 1, client.getMessages().size());
        InternalSend<?> send = (InternalSend<?>)client.getMessages().get(0);
        byte[] data = new byte[send.length];
        System.arraycopy(send.buf.array(), 0, data, 0, send.length);
        ByteBuf msgData = io.netty.buffer.Unpooled.wrappedBuffer(data);

        DeliveryRequest dr = new DeliveryRequest(msgData, QOS.AT_MOST_ONCE, "/kittens", null, null);
        TestDestinationListener destinationListener = new TestDestinationListener();
        DestinationListenerWrapper<Void> wrapper = new DestinationListenerWrapper<>(client, new GsonBuilder(), destinationListener, null);
        wrapper.onDelivery(new SameThreadCallbackService(), dr, QOS.AT_MOST_ONCE, false);

        assertNotNull("Expected onMessage to have been called with message properties", destinationListener.properties);
        assertEquals("Expected all message properties to have been round-tripped", props.size(), destinationListener.properties.size());
        Map<String, Object> actualProperties = destinationListener.properties;
        for (Map.Entry<String, Object> expectedProperty : props.entrySet()) {
            assertTrue("Round-tripped properties should have contained key: "+expectedProperty.getKey(), actualProperties.containsKey(expectedProperty.getKey()));
            if (expectedProperty.getValue() instanceof Byte[]) {
                final Byte[] expected = (Byte[]) expectedProperty.getValue();
                final byte[] actual = (byte[]) actualProperties.get(expectedProperty.getKey());
                for (int i = 0; i < expected.length; i++) {
                    assertTrue("Round-tripped Byte array should match for key: "+expectedProperty.getKey(),
                            expected[i].byteValue() == actual[i]);
                }
            } else if (expectedProperty.getValue() instanceof byte[]) {
                assertTrue("Round-tripped byte array should match for key: "+expectedProperty.getKey(),
                        Arrays.equals((byte[])expectedProperty.getValue(), (byte[])actualProperties.get(expectedProperty.getKey())));
            } else {
                assertEquals("Ronnd-tripped value should match for key: "+expectedProperty.getKey(), expectedProperty.getValue(), actualProperties.get(expectedProperty.getKey()));
            }
        }
    }

    @Test
    public void testValidPropertyValues() {
        assertTrue("null", NonBlockingClientImpl.isValidPropertyValue(null));
        assertTrue("boolean", NonBlockingClientImpl.isValidPropertyValue(false));
        assertTrue("byte", NonBlockingClientImpl.isValidPropertyValue((byte)3));
        assertTrue("short", NonBlockingClientImpl.isValidPropertyValue((short)3));
        assertTrue("int", NonBlockingClientImpl.isValidPropertyValue(3));
        assertTrue("long", NonBlockingClientImpl.isValidPropertyValue(3L));
        assertTrue("float", NonBlockingClientImpl.isValidPropertyValue((float)3.0));
        assertTrue("double", NonBlockingClientImpl.isValidPropertyValue(3.0));
        assertTrue("byte[]", NonBlockingClientImpl.isValidPropertyValue(new byte[0]));
        assertTrue("Byte[]", NonBlockingClientImpl.isValidPropertyValue(new Byte[0]));
        assertTrue("string", NonBlockingClientImpl.isValidPropertyValue("hello"));

        assertFalse("Object", NonBlockingClientImpl.isValidPropertyValue(new Object()));
        assertFalse("char", NonBlockingClientImpl.isValidPropertyValue('c'));
        assertFalse("BigDecimal", NonBlockingClientImpl.isValidPropertyValue(new BigDecimal(3)));
    }

    private Map<String, Object> getPropertiesMap(final String key, final Object value) {
        return new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put(key, value);
            }
        };
    }

    @Test
    public void testValidPropertyValuesInSend() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null, null);

        // valid
        client.send("topic", "data", getPropertiesMap("null", null));
        client.send("topic", "data", getPropertiesMap("boolean", false));
        client.send("topic", "data", getPropertiesMap("byte", (byte)3));
        client.send("topic", "data", getPropertiesMap("short", (short)3));
        client.send("topic", "data", getPropertiesMap("int", 3));
        client.send("topic", "data", getPropertiesMap("long", 3L));
        client.send("topic", "data", getPropertiesMap("float", (float)3.0));
        client.send("topic", "data", getPropertiesMap("double", 3.0));
        client.send("topic", "data", getPropertiesMap("byte[]", new byte[0]));
        client.send("topic", "data", getPropertiesMap("Byte[]", new Byte[0]));
        client.send("topic", "data", getPropertiesMap("string", "hello"));

        // invalid
        try {
            client.send("topic", "data", getPropertiesMap("Object", new Object()));
            throw new AssertionFailedError("Object property value should have thrown an Exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            client.send("topic", "data", getPropertiesMap("char", 'c'));
            throw new AssertionFailedError("char property value should have thrown an Exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            client.send("topic", "data", getPropertiesMap("BigDecimal", new BigDecimal(3)));
            throw new AssertionFailedError("BigDecimal property value should have thrown an Exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testEndpointServiceFailureStopsClient() {
        final ClientException expectedException = new ClientException("badness!");

        class BadEndpointService implements EndpointService {
            @Override
            public void lookup(EndpointPromise promise) {
                promise.setFailure(expectedException);
            }
            @Override
            public void onSuccess(Endpoint endpoint) {
                throw new AssertionFailedError("Should not have been called!");
            }
        }

        class TestClientListener extends MockNonBlockingClientListener {
            private ClientException actualException;
            public TestClientListener() {
                super(true);
            }
            @Override
            public void onStopped(NonBlockingClient client, Void context, ClientException exception) {
                actualException = exception;
            }
        }

        TestClientListener listener = new TestClientListener();

        NonBlockingClientImpl client =
                new NonBlockingClientImpl(new BadEndpointService(), new SameThreadCallbackService(), new MockComponent(), new StubTimerService(), null, null, listener, null);

        assertEquals(ClientState.STOPPED, client.getState());
        assertSame(expectedException, listener.actualException);
    }

    @Test
    public void testEndpointServiceRetry() {
        class RetryEndpointService implements EndpointService {
            boolean first = true;
            @Override
            public void lookup(EndpointPromise promise) {
                if (first) {
                    first = false;
                    promise.setWait(1000);
                } else {
                    promise.setSuccess(new StubEndpoint());
                }
            }
            @Override
            public void onSuccess(Endpoint endpoint) {
                throw new AssertionFailedError("Should not have been called!");
            }
        }

        class TestTimerService implements TimerService {
            long lastDelay = -1;
            @Override
            public void schedule(long delay, Promise<Void> promise) {
                lastDelay = delay;
                promise.setSuccess(null);
            }
            @Override
            public void cancel(Promise<Void> promise) { promise.setSuccess(null);}
        }

        class TestClientListener extends MockNonBlockingClientListener {
            private boolean onRetryingCalled = false;
            public TestClientListener() {
                super(true);
            }
            @Override
            public void onRetrying(NonBlockingClient client, Void context, ClientException exception) {
                onRetryingCalled = true;
            }
        }

        TestTimerService timer = new TestTimerService();
        TestClientListener listener = new TestClientListener();

        NonBlockingClientImpl client =
                new NonBlockingClientImpl(new RetryEndpointService(), new SameThreadCallbackService(), new MockComponent(), timer, null, null, listener, null);

        assertEquals(ClientState.RETRYING, client.getState());
        assertTrue(listener.onRetryingCalled);
        assertEquals(1000, timer.lastDelay);
    }

    private NonBlockingClientImpl openCommon(MockComponent engine, MockNonBlockingClientListener listener) {
        NonBlockingClientImpl client =
                new NonBlockingClientImpl(new MockEndpointService(), new SameThreadCallbackService(), engine, new MockTimerService(), null, null, listener, null);
        assertEquals(ClientState.STARTING, client.getState());
        assertEquals(1, engine.getMessages().size());
        assertTrue(engine.getMessages().get(0) instanceof OpenRequest);
        return client;
    }

    @Test
    public void testOpenSuccess() {
        class TestClientListener extends MockNonBlockingClientListener {
            public TestClientListener() { super(true); }
            @Override public void onStarted(NonBlockingClient client, Void context) {}
        }
        MockComponent engine = new MockComponent();
        TestClientListener listener = new TestClientListener();
        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, new EngineConnection()), engine);
        assertEquals(ClientState.STARTED, client.getState());
    }

    @Test
    public void testOpenRetryableFailure() {
        class TestClientListener extends MockNonBlockingClientListener {
            public TestClientListener() { super(true); }
            @Override public void onRetrying(NonBlockingClient client, Void context, ClientException exception) {}
        }
        MockComponent engine = new MockComponent();
        TestClientListener listener = new TestClientListener();
        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, new ClientException("too bad!")), engine);
        assertEquals(ClientState.RETRYING, client.getState());
    }

    private NonBlockingClientImpl stoppedClient() {
        class TestClientListener extends MockNonBlockingClientListener {
            public TestClientListener() { super(true); }
            @Override public void onStopped(NonBlockingClient client, Void context, ClientException exception) {}
        }
        MockComponent engine = new MockComponent();
        TestClientListener listener = new TestClientListener();
        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, new com.ibm.mqlight.api.SecurityException("sasl")), engine);
        assertEquals(ClientState.STOPPED, client.getState());
        return client;
    }

    @Test
    public void testOpenFatalFailure() {
        stoppedClient();
    }

    @Test(expected=StoppedException.class)
    public void testSendWhileStoppedThrowsException() {
        NonBlockingClientImpl client = stoppedClient();
        client.send("/kittens", "data", null);
    }

    @Test(expected=StoppedException.class)
    public void testSubscribeWhileStoppedThrowsException() {
        NonBlockingClientImpl client = stoppedClient();
        client.subscribe("/kittens", new DestinationAdapter<Object>() {}, null, null);
    }

    @Test(expected=StoppedException.class)
    public void testUnsubscribeWhileStoppedThrowsException() {
        NonBlockingClientImpl client = stoppedClient();
        client.unsubscribe("/kittens", null, null);
    }

    @Test
    public void testSubscribeUnsubscribe() {
        class TestClientListener extends MockNonBlockingClientListener {
            public TestClientListener() { super(true); }
            @Override public void onStarted(NonBlockingClient client, Void context) {}
        }
        MockComponent engine = new MockComponent();
        TestClientListener listener = new TestClientListener();
        EngineConnection engineConnection = new EngineConnection();

        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, engineConnection), engine);
        assertEquals(ClientState.STARTED, client.getState());

        class TestDestinationAdapter extends DestinationAdapter<Void> {
            private boolean onUnsubscribeCalled;
            private String unsubscribeTopicPattern;
            private String unsubscribeShare;
            private Exception unsubscribedError;
            @Override
            public void onUnsubscribed(NonBlockingClient client, Void context, String topicPattern, String share, Exception error) {
                onUnsubscribeCalled = true;
                unsubscribeTopicPattern = topicPattern;
                unsubscribeShare = share;
                unsubscribedError = error;
            }
        }
        TestDestinationAdapter destAdapter = new TestDestinationAdapter();
        client.subscribe("/kittens", destAdapter, null, null);
        assertEquals(2, engine.getMessages().size());
        assertTrue(engine.getMessages().get(1) instanceof SubscribeRequest);
        SubscribeRequest subRequest = (SubscribeRequest)engine.getMessages().get(1);
        assertEquals("private:/kittens", subRequest.topic);
        client.tell(new SubscribeResponse(engineConnection, "private:/kittens"), engine);

        // Subscribing again should give an error
        try {
          client.subscribe("/kittens", destAdapter, null, null);
          fail("expected a SubscribedException");
        } catch (SubscribedException e) {
        }
        
        client.unsubscribe("/kittens", null, null);
        assertEquals(3, engine.getMessages().size());
        assertTrue(engine.getMessages().get(2) instanceof UnsubscribeRequest);
        client.tell(new UnsubscribeResponse(engineConnection, "private:/kittens", null), engine);

        assertTrue(destAdapter.onUnsubscribeCalled);
        assertEquals("/kittens", destAdapter.unsubscribeTopicPattern);
        assertEquals(null, destAdapter.unsubscribeShare);
        assertEquals(null, destAdapter.unsubscribedError);
        
        // unsubscribing again should give an error
        try {
          client.unsubscribe("/kittens", null, null);
          fail("expected an UnsubscribedException");
        } catch (UnsubscribedException e) {
        }
    }
        
    private class MockCompletionListener implements CompletionListener<Object> {
        protected boolean onSuccessCalled = false;
        protected boolean onErrorCalled = false;
        protected Exception onErrorException = null;

        @Override
        public void onSuccess(NonBlockingClient client, Object context) {
          onSuccessCalled = true;
        }

        @Override
        public void onError(NonBlockingClient client, Object context, Exception exception) {
          onErrorCalled = true;
          onErrorException = exception;
        }
    }
    
    @Test
    public void testStopFailsSubscribe() {
        MockComponent engine = new MockComponent();
        MockNonBlockingClientListener listener = new MockNonBlockingClientListener(false);
        EngineConnection engineConnection = new EngineConnection();

        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, engineConnection), engine);
        assertEquals(ClientState.STARTED, client.getState());

        MockCompletionListener inflightListener = new MockCompletionListener();
        client.subscribe("inflight/kittens", new DestinationAdapter<Object>() {}, inflightListener, null);
        
        client.tell(new DisconnectNotification(engineConnection, new ClientException("you got disconnected!")), engine);
        assertEquals(ClientState.RETRYING, client.getState());

        MockCompletionListener queuedListener = new MockCompletionListener();
        client.subscribe("queued/kittens", new DestinationAdapter<Object>() {}, queuedListener, null);

        client.stop(null, null);
        assertEquals(3, engine.getMessages().size());
        assertTrue(engine.getMessages().get(2) instanceof OpenRequest);
        openRequest = (OpenRequest)engine.getMessages().get(2);
        client.tell(new OpenResponse(openRequest, new ClientException("")), engine);
        assertEquals(ClientState.STOPPED, client.getState());

        assertFalse(inflightListener.onSuccessCalled);
        assertTrue(inflightListener.onErrorCalled);
        assertTrue(inflightListener.onErrorException instanceof StoppedException);
     
        assertFalse(queuedListener.onSuccessCalled);
        assertTrue(queuedListener.onErrorCalled);
        assertTrue(queuedListener.onErrorException instanceof StoppedException);        
    }

    @Test
    public void testSendSucceeds() {
        class TestClientListener extends MockNonBlockingClientListener {
            public TestClientListener() { super(true); }
            @Override public void onStarted(NonBlockingClient client, Void context) {}
        }
        MockComponent engine = new MockComponent();
        TestClientListener listener = new TestClientListener();
        EngineConnection engineConnection = new EngineConnection();

        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, engineConnection), engine);
        assertEquals(ClientState.STARTED, client.getState());

        MockCompletionListener compListener = new MockCompletionListener();
        client.send("/kittens", "data", null, compListener, null);
        assertEquals(2, engine.getMessages().size());
        assertTrue(engine.getMessages().get(1) instanceof SendRequest);
        SendRequest sendRequest = (SendRequest)engine.getMessages().get(1);

        client.tell(new SendResponse(sendRequest, null), engine);
        assertTrue("Completion listener for send should have been called", compListener.onSuccessCalled);
    }

    @Test
    public void testSendFails() {
        class TestClientListener extends MockNonBlockingClientListener {
            public TestClientListener() { super(true); }
            @Override public void onStarted(NonBlockingClient client, Void context) {}
        }
        MockComponent engine = new MockComponent();
        TestClientListener listener = new TestClientListener();
        EngineConnection engineConnection = new EngineConnection();

        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, engineConnection), engine);
        assertEquals(ClientState.STARTED, client.getState());

        MockCompletionListener compListener = new MockCompletionListener();
        client.send("/kittens", "data", null, compListener, null);
        assertEquals(2, engine.getMessages().size());
        assertTrue(engine.getMessages().get(1) instanceof SendRequest);
        SendRequest sendRequest = (SendRequest)engine.getMessages().get(1);

        final Exception exception = new Exception("something nasty, I'm sure");
        client.tell(new SendResponse(sendRequest, exception), engine);
        assertTrue("Completion listener for send should have been called", compListener.onErrorCalled);
       assertEquals("Exception passed to completion listener should match", exception, compListener.onErrorException);
    }

    @Test
    public void testThrowingExceptionInCallbackStopsClient() {
        final RuntimeException exception = new RuntimeException("");
        class TestClientListener extends MockNonBlockingClientListener {
            ClientException onStoppedException = null;
            public TestClientListener() { super(true); }
            @Override public void onStarted(NonBlockingClient client, Void context) {
                throw exception;
            }
            @Override
            public void onStopped(NonBlockingClient client, Void context, ClientException exception) {
                onStoppedException = exception;
            }
        }
        MockComponent engine = new MockComponent();
        TestClientListener listener = new TestClientListener();
        EngineConnection engineConnection = new EngineConnection();

        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, engineConnection), engine);
        assertEquals(ClientState.STOPPING, client.getState());

        assertEquals(2, engine.getMessages().size());
        assertTrue(engine.getMessages().get(1) instanceof CloseRequest);
        CloseRequest closeRequest = (CloseRequest)engine.getMessages().get(1);
        client.tell(new CloseResponse(closeRequest), engine);
        assertEquals(ClientState.STOPPED, client.getState());

        assertEquals("Expected exception to have been propagated to onStopped", exception, listener.onStoppedException.getCause());
    }

    @Test
    public void testCrackLinkName() {
        String[] results = NonBlockingClientImpl.crackLinkName("private:/kittens");
        assertEquals(2, results.length);
        assertEquals("/kittens", results[0]);
        assertTrue(results[1] == null);

        results = NonBlockingClientImpl.crackLinkName("share:sharename:/puppies");
        assertEquals(2, results.length);
        assertEquals("/puppies", results[0]);
        assertEquals("sharename", results[1]);
    }

    @Test
    public void testStopFailsSends() {
        MockComponent engine = new MockComponent();
        MockNonBlockingClientListener listener = new MockNonBlockingClientListener(false);
        EngineConnection engineConnection = new EngineConnection();

        NonBlockingClientImpl client = openCommon(engine, listener);
        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, engineConnection), engine);
        assertEquals(ClientState.STARTED, client.getState());

        MockCompletionListener inflightQos0Listener = new MockCompletionListener();
        client.send("/inflight/qos0", "data", null, SendOptions.builder().setQos(QOS.AT_MOST_ONCE).build(), inflightQos0Listener, null);
        MockCompletionListener inflightQos1Listener = new MockCompletionListener();
        client.send("/inflight/qos1", "data", null, SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).build(), inflightQos1Listener, null);

        client.tell(new DisconnectNotification(engineConnection, new ClientException("you got disconnected!")), engine);
        assertEquals(ClientState.RETRYING, client.getState());

        MockCompletionListener queuedQos0Listener = new MockCompletionListener();
        client.send("/queued/qos0", "data", null, SendOptions.builder().setQos(QOS.AT_MOST_ONCE).build(), queuedQos0Listener, null);
        MockCompletionListener queuedQos1Listener = new MockCompletionListener();
        client.send("/queued/qos1", "data", null, SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).build(), queuedQos1Listener, null);

        client.stop(null, null);
        assertEquals(4, engine.getMessages().size());
        assertTrue(engine.getMessages().get(3) instanceof OpenRequest);
        openRequest = (OpenRequest)engine.getMessages().get(3);
        client.tell(new OpenResponse(openRequest, new ClientException("")), engine);
        assertEquals(ClientState.STOPPED, client.getState());

        // If the client is stopped with messages in-flight (e.g. potentially sent to the server) then expect:
        // a) QOS 0 to be marked as success (as the client delivers them 'at most once' and so is optimistic...)
        // b) QOS 1 to be marked as failure (as the client delivers them 'at least once' and so is pessimistic...)
        assertTrue(inflightQos0Listener.onSuccessCalled);
        assertNull(inflightQos0Listener.onErrorException);
        assertTrue(inflightQos1Listener.onErrorCalled);
        assertTrue(inflightQos1Listener.onErrorException instanceof StoppedException);

        // If the client has queued (but never attempted to send) a message then stopping will always result
        // in the application being notified that the operation failed - as the client can be sure that the
        // message has never been sent.
        assertTrue(queuedQos0Listener.onErrorCalled);
        assertTrue(queuedQos1Listener.onErrorCalled);
        
        assertTrue(queuedQos0Listener.onErrorException instanceof StoppedException);
        assertTrue(queuedQos1Listener.onErrorException instanceof StoppedException);
    }
    
    private org.apache.qpid.proton.message.Message decodeProtonMessage(InternalSend<?> send) {
        org.apache.qpid.proton.message.Message result = Proton.message();
        result.decode(send.buf.array(), 0, send.length);
        return result;
    }

    @Test
    public void testSendPayloads() {

        class MockClient extends NonBlockingClientImpl {

            private final LinkedList<InternalSend<?>> sends = new LinkedList<>();

            protected <T> MockClient(EndpointService endpointService,
                    CallbackService callbackService, ComponentImpl engine,
                    TimerService timerService, GsonBuilder gsonBuilder,
                    ClientOptions options,
                    NonBlockingClientListener<T> listener, T context) {
                super(endpointService, callbackService, engine, timerService, gsonBuilder,
                        options, listener, context);
            }

            @Override
            public void tell(Message message, Component self) {
                if (message instanceof InternalSend<?>) {
                    sends.addLast((InternalSend<?>)message);
                }
                super.tell(message, self);
            }
        }
        MockComponent engine = new MockComponent();
        EngineConnection engineConnection = new EngineConnection();

        MockClient client =
                new MockClient(new MockEndpointService(), new SameThreadCallbackService(), engine, new MockTimerService(), null, null, null, null);
        assertEquals(ClientState.STARTING, client.getState());
        assertEquals(1, engine.getMessages().size());
        assertTrue(engine.getMessages().get(0) instanceof OpenRequest);

        OpenRequest openRequest = (OpenRequest)engine.getMessages().get(0);
        client.tell(new OpenResponse(openRequest, engineConnection), engine);
        assertEquals(ClientState.STARTED, client.getState());

        String expectedTopic = "/kittens";
        byte[] expectedBytes = new byte[] {1, 2, 3};
        Object expectedJsonObject = new int[] { 1, 2, 3 };
        String expectedRawJson = "this doesn't even need to be valid JSON!";
        String expectedStringData = "some data";

        client.send(expectedTopic, ByteBuffer.wrap(expectedBytes), (Map<String, Object>)null, null, null, null);
        org.apache.qpid.proton.message.Message msg = decodeProtonMessage(client.sends.get(0));
        assertEquals("Message 1: topic doesn't match", "amqp:///" + expectedTopic, msg.getAddress());
        assertNull("Message 1: content type should not have been set", msg.getContentType());
        assertArrayEquals("Message 1: body doesn't match", expectedBytes, ((Binary)((AmqpValue)msg.getBody()).getValue()).getArray());

        client.send(expectedTopic, expectedJsonObject, (Map<String, Object>)null, null, null, null);
        msg = decodeProtonMessage(client.sends.get(1));
        assertEquals("Message 2: topic doesn't match", "amqp:///" + expectedTopic, msg.getAddress());
        assertEquals("Message 2: content type set incorrectly", "application/json", msg.getContentType());
        assertEquals("Message 2: body doesn't match", "[1,2,3]", ((AmqpValue)msg.getBody()).getValue());

        client.send(expectedTopic, expectedStringData, (Map<String, Object>)null, null, null, null);
        msg = decodeProtonMessage(client.sends.get(2));
        assertEquals("Message 3: topic doesn't match", "amqp:///" + expectedTopic, msg.getAddress());
        assertNull("Message 3: content type should not have been set", msg.getContentType());
        assertEquals("Message 3: body doesn't match", expectedStringData, ((AmqpValue)msg.getBody()).getValue());

        client.send(expectedTopic, expectedJsonObject, expectedJsonObject.getClass().getGenericSuperclass(), null, null, null, null);
        msg = decodeProtonMessage(client.sends.get(3));
        assertEquals("Message 4: topic doesn't match", "amqp:///" + expectedTopic, msg.getAddress());
        assertEquals("Message 4: content type set incorrectly", "application/json", msg.getContentType());
        assertEquals("Message 4: body doesn't match", "[1,2,3]", ((AmqpValue)msg.getBody()).getValue());

        client.sendJson(expectedTopic, expectedRawJson, null, null, null, null);
        msg = decodeProtonMessage(client.sends.get(4));
        assertEquals("Message 5: topic doesn't match", "amqp:///" + expectedTopic, msg.getAddress());
        assertEquals("Message 5: content type set incorrectly", "application/json", msg.getContentType());
        assertEquals("Message 5: body doesn't match", expectedRawJson, ((AmqpValue)msg.getBody()).getValue());
    }   
}
