package com.ibm.mqlight.api.impl;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ibm.mqlight.api.JsonDelivery;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;

public class JsonDeliveryImpl extends DeliveryImpl implements JsonDelivery {

    private final String jsonString;
    private final GsonBuilder gsonBuilder;

    private Gson gson;
    private JsonParser jsonParser;

    protected JsonDeliveryImpl(NonBlockingClientImpl client, QOS qos,
                               String share, String topic, String topicPattern, long ttl,
                               String data, GsonBuilder gsonBuilder,
                               Map<String, Object> properties, DeliveryRequest deliveryRequest) {
        super(client, qos, share, topic, topicPattern, ttl, properties, deliveryRequest);
        jsonString = data;
        this.gsonBuilder = gsonBuilder;
    }

    private void init() {
        if (gson == null) {
            gson = gsonBuilder.create();
        }
    }

    @Override
    public synchronized <T> T getData(Class<T> classOfT) throws JsonSyntaxException {
        init();
        return gson.fromJson(jsonString, classOfT);
    }

    @Override
    public <T> T getData(java.lang.reflect.Type typeOfT) {
        init();
        return gson.fromJson(jsonString, typeOfT);
    }

    @Override
    public synchronized JsonElement getData() throws JsonSyntaxException {
        if (jsonParser == null) {
            jsonParser = new JsonParser();
        }
        return jsonParser.parse(jsonString);
    }

    @Override
    public String getRawData() {
        return jsonString;
    }

    @Override
    public Type getType() {
        return Type.JSON;
    }
}
