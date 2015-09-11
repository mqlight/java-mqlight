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
package com.ibm.mqlight.api;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * A sub-type of delivery that is used to represent JSON data being received
 * by the client.
 */
public interface JsonDelivery {

    /**
     * Deserializes the JSON data into an object of the specified class.  Use the {@link JsonDelivery#getData(Type)}
     * method instead if the class is of a generic type.  This is equivalent to calling {@link Gson#fromJson(String, Type)}.
     *
     * @param classOfT the class for the desired object.
     * @return an object of type T (or <code>null</code> if the JSON is a representation of null).
     * @throws JsonSyntaxException if the JSON data cannot be deserialized into an object of type <code>classOfT</code>.
     */
    <T> T getData(Class<T> classOfT) throws JsonSyntaxException;

    /**
     * Deserializes the JSON data into an object of the specified type.  Use the {@link JsonDelivery#getData(Class)}
     * method instead if the class is not of a generic type.  This is equivalent to calling {@link Gson#fromJson(String, Type)}.
     *
     * @param typeOfT the type for the desired object.
     * @return an object of type T (or <code>null</code> if the JSON is a representation of null).
     * @throws JsonSyntaxException if the JSON data cannot be deserialized into an object of type <code>typeOfT</code>.
     */
    <T> T getData(Type typeOfT) throws JsonSyntaxException;

    /**
     * @return a {@link JsonElement} representing the deserialized JSON data.  This is equivalent to calling {@link JsonParser#parse(String)}.
     * @throws JsonParseException if the data is not valid JSON.
     * @throws JsonSyntaxException if the data contains malformed JSON elements.
     */
    JsonElement getData() throws JsonParseException;

    /**
     * @return a <code>String</code> representation of the JSON.  This allows other parsers to be used.
     */
    String getRawData();
}
