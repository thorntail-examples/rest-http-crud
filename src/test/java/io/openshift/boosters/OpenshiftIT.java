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

package io.openshift.boosters;

import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * @author Heiko Braun
 */
@RunWith(Arquillian.class)
public class OpenshiftIT {

    @RouteURL(value = "${app.name}", path = "/api/fruits")
    @AwaitRoute(path = "/")
    private String url;

    @Before
    public void setup() throws Exception {
        RestAssured.baseURI = url;

        String jsonData = when()
                .get()
                .then()
                .extract().asString();

        JsonArray array = Json.createReader(new StringReader(jsonData)).readArray();
        array.forEach(val -> delete("/" + ((JsonObject) val).getInt("id")));
    }

    @Test
    public void testRetrieveNoFruit() {
        get()
                .then()
                .assertThat().statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void testWithOneFruit() throws Exception {
        createFruit("Peach");

        String payload = get()
                .then()
                .assertThat().statusCode(200)
                .extract().asString();

        JsonArray array = Json.createReader(new StringReader(payload)).readArray();

        assertThat(array).hasSize(1);
        assertThat(array.get(0).getValueType()).isEqualTo(JsonValue.ValueType.OBJECT);

        JsonObject obj = (JsonObject) array.get(0);
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        given()
                .pathParam("fruitId", obj.getInt("id"))
                .when()
                .get("/{fruitId}")
                .then()
                .assertThat().statusCode(200)
                .body(containsString("Peach"));
    }

    @Test
    public void testCreateFruit() {
        String payload = given()
                .contentType(ContentType.JSON)
                .body(convert(Json.createObjectBuilder().add("name", "Raspberry").build()))
                .post()
                .then()
                .assertThat().statusCode(201)
                .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);
        assertThat(obj.getString("name")).isNotNull().isEqualTo("Raspberry");
    }

    @Test
    public void testCreateInvalidPayload() {
        given()
                .contentType(ContentType.TEXT)
                .body("")
                .post()
                .then()
                .assertThat().statusCode(415);
    }

    @Test
    public void testCreateIllegalPayload() {
        Fruit badFruit = new Fruit("Carrot");
        badFruit.setId(2);

        String payload = given()
                .contentType(ContentType.JSON)
                .body(badFruit)
                .post()
                .then()
                .assertThat().statusCode(422)
                .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void testUpdate() throws Exception {
        Fruit pear = createFruit("Pear");

        String response = given()
                .pathParam("fruitId", pear.getId())
                .when()
                .get("/{fruitId}")
                .then()
                .assertThat().statusCode(200)
                .extract().asString();

        pear = new ObjectMapper().readValue(response, Fruit.class);

        pear.setName("Not Pear");

        response = given()
                .pathParam("fruitId", pear.getId())
                .contentType(ContentType.JSON)
                .body(new ObjectMapper().writeValueAsString(pear))
                .when()
                .put("/{fruitId}")
                .then()
                .assertThat().statusCode(200)
                .extract().asString();

        Fruit updatedPear = new ObjectMapper().readValue(response, Fruit.class);

        assertThat(pear.getId()).isEqualTo(updatedPear.getId());
        assertThat(updatedPear.getName()).isEqualTo("Not Pear");
    }

    @Test
    public void testUpdateWithUnknownId() throws Exception {
        Fruit bad = new Fruit("bad");
        bad.setId(12345678);

        given()
                .pathParam("fruitId", bad.getId())
                .contentType(ContentType.JSON)
                .body(new ObjectMapper().writeValueAsString(bad))
                .when()
                .put("/{fruitId}")
                .then()
                .assertThat().statusCode(404)
                .extract().asString();
    }

    @Test
    public void testUpdateInvalidPayload() {
        given()
                .contentType(ContentType.TEXT)
                .body("")
                .post()
                .then()
                .assertThat().statusCode(415);
    }

    @Test
    public void testUpdateIllegalPayload() throws Exception {
        Fruit carrot = createFruit("Carrot");
        System.out.println(carrot.getId());
        carrot.setName(null);

        String payload = given()
                .pathParam("fruitId", carrot.getId())
                .contentType(ContentType.JSON)
                .body(new ObjectMapper().writeValueAsString(carrot))
                .when()
                .put("/{fruitId}")
                .then()
                .assertThat().statusCode(422)
                .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        System.out.println(obj.getString("error"));
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void testDelete() throws Exception {
        Fruit orange = createFruit("Orange");

        delete("/" + orange.getId())
                .then()
                .assertThat().statusCode(204);

        get()
                .then()
                .assertThat().statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void testDeleteWithUnknownId() {
        delete("/unknown")
                .then()
                .assertThat().statusCode(404);

        get()
                .then()
                .assertThat().statusCode(200)
                .body(is("[]"));
    }

    private Fruit createFruit(String name) throws Exception {
        String payload = given()
                .contentType(ContentType.JSON)
                .body(convert(Json.createObjectBuilder().add("name", name).build()))
                .post()
                .then().log().ifValidationFails(LogDetail.ALL)
                .assertThat().statusCode(201)
                .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        return new ObjectMapper().readValue(payload, Fruit.class);
    }

    private String convert(JsonObject object) {
        StringWriter stWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stWriter);
        jsonWriter.writeObject(object);
        jsonWriter.close();

        return stWriter.toString();
    }
}

