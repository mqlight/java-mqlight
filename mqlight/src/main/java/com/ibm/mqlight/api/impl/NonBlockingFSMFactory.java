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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashSet;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.StateRepresentation;
import com.github.oxo42.stateless4j.delegates.Action;

class NonBlockingFSMFactory {
    
    private static StateMachineConfig<NonBlockingClientState, NonBlockingClientTrigger> createConfig(final FSMActions actions) {

        Action startTimerAction = new Action() {
            @Override public void doIt() {
                actions.startTimer();
            }
        };
        
        Action cancelTimerAction = new Action() {
            @Override public void doIt() {
                actions.cancelTimer();
            }
        };
        
        Action requestEndpointAction = new Action() {
            @Override public void doIt() {
                actions.requestEndpoint();
            }
        };
        
        Action blessEndpointAction = new Action() {
            @Override public void doIt() {
                actions.blessEndpoint();
            }
        };
        
        Action openConnectionAction = new Action() {
            @Override public void doIt() {
                actions.openConnection();
            }
        };
        
        Action closeConnectionAction = new Action() {
            @Override public void doIt() {
                actions.closeConnection();
            }
        };
        
        Action remakeInboundLinksAction = new Action() {
            @Override public void doIt() {
                actions.remakeInboundLinks();
            }
        };
        
        Action cleanupAction = new Action() {
            @Override public void doIt() {
                actions.cleanup();
            }
        };
        
        Action failPendingStopsAction = new Action() {
            @Override public void doIt() {
                actions.failPendingStops();
            }
        };
        
        Action succeedPendingStopsAction = new Action() {
            @Override public void doIt() {
                actions.succeedPendingStops();
            }
        };
        
        Action failPendingStartAction = new Action() {
            @Override public void doIt() {
                actions.failPendingStarts();
            }
        };
        
        Action succeedPendingStartsAction = new Action() {
            @Override public void doIt() {
                actions.succeedPendingStarts();
            }
        };
        
        Action eventStartingAction = new Action() {
            @Override public void doIt() {
                actions.eventStarting();
            }
        };
        Action eventUserStoppingAction = new Action() {
            @Override public void doIt() {
                actions.eventUserStopping();
            }
        };
        Action eventSystemStoppingAction = new Action() {
            @Override public void doIt() {
                actions.eventSystemStopping();
            }
        };
        Action eventStoppedAction = new Action() {
            @Override public void doIt() {
                actions.eventStopped();
            }
        };
        Action eventStartedAction = new Action() {
            @Override public void doIt() {
                actions.eventStarted();
            }
        };
        Action eventRetryingAction = new Action() {
            @Override public void doIt() {
                actions.eventRetrying();
            }
        };
        Action eventRestartedAction = new Action() {
            @Override public void doIt() {
                actions.eventRestarted();
            }
        };
        Action breakInboundLinksAction = new Action() {
            @Override public void doIt() {
                actions.breakInboundLinks();
            }
        };
        Action processQueuedActionsAction = new Action() {
            @Override public void doIt() {
                actions.processQueuedActions();
            }
        };
        
        StateMachineConfig<NonBlockingClientState, NonBlockingClientTrigger> config = new StateMachineConfig<>();
        
        config.configure(NonBlockingClientState.Retrying1A)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.TIMER_RESP_POP, NonBlockingClientState.Retrying1B)
              .permit(NonBlockingClientTrigger.STOP,  NonBlockingClientState.StoppingR1A)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, startTimerAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, eventRetryingAction);
        
        config.configure(NonBlockingClientState.Retrying1B)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.Retrying1C)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR1C)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.Retrying1A)
              .onEntryFrom(NonBlockingClientTrigger.TIMER_RESP_POP, requestEndpointAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_RETRY, requestEndpointAction);
        
        config.configure(NonBlockingClientState.Retrying1C)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR1E)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.Started)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.Retrying1B)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_OK, openConnectionAction);
              
        config.configure(NonBlockingClientState.Retrying2A)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.Retrying2D)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2C)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.Retrying2B)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.NETWORK_ERROR, requestEndpointAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_RETRY, requestEndpointAction)
              .onEntryFrom(NonBlockingClientTrigger.TIMER_RESP_POP, requestEndpointAction)
              .onEntryFrom(NonBlockingClientTrigger.NETWORK_ERROR, eventRetryingAction)
              .onEntryFrom(NonBlockingClientTrigger.NETWORK_ERROR, breakInboundLinksAction);
        
        config.configure(NonBlockingClientState.Retrying2B)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.Retrying2A)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2E)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.Retrying2C)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_OK, openConnectionAction);
        
        config.configure(NonBlockingClientState.Retrying2C)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.SUBS_REMADE, NonBlockingClientState.Started)
              .permit(NonBlockingClientTrigger.NETWORK_ERROR, NonBlockingClientState.Retrying2A)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2G)
              .permit(NonBlockingClientTrigger.REPLACED, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, remakeInboundLinksAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, blessEndpointAction);

        config.configure(NonBlockingClientState.Retrying2D)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2A)
              .permit(NonBlockingClientTrigger.TIMER_RESP_POP, NonBlockingClientState.Retrying2A)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, startTimerAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, eventRetryingAction);
              
        config.configure(NonBlockingClientState.Started)    
              .permitReentry(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.NETWORK_ERROR, NonBlockingClientState.Retrying2A)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingA)
              .permit(NonBlockingClientTrigger.REPLACED, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, blessEndpointAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, succeedPendingStartsAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, failPendingStopsAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, eventStartedAction)
              .onEntryFrom(NonBlockingClientTrigger.SUBS_REMADE, eventRestartedAction)
              .onEntryFrom(NonBlockingClientTrigger.SUBS_REMADE, processQueuedActionsAction)
              .onEntryFrom(NonBlockingClientTrigger.START, succeedPendingStartsAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, processQueuedActionsAction);
        
        config.configure(NonBlockingClientState.StartingA)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingSA)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.Retrying1A)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.StartingB)
              .onEntryFrom(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE, requestEndpointAction)
              .onEntryFrom(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE, eventStartingAction)
              .onEntryFrom(NonBlockingClientTrigger.START, requestEndpointAction)
              .onEntryFrom(NonBlockingClientTrigger.START, eventStartingAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_RETRY, requestEndpointAction);
        
        config.configure(NonBlockingClientState.StartingB)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.StartingA)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.Started)
              .permit(NonBlockingClientTrigger.STOP,  NonBlockingClientState.StoppingSC)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_OK, openConnectionAction);
        
        config.configure(NonBlockingClientState.Stopped)
              .permitReentry(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StartingA)
              .onEntryFrom(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE, failPendingStartAction)
              .onEntryFrom(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE, succeedPendingStopsAction)
              .onEntryFrom(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE, eventStoppedAction)
              .onEntryFrom(NonBlockingClientTrigger.STOP, succeedPendingStopsAction);
        
        config.configure(NonBlockingClientState.StoppingA)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingC)
              .permit(NonBlockingClientTrigger.CLOSE_RESP, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.NETWORK_ERROR, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, closeConnectionAction)
              .onEntryFrom(NonBlockingClientTrigger.SUBS_REMADE, closeConnectionAction)
              .onEntryFrom(NonBlockingClientTrigger.STOP, closeConnectionAction)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
              
        config.configure(NonBlockingClientState.StoppingB)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE, NonBlockingClientState.Stopped)
              .onEntryFrom(NonBlockingClientTrigger.CLOSE_RESP, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.NETWORK_ERROR, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_FATAL, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_FATAL, eventSystemStoppingAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_RETRY, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_FATAL, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_FATAL, eventSystemStoppingAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_OK, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.TIMER_RESP_CANCEL, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.TIMER_RESP_POP, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.REPLACED, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.REPLACED, eventSystemStoppingAction);

        config.configure(NonBlockingClientState.StoppingC)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingA)
              .permit(NonBlockingClientTrigger.CLOSE_RESP, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.NETWORK_ERROR, NonBlockingClientState.StoppingD)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_OK, closeConnectionAction)
              .onEntryFrom(NonBlockingClientTrigger.SUBS_REMADE, closeConnectionAction);

        config.configure(NonBlockingClientState.StoppingD)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE, NonBlockingClientState.StartingA)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.CLOSE_RESP, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.NETWORK_ERROR, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_RETRY, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.OPEN_RESP_FATAL, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.TIMER_RESP_CANCEL, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.TIMER_RESP_POP, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_FATAL, cleanupAction)
              .onEntryFrom(NonBlockingClientTrigger.EP_RESP_OK, cleanupAction);

        config.configure(NonBlockingClientState.StoppingR1A)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.TIMER_RESP_POP, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.TIMER_RESP_CANCEL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingR1B)
              .onEntryFrom(NonBlockingClientTrigger.STOP, cancelTimerAction)  // TODO: cancel timer needs to deal with duplicate cancels!
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingR1B)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR1A)
              .permit(NonBlockingClientTrigger.TIMER_RESP_CANCEL, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.TIMER_RESP_POP, NonBlockingClientState.StoppingD);
        
        config.configure(NonBlockingClientState.StoppingR1C)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingR1D)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingR1D)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR1C);
        
        config.configure(NonBlockingClientState.StoppingR1E)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.StoppingA)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingR1F)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingR1F)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.StoppingC)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR1E);
        
        config.configure(NonBlockingClientState.StoppingR2A)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingR2B)
              .permit(NonBlockingClientTrigger.TIMER_RESP_CANCEL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.TIMER_RESP_POP, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.STOP, cancelTimerAction)  // TODO: another reason cancel needs to deal with multiple calls...
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingR2B)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2A)
              .permit(NonBlockingClientTrigger.TIMER_RESP_CANCEL, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.TIMER_RESP_POP, NonBlockingClientState.StoppingD);
        
        config.configure(NonBlockingClientState.StoppingR2C)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingR2D)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingR2D)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2C)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.StoppingD);
        
        config.configure(NonBlockingClientState.StoppingR2E)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingR2F)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.StoppingA)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingR2F)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2E)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.StoppingC)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingD);
        
        config.configure(NonBlockingClientState.StoppingR2G)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingR2H)
              .permit(NonBlockingClientTrigger.SUBS_REMADE, NonBlockingClientState.StoppingA)
              .permit(NonBlockingClientTrigger.NETWORK_ERROR, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.REPLACED, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingR2H)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingR2G)
              .permit(NonBlockingClientTrigger.SUBS_REMADE, NonBlockingClientState.StoppingC)
              .permit(NonBlockingClientTrigger.NETWORK_ERROR, NonBlockingClientState.StoppingD);
        
        config.configure(NonBlockingClientState.StoppingSA)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingSB)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingSB)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingSA)
              .permit(NonBlockingClientTrigger.EP_RESP_EXHAUSTED, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.EP_RESP_FATAL, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.EP_RESP_OK, NonBlockingClientState.StoppingD);
        
        config.configure(NonBlockingClientState.StoppingSC)
              .ignore(NonBlockingClientTrigger.STOP)
              .permit(NonBlockingClientTrigger.START, NonBlockingClientState.StoppingSD)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.StoppingA)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingB)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.StoppingB)
              .onEntryFrom(NonBlockingClientTrigger.STOP, eventUserStoppingAction);
        
        config.configure(NonBlockingClientState.StoppingSD)
              .ignore(NonBlockingClientTrigger.START)
              .permit(NonBlockingClientTrigger.STOP, NonBlockingClientState.StoppingSC)
              .permit(NonBlockingClientTrigger.OPEN_RESP_OK, NonBlockingClientState.StoppingC)
              .permit(NonBlockingClientTrigger.OPEN_RESP_FATAL, NonBlockingClientState.StoppingD)
              .permit(NonBlockingClientTrigger.OPEN_RESP_RETRY, NonBlockingClientState.StoppingD);
        
        return config;
    }
    
    public static StateMachine<NonBlockingClientState, NonBlockingClientTrigger> newStateMachine(final FSMActions actions) {
        return new StateMachine<>(NonBlockingClientState.StartingA, createConfig(actions));
    }
    
    private static void generateDotFile(OutputStream dotFile) {
        //OutputStreamWriter w = new OutputStreamWriter(dotFile, "UTF-8")) {
            //PrintWriter writer = new PrintWriter(w);
            //writer.write("digraph G {\n");
        System.out.println("digraph G {");
        final HashSet<String> invokedMethods = new HashSet<String>();
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                invokedMethods.add(method.getName());
                return null;
            }
        };
        FSMActions actions = (FSMActions) Proxy.newProxyInstance(FSMActions.class.getClassLoader(), new Class[] {FSMActions.class}, handler);

        
        StateMachineConfig<NonBlockingClientState, NonBlockingClientTrigger> smConfig = createConfig(actions);
        
        for (NonBlockingClientState state : EnumSet.allOf(NonBlockingClientState.class)) {
            StateRepresentation<NonBlockingClientState, NonBlockingClientTrigger> rep = smConfig.getRepresentation(state);
            //System.out.println(rep.getUnderlyingState());
            
            for (NonBlockingClientTrigger trigger : rep.getPermittedTriggers()) {
                StateMachine<NonBlockingClientState, NonBlockingClientTrigger> sm = new StateMachine<NonBlockingClientState, NonBlockingClientTrigger>(rep.getUnderlyingState(), smConfig);
                sm.fire(trigger);
                System.out.print("\t" + rep.getUnderlyingState() + " -> " + sm.getState() + "[ label = \"" + trigger);
                if (!invokedMethods.isEmpty()) {
                    System.out.print(" |");
                    for (String methodName : invokedMethods) {
                        System.out.print(" " + methodName);
                    }
                    invokedMethods.clear();
                }
                System.out.println("\" ];");
            }
            
            
        }
            
        System.out.println("}");
           // writer.write("}");
        //}
    }
    public static void main(String[] args) throws IOException {
      LogbackLogging.stop();

        //FileOutputStream dotFile = new FileOutputStream("statemachine.dot");
        generateDotFile(null);

        //createConfig(null).generateDotFileInto(fos);
        //fos.close();
    }
}
