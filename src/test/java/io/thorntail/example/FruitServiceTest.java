/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.thorntail.example;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@DefaultDeployment
public class FruitServiceTest {
    @Test
    @RunAsClient
    public void listFruits() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/fruits");

        Response response = target.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        JsonArray values = Json.parse(response.readEntity(String.class)).asArray();
        assertTrue(values.size() > 0);
    }

    @Test
    @RunAsClient
    public void fruitById() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/fruits")
                .path("/1"); // fruit by ID

        Response response = target.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, response.getStatus());
        JsonObject value = Json.parse(response.readEntity(String.class)).asObject();
        assertEquals("Cherry", value.get("name").asString());
    }

    @Test
    @RunAsClient
    public void createFruit() {
        createNewFruit("Pineapple");
    }

    private JsonObject createNewFruit(String name) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/fruits");

        Response response = target.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(
                        new Fruit(name),
                        MediaType.APPLICATION_JSON)
                );
        assertEquals(201, response.getStatus());
        JsonObject value = Json.parse(response.readEntity(String.class)).asObject();
        assertEquals(name, value.get("name").asString());
        return value;
    }

    @Test
    @RunAsClient
    public void modifyFruit() {
        JsonObject lemon = createNewFruit("Lemon");
        int id = lemon.get("id").asInt();

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080")
                .path("/api")
                .path("/fruits")
                .path(String.valueOf(id));

        Response response = target.request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(
                        new Fruit("Apricot"),
                        MediaType.APPLICATION_JSON)
                );
        assertEquals(200, response.getStatus());
        JsonObject value = Json.parse(response.readEntity(String.class)).asObject();
        assertEquals("Apricot", value.get("name").asString());
        assertEquals(id, value.get("id").asInt());
    }
}
