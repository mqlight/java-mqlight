/*
 *   <copyright 
 *   notice="oco-source" 
 *   pids="5725-P60" 
 *   years="2015" 
 *   crc="1438874957" > 
 *   IBM Confidential 
 *    
 *   OCO Source Materials 
 *    
 *   5724-H72
 *    
 *   (C) Copyright IBM Corp. 2015
 *    
 *   The source code for the program is not published 
 *   or otherwise divested of its trade secrets, 
 *   irrespective of what has been deposited with the 
 *   U.S. Copyright Office. 
 *   </copyright> 
 */

package com.ibm.mqlight.api.impl;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import junit.framework.AssertionFailedError;

import org.junit.Test;

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
import com.ibm.mqlight.api.StateException;
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

    @Test public void autoGeneratedClientId() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, ClientOptions.builder().build(), null, null);
        assertTrue("Expected auto generated client ID to start with string 'AUTO_'", client.getId().startsWith("AUTO_"));
    }
    
    @Test public void endpointServiceReportsFatalFailure() {
        StubEndpointService endpointService = new StubEndpointService() {
            @Override public void lookup(EndpointPromise promise) {
                promise.setFailure(new Exception());
            }
        };
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, ClientOptions.builder().build(), null, null);
        assertEquals("Client should have transitioned into stopping state, ", ClientState.STOPPING, client.getState());
    }
    
    @Test
    public void nullValuesIntoConstructor() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();

        // Specifying null options, listener and context object should not throw an exception
        new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null);

        // Specifying a null endpoint service should throw an exception
        try {
            new NonBlockingClientImpl(null, callbackService, component, timerService, null, null, null);
            throw new AssertionFailedError("Null endpoint service should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        
        // Specifying a null callback service should throw an exception
        try {
            new NonBlockingClientImpl(endpointService, null, component, timerService, null, null, null);
            throw new AssertionFailedError("Null callback service should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        
        // Specifying a null timer service should throw an exception
        try {
            new NonBlockingClientImpl(endpointService, callbackService, component, null, null, null, null);
            throw new AssertionFailedError("Null timer service should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void nullValuesIntoSend() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null);
        
        // Null properties, send options, listener and context object should be okay...
        client.send("topic", "data", null, null, null, null);
        client.send("topic", ByteBuffer.allocate(1), null, null, null, null);
        
        // Null topic should throw an exception
        try {
            client.send(null, "data", null, null, null, null);
            throw new AssertionFailedError("Null topic (send String) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            client.send(null, ByteBuffer.allocate(1), null, null, null, null);
            throw new AssertionFailedError("Null topic (send ByteBuffer) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        
        // Null data should throw an exception
        // Null topic should throw an exception
        try {
            client.send("topic", (String)null, null, null, null, null);
            throw new AssertionFailedError("Null data (send String) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            client.send("topic", (ByteBuffer)null, null, null, null, null);
            throw new AssertionFailedError("Null data (send ByteBuffer) should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void messageTtlValues() {
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
    public void nullValuesIntoSubscribe() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null);
        
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
    public void nullValuesIntoUnsubscribe() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null);
        
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
    public void nonzeroTtlIntoUnsubscribe() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        NonBlockingClientImpl client = new NonBlockingClientImpl(endpointService, callbackService, component, timerService, null, null, null);
        
        try {
            client.unsubscribe("topicPattern", null, 7, null, null);
            throw new AssertionFailedError("Non-zero ttl should have thrown an exception");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void topicEncoding() {
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
        };
        for (int i = 0; i < testData.length; ++i) {
            assertEquals("test case #"+i, testData[i][1], NonBlockingClientImpl.encodeTopic(testData[i][0]));
        }
    }
    
    @Test
    public void roundtripMessageProperties() {
        StubEndpointService endpointService = new StubEndpointService();
        StubCallbackService callbackService = new StubCallbackService();
        MockComponent component = new MockComponent();
        StubTimerService timerService = new StubTimerService();
        class MockClient extends NonBlockingClientImpl {
            private LinkedList<Message> messages = new LinkedList<>();
            protected <T> MockClient(EndpointService endpointService,
                    CallbackService callbackService, Component engine,
                    TimerService timerService, ClientOptions options,
                    NonBlockingClientListener<T> listener, T context) {
                super(endpointService, callbackService, engine, timerService, options,
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
            public void onUnsubscribed(NonBlockingClient client, Void context, String topicPattern, String share) {
            }
        }
        
        MockClient client = new MockClient(endpointService, callbackService, component, timerService, null, null, null);
        HashMap<String, Object> props = new HashMap<>();
        //boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, byte[].class, String.class
        props.put("boolean", true);
        props.put("byte", (byte)0x01);
        props.put("short", (short)123);
        props.put("int", (int)4567);
        props.put("long", (long)121723);
        props.put("float", (float)0.1234);
        props.put("double", (double)543.1234);
        props.put("byte[]", new byte[]{1,2,3,4});
        props.put("string", "this is a string");
       
        client.send("/kittens", "data", props);

        assertEquals("Expected a single message to have been sent to the mock engine component", 1, client.getMessages().size());
        InternalSend<?> send = (InternalSend<?>)client.getMessages().get(0);
        byte[] data = new byte[send.length];
        System.arraycopy(send.data, 0, data, 0, send.length);

        DeliveryRequest dr = new DeliveryRequest(data, QOS.AT_MOST_ONCE, "/kittens", null, null);
        TestDestinationListener destinationListener = new TestDestinationListener();
        DestinationListenerWrapper<Void> wrapper = new DestinationListenerWrapper<>(client, destinationListener, null);
        wrapper.onDelivery(new SameThreadCallbackService(), dr, QOS.AT_MOST_ONCE, false);
        
        assertNotNull("Expected onMessage to have been called with message properties", destinationListener.properties);
        assertEquals("Expected all message properties to have been round-tripped", props.size(), destinationListener.properties.size());
        Map<String, Object> actualProperties = destinationListener.properties;
        for (Map.Entry<String, Object> expectedProperty : props.entrySet()) {
            assertTrue("Round-tripped properties should have contained key: "+expectedProperty.getKey(), actualProperties.containsKey(expectedProperty.getKey()));
            if (expectedProperty.getValue() instanceof byte[]) {
                assertTrue("Round-tripped byte array should match for key: "+expectedProperty.getKey(), 
                        Arrays.equals((byte[])expectedProperty.getValue(), (byte[])actualProperties.get(expectedProperty.getKey())));
            } else {
                assertEquals("Ronnd-tripped value should match for key: "+expectedProperty.getKey(), expectedProperty.getValue(), actualProperties.get(expectedProperty.getKey()));
            }
        }
    }
    
    @Test
    public void validPropertyValues() {
        assertTrue("null", NonBlockingClientImpl.isValidPropertyValue(null));
        assertTrue("boolean", NonBlockingClientImpl.isValidPropertyValue(false));
        assertTrue("byte", NonBlockingClientImpl.isValidPropertyValue((byte)3));
        assertTrue("short", NonBlockingClientImpl.isValidPropertyValue((short)3));
        assertTrue("int", NonBlockingClientImpl.isValidPropertyValue((int)3));
        assertTrue("long", NonBlockingClientImpl.isValidPropertyValue((long)3L));
        assertTrue("float", NonBlockingClientImpl.isValidPropertyValue((float)3.0));
        assertTrue("double", NonBlockingClientImpl.isValidPropertyValue((double)3.0));
        assertTrue("byte[]", NonBlockingClientImpl.isValidPropertyValue(new byte[0]));
        assertTrue("string", NonBlockingClientImpl.isValidPropertyValue("hello"));
        
        assertFalse("Object", NonBlockingClientImpl.isValidPropertyValue(new Object()));
        assertFalse("char", NonBlockingClientImpl.isValidPropertyValue('c'));
        assertFalse("BigDecimal", NonBlockingClientImpl.isValidPropertyValue(new BigDecimal(3)));
    }

    @Test
    public void endpointServiceFailureStopsClient() {
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
                new NonBlockingClientImpl(new BadEndpointService(), new SameThreadCallbackService(), new MockComponent(), new StubTimerService(), null, listener, null);
        
        assertEquals(ClientState.STOPPED, client.getState());
        assertSame(expectedException, listener.actualException);
    }
    
    @Test
    public void endpointServiceRetry() {
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
                new NonBlockingClientImpl(new RetryEndpointService(), new SameThreadCallbackService(), new MockComponent(), timer, null, listener, null);
        
        assertEquals(ClientState.RETRYING, client.getState());
        assertTrue(listener.onRetryingCalled);
        assertEquals(1000, timer.lastDelay);
    }

    private NonBlockingClientImpl openCommon(MockComponent engine, MockNonBlockingClientListener listener) {
        NonBlockingClientImpl client = 
                new NonBlockingClientImpl(new MockEndpointService(), new SameThreadCallbackService(), engine, new MockTimerService(), null, listener, null);
        assertEquals(ClientState.STARTING, client.getState());
        assertEquals(1, engine.getMessages().size());
        assertTrue(engine.getMessages().get(0) instanceof OpenRequest);
        return client;
    }

    @Test
    public void openSuccess() {
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
    public void openRetryableFailure() {
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
        client.tell(new OpenResponse(openRequest, new ClientException("sasl")), engine);
        assertEquals(ClientState.STOPPED, client.getState());
        return client;
    }
    
    @Test
    public void openFatalFailure() {
        stoppedClient();
    }
    
    @Test(expected=StateException.class)
    public void sendWhileStoppedThrowsException() {
        NonBlockingClientImpl client = stoppedClient();
        client.send("/kittens", "data", null);
    }
    
    @Test(expected=StateException.class)
    public void subscribeWhileStoppedThrowsException() {
        NonBlockingClientImpl client = stoppedClient();
        client.subscribe("/kittens", new DestinationAdapter<Object>() {}, null, null);
    }
    
    @Test(expected=StateException.class)
    public void unsubscribeWhileStoppedThrowsException() {
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
            @Override
            public void onUnsubscribed(NonBlockingClient client, Void context, String topicPattern, String share) {
                onUnsubscribeCalled = true;
                unsubscribeTopicPattern = topicPattern;
                unsubscribeShare = share;
            }
        }
        TestDestinationAdapter destAdapter = new TestDestinationAdapter();
        client.subscribe("/kittens", destAdapter, null, null);
        assertEquals(2, engine.getMessages().size());
        assertTrue(engine.getMessages().get(1) instanceof SubscribeRequest);
        SubscribeRequest subRequest = (SubscribeRequest)engine.getMessages().get(1);
        assertEquals("private:/kittens", subRequest.topic);
        client.tell(new SubscribeResponse(engineConnection, "private:/kittens"), engine);
        
        client.unsubscribe("/kittens", null, null);
        assertEquals(3, engine.getMessages().size());
        assertTrue(engine.getMessages().get(2) instanceof UnsubscribeRequest);
        client.tell(new UnsubscribeResponse(engineConnection, "private:/kittens", false), engine);
        
        assertTrue(destAdapter.onUnsubscribeCalled);
        assertEquals("/kittens", destAdapter.unsubscribeTopicPattern);
        assertEquals(null, destAdapter.unsubscribeShare);
    }
    
    private class MockCompletionListener implements CompletionListener<Void> {
        protected boolean onSuccessCalled = false;
        protected boolean onErrorCalled = false;
        protected Exception onErrorException = null;
        
        @Override
        public void onSuccess(NonBlockingClient client, Void context) {
            onSuccessCalled = true;
        }

        @Override
        public void onError(NonBlockingClient client, Void context, Exception exception) {
            onErrorCalled = true;
            onErrorException = exception;
        }
        
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
    public void throwingExceptionInCallbackStopsClient() {
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
    public void crackLinkName() {
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
        
        client.tell(new DisconnectNotification(engineConnection, "you got", "disconnected!"), engine);
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
        assertTrue(inflightQos1Listener.onErrorCalled);
        
        // If the client has queued (but never attempted to send) a message then stopping will always result
        // in the application being notified that the operation failed - as the client can be sure that the
        // message has never been sent.
        assertTrue(queuedQos0Listener.onErrorCalled);
        assertTrue(queuedQos1Listener.onErrorCalled);
    }
}
