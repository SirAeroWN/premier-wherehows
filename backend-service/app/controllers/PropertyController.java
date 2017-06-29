/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import models.daos.PropertyDao;
import models.daos.UserDao;
import utils.Urn;
import org.springframework.dao.EmptyResultDataAccessException;
import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by norv on 06/26/17.
 */
public class PropertyController extends Controller {

    @BodyParser.Of(BodyParser.Json.class)
    public static Result addAssignProp() {
        JsonNode props = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.addAssignProp(props);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Assignment Property inserted!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result updateAssignProp() {
        JsonNode props = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.updateAssignProp(props);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Assignment Property updated!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    public static Result getAssignProp(String name)
            throws SQLException {
        ObjectNode resultJson = Json.newObject();
        try {
            if (name != null) {
                ObjectNode result = PropertyDao.getAssignProp(name);
                resultJson.put("return_code", 200);
                resultJson.put("properties", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }



    @BodyParser.Of(BodyParser.Json.class)
    public static Result addSortListProp() {
        JsonNode props = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.addSortListProp(props);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Sort List Property inserted!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result updateSortListProp() {
        JsonNode props = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.updateSortListProp(props);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Sort List Property updated!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    public static Result getSortListProp(String name)
            throws SQLException {
        ObjectNode resultJson = Json.newObject();
        try {
            if (name != null) {
                ObjectNode result = PropertyDao.getSortListProp(name);
                resultJson.put("return_code", 200);
                resultJson.put("properties", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }



    @BodyParser.Of(BodyParser.Json.class)
    public static Result addNodeColor() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.addNodeColor(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Node Color inserted!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result updateNodeColor() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.updateNodeColor(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Node Color updated!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    public static Result getNodeColor(String name) {
        ObjectNode resultJson = Json.newObject();
        try {
            if (name != null) {
                ObjectNode result = PropertyDao.getNodeColor(name);
                resultJson.put("return_code", 200);
                resultJson.put("color", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }



    @BodyParser.Of(BodyParser.Json.class)
    public static Result addNodeType() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.addNodeType(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Node Type inserted!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result updateNodeType() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.updateNodeType(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Node Color updated!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    public static Result getNodeType(String name) throws SQLException {
        ObjectNode resultJson = Json.newObject();
        try {
            if (name != null) {
                ObjectNode result = PropertyDao.getNodeType(name);
                resultJson.put("return_code", 200);
                resultJson.put("type", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }


    // edge color is not currently used, but might be in the future
    @BodyParser.Of(BodyParser.Json.class)
    public static Result addEdgeColor() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.addEdgeColor(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Edge Color inserted!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result updateEdgeColor() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.updateEdgeColor(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Edge Color updated!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    public static Result getEdgeColor(String name) throws SQLException {
        ObjectNode resultJson = Json.newObject();
        try {
            if (name != null) {
                ObjectNode result = PropertyDao.getEdgeColor(name);
                resultJson.put("return_code", 200);
                resultJson.put("color", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }



    @BodyParser.Of(BodyParser.Json.class)
    public static Result addEdgeType() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.addEdgeType(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Edge Type inserted!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result updateEdgeType() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.updateEdgeType(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Edge Type updated!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    public static Result getEdgeType(String name) throws SQLException {
        ObjectNode resultJson = Json.newObject();
        try {
            if (name != null) {
                ObjectNode result = PropertyDao.getEdgeType(name);
                resultJson.put("return_code", 200);
                resultJson.put("type", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }



    @BodyParser.Of(BodyParser.Json.class)
    public static Result addEdgeStyle() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.addEdgeStyle(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Edge Style inserted!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result updateEdgeStyle() {
        JsonNode prop = request().body().asJson();
        ObjectNode resultJson = Json.newObject();
        try {
            PropertyDao.updateEdgeStyle(prop);
            resultJson.put("return_code", 200);
            resultJson.put("message", "Edge Style updated!");
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }

    public static Result getEdgeStyle(String name) throws SQLException {
        ObjectNode resultJson = Json.newObject();
        try {
            if (name != null) {
                ObjectNode result = PropertyDao.getEdgeStyle(name);
                resultJson.put("return_code", 200);
                resultJson.put("Style", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultJson.put("return_code", 404);
            resultJson.put("error_message", e.getMessage());
        }

        return ok(resultJson);
    }
}