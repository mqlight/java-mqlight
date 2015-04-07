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

import com.ibm.mqlight.api.impl.network.NettyNetworkService;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class SubscriptionTopic {

  private static final Logger logger = LoggerFactory.getLogger(NettyNetworkService.class);
  
  private final String topic;
  
  public SubscriptionTopic(String topicPattern, String shareName) {
    final String methodName = "<init>";
    logger.entry(this, methodName, topicPattern, shareName);

    String subTopic;
    if (shareName == null || "".equals(shareName)) {
        subTopic = "private:" + topicPattern;
    } else {
        if (shareName.contains(":")) {
          final IllegalArgumentException exception = new IllegalArgumentException("Share name cannot contain a colon (:) character");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        subTopic = "share:" + shareName + ":" + topicPattern;
    }
    topic = subTopic;
    
    logger.exit(this, methodName);
  }
  
  public SubscriptionTopic(String topic) {
    final String methodName = "<init>";
    logger.entry(this, methodName, topic);
    this.topic = topic;
    logger.exit(this, methodName);
  }

  public String getTopic() {
    return topic;
  }

  public String[] split() {
    final String methodName = "split";
    logger.entry(methodName);

    String topicPattern;
    String share;
    if (topic.startsWith("share:")) {
        share = topic.substring("share:".length());
        topicPattern = share.substring(share.indexOf(':')+1);
        share = share.substring(0, share.indexOf(':'));
    } else {
        topicPattern = topic.substring("private:".length());
        share = null;
    }

    final String [] result = new String[] {topicPattern, share};

    logger.exit(methodName, result);

    return result;
}
  
  public String toString() {
    return topic;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((topic == null) ? 0 : topic.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SubscriptionTopic other = (SubscriptionTopic) obj;
    if (topic == null) {
      if (other.topic != null) return false;
    } else if (!topic.equals(other.topic)) return false;
    return true;
  }
}
