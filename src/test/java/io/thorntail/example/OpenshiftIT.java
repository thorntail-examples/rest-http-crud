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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@RunWith(Arquillian.class)
public class OpenshiftIT {
    @RouteURL(value = "${app.name}", path = "/api/fruits")
    @AwaitRoute(path = "/")
    private String url;

    @Before
    public void setup() {
        String jsonData =
                given()
                        .baseUri(url)
                .when()
                        .get()
                .then()
                        .extract().asString();

        JsonArray array = Json.createReader(new StringReader(jsonData)).readArray();
        array.forEach(val -> {
            given()
                    .baseUri(url)
            .when()
                    .delete("/" + ((JsonObject) val).getInt("id"))
            .then()
                    .statusCode(204);
        });
    }

    @Test
    public void retrieveNoFruit() {
        given()
                .baseUri(url)
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void oneFruit() throws Exception {
        createFruit("Peach");

        String payload =
                given()
                        .baseUri(url)
                .when()
                        .get()
                .then()
                        .statusCode(200)
                        .extract().asString();

        JsonArray array = Json.createReader(new StringReader(payload)).readArray();

        assertThat(array).hasSize(1);
        assertThat(array.get(0).getValueType()).isEqualTo(JsonValue.ValueType.OBJECT);

        JsonObject obj = (JsonObject) array.get(0);
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        given()
                .baseUri(url)
        .when()
                .pathParam("fruitId", obj.getInt("id"))
                .get("/{fruitId}")
        .then()
                .statusCode(200)
                .body(containsString("Peach"));
    }

    @Test
    public void createFruit() {
        String payload =
                given()
                        .baseUri(url)
                .when()
                        .contentType(ContentType.JSON)
                        .body(convert(Json.createObjectBuilder().add("name", "Raspberry").build()))
                        .post()
                .then()
                        .statusCode(201)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);
        assertThat(obj.getString("name")).isNotNull().isEqualTo("Raspberry");
    }

    @Test
    public void createInvalidPayload() {
        given()
                .baseUri(url)
        .when()
                .contentType(ContentType.TEXT)
                .body("")
                .post()
        .then()
                .statusCode(415);
    }

    @Test
    public void createIllegalPayload() {
        Fruit badFruit = new Fruit("Carrot");
        badFruit.setId(2);

        String payload =
                given()
                        .baseUri(url)
                .when()
                        .contentType(ContentType.JSON)
                        .body(badFruit)
                        .post()
                .then()
                        .statusCode(422)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void update() throws Exception {
        Fruit pear = createFruit("Pear");

        String response =
                given()
                        .baseUri(url)
                .when()
                        .pathParam("fruitId", pear.getId())
                        .get("/{fruitId}")
                .then()
                        .statusCode(200)
                        .extract().asString();

        pear = new ObjectMapper().readValue(response, Fruit.class);

        pear.setName("Not Pear");

        response =
                given()
                        .baseUri(url)
                .when()
                        .pathParam("fruitId", pear.getId())
                        .contentType(ContentType.JSON)
                        .body(new ObjectMapper().writeValueAsString(pear))
                        .put("/{fruitId}")
                .then()
                        .statusCode(200)
                        .extract().asString();

        Fruit updatedPear = new ObjectMapper().readValue(response, Fruit.class);

        assertThat(pear.getId()).isEqualTo(updatedPear.getId());
        assertThat(updatedPear.getName()).isEqualTo("Not Pear");
    }

    @Test
    public void updateWithUnknownId() throws Exception {
        Fruit bad = new Fruit("bad");
        bad.setId(12345678);

        given()
                .baseUri(url)
        .when()
                .pathParam("fruitId", bad.getId())
                .contentType(ContentType.JSON)
                .body(new ObjectMapper().writeValueAsString(bad))
                .put("/{fruitId}")
        .then()
                .statusCode(404)
                .extract().asString();
    }

    @Test
    public void updateInvalidPayload() {
        given()
                .baseUri(url)
        .when()
                .contentType(ContentType.TEXT)
                .body("")
                .post()
        .then()
                .statusCode(415);
    }

    @Test
    public void updateIllegalPayload() throws Exception {
        Fruit carrot = createFruit("Carrot");
        carrot.setName(null);

        String payload =
                given()
                        .baseUri(url)
                .when()
                        .pathParam("fruitId", carrot.getId())
                        .contentType(ContentType.JSON)
                        .body(new ObjectMapper().writeValueAsString(carrot))
                        .put("/{fruitId}")
                .then()
                        .statusCode(422)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getString("error")).isNotNull();
        assertThat(obj.getInt("code")).isNotNull().isEqualTo(422);
    }

    @Test
    public void testDelete() throws Exception {
        Fruit orange = createFruit("Orange");

        given()
                .baseUri(url)
        .when()
                .delete("/" + orange.getId())
        .then()
                .statusCode(204);

        given()
                .baseUri(url)
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    @Test
    public void deleteWithUnknownId() {
        given()
                .baseUri(url)
        .when()
                .delete("/unknown")
        .then()
                .statusCode(404);

        given()
                .baseUri(url)
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(is("[]"));
    }

    private Fruit createFruit(String name) throws Exception {
        String payload =
                given()
                        .baseUri(url)
                .when()
                        .contentType(ContentType.JSON)
                        .body(convert(Json.createObjectBuilder().add("name", name).build()))
                        .post()
                .then()
                        .statusCode(201)
                        .extract().asString();

        JsonObject obj = Json.createReader(new StringReader(payload)).readObject();
        assertThat(obj).isNotNull();
        assertThat(obj.getInt("id")).isNotNull().isGreaterThan(0);

        return new ObjectMapper().readValue(payload, Fruit.class);
    }

    private String convert(JsonObject object) {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stringWriter);
        jsonWriter.writeObject(object);
        jsonWriter.close();
        return stringWriter.toString();
    }
}
