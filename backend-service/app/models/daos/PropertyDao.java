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
package models.daos;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.sql.SQLException;
import org.springframework.dao.DataAccessException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import utils.Urn;
import utils.JdbcUtil;
import play.Logger;
import wherehows.common.schemas.PropertyRecord;
import wherehows.common.utils.PartitionPatternMatcher;
import wherehows.common.writers.DatabaseWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import play.libs.Json;
import wherehows.common.schemas.DatasetDependencyRecord;
import wherehows.common.schemas.DatasetRecord;


/**
 * Created by norv on 06/26/17.
 */
// implimentations to go along with the controllers in PropertyController
public class PropertyDao {

    // sql query for getting the value of a property
    private final static String GET_PROPERTY = "SELECT property_value FROM wh_property WHERE property_name = :name";

    // function for getting the value of a property, returns "default" if the property does not exist
    private static String getProp(String propName) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", propName);
        List<Map<String, Object>> props = JdbcUtil.wherehowsNamedJdbcTemplate.queryForList(GET_PROPERTY, params);
        if (props == null || props.size() == 0) {
            Logger.info("Could not find property for property_name: " + propName);
            return "default";
        }
        for (Map p : props) {
            return (String) p.get("property_value");
        }
        return "default";
    }

    // function for actuallu setting a property/preference
    private static void setProp(String propName, String propVal) throws IOException, SQLException, DataAccessException {
        DatabaseWriter dw = new DatabaseWriter(JdbcUtil.wherehowsJdbcTemplate, "wh_property");
        PropertyRecord record = new PropertyRecord(propName, propVal, "N");
        dw.append(record);
        boolean temp = dw.insert("property_name, property_value, is_encrypted");
        dw.close();
    }

    // function for actually removing a  property/preference
    private static void remProp(String propName) throws IOException, SQLException, DataAccessException {
        DatabaseWriter dw = new DatabaseWriter(JdbcUtil.wherehowsJdbcTemplate, "wh_property");
        Map<String, String> params = new HashMap();
        params.put("property_name", "'" + propName + "'");
        dw.remove(params);
    }

    // function for updating property/preference
    private static void chngProp(String propName, String propVal) throws IOException, SQLException, DataAccessException {
        DatabaseWriter dw = new DatabaseWriter(JdbcUtil.wherehowsJdbcTemplate, "wh_property");
        dw.generalUpdate("property_value = '" + propVal + "'", "property_name", propName);
    }


    // Functions for the implimentation of setting, updating, and getting the properties field
    public static void addAssignProp(JsonNode props) throws IOException, SQLException, DataAccessException {
        String name = props.get("scheme").asText();
        JsonNode propNode = props.findPath("properties");
        String propString = "";
        if (propNode.isArray()) {
            for (JsonNode p : propNode) {
                propString = propString + p.textValue() + ",";
            }
            propString = propString.substring(0, propString.length() - 1);
        } else if (propNode.isTextual()) {
            propString = propNode.textValue();
        } else {
            Logger.error("passed property neither a list or array");
            throw new IllegalArgumentException();
        }
        setProp("prop." + name, propString);
    }

    public static void updateAssignProp(JsonNode props) throws IOException, SQLException, DataAccessException {
        String name = props.get("scheme").asText();
        JsonNode propNode = props.findPath("properties");
        String propString = getProp("prop." + name) + ",";
        if (propNode.isArray()) {
            for (JsonNode p : propNode) {
                propString = propString + p.textValue() + ",";
            }
            propString = propString.substring(0, propString.length() - 1);
        } else if (propNode.isTextual()) {
            propString = propString + propNode.textValue();
        } else {
            Logger.error("passed property neither a list or array");
            throw new IllegalArgumentException();
        }
        chngProp("prop." + name, propString);
    }

    public static ObjectNode getAssignProp(String prop) {
        String name = prop;

        String propString = getProp("prop." + name);

        ObjectNode resultJson = Json.newObject();
        resultJson.put("name", name);
        resultJson.put("properties", propString);

        return resultJson;
    }



    // Functions for the implimentation of setting, updating, and getting the tooltip list
    public static void addSortListProp(JsonNode props) throws IOException, SQLException, DataAccessException {
        String name = props.get("scheme").asText();
        JsonNode propNode = props.findPath("properties");
        String propString = "";
        if (propNode.isArray()) {
            for (JsonNode p : propNode) {
                propString = propString + p.textValue() + ",";
            }
            propString = propString.substring(0, propString.length() - 1);
        } else if (propNode.isTextual()) {
            propString = propNode.textValue();
        } else {
            Logger.error("passed property neither a list or array");
            throw new IllegalArgumentException();
        }
        setProp("prop.sortlist." + name, propString);
    }

    public static void updateSortListProp(JsonNode props) throws IOException, SQLException, DataAccessException {
        String name = props.get("scheme").asText();
        JsonNode propNode = props.findPath("properties");
        String propString = getProp("prop.sortlist." + name) + ",";
        if (propNode.isArray()) {
            for (JsonNode p : propNode) {
                propString = propString + p.textValue() + ",";
            }
            propString = propString.substring(0, propString.length() - 1);
        } else if (propNode.isTextual()) {
            propString = propString + propNode.textValue();
        } else {
            Logger.error("passed property neither a list or array");
            throw new IllegalArgumentException();
        }
        chngProp("prop.sortlist." + name, propString);
    }

    public static ObjectNode getSortListProp(String prop) {
        String name = prop;

        String propString = getProp("prop.sortlist." + name);

        ObjectNode resultJson = Json.newObject();
        resultJson.put("name", name);
        resultJson.put("properties", propString);

        return resultJson;
    }



    // Functions for the implimentation of setting, updating, and getting node colors
    public static void addNodeColor(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "node.color." + prop.get("scheme").asText();
        String value = prop.get("color").asText();
        setProp(name, value);
    }

    public static void updateNodeColor(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "node.color." + prop.get("scheme").asText();
        String value = prop.get("color").asText();
        chngProp(name, value);
    }

    public static ObjectNode getNodeColor(String name) {
        name = "node.color." + name;
        ObjectNode resultJson = Json.newObject();
        resultJson.put("property_name", name);
        resultJson.put("property_value", getProp(name));
        return resultJson;
    }



    // Functions for the implimentation of setting, updating, and getting node types
    public static void addNodeType(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "node.type." + prop.get("scheme").asText();
        String value = prop.get("type").asText();
        setProp(name, value);
    }

    public static void updateNodeType(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "node.type." + prop.get("scheme").asText();
        String value = prop.get("type").asText();
        chngProp(name, value);
    }

    public static ObjectNode getNodeType(String name) {
        name = "node.type." + name;
        ObjectNode resultJson = Json.newObject();
        resultJson.put("property_name", name);
        resultJson.put("property_value", getProp(name));
        return resultJson;

    }



    // Edge color is not currently used, but may be in the future
    public static void addEdgeColor(JsonNode prop) throws IOException, SQLException, DataAccessException {

    }

    public static void updateEdgeColor(JsonNode prop) throws IOException, SQLException, DataAccessException {

    }

    public static ObjectNode getEdgeColor(String name) {
        name = "edge.color." + name;
        ObjectNode resultJson = Json.newObject();
        resultJson.put("property_name", name);
        resultJson.put("property_value", getProp(name));
        return resultJson;
    }



    // Functions for the implimentation of setting, updating, and getting edge type
    public static void addEdgeType(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "edge.type." + prop.get("scheme").asText();
        String value = prop.get("type").asText();
        setProp(name, value);
    }

    public static void updateEdgeType(JsonNode prop) throws IOException, SQLException, DataAccessException {
        addEdgeType(prop);
    }

    public static ObjectNode getEdgeType(String name) {
        name = "edge.type." + name;
        ObjectNode resultJson = Json.newObject();
        resultJson.put("property_name", name);
        resultJson.put("property_value", getProp(name));
        return resultJson;
    }



    // Functions for the implimentation of setting, updating, and getting edge style
    public static void addEdgeStyle(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "edge.style." + prop.get("scheme").asText();
        String value = prop.get("style").asText();
        setProp(name, value);
    }

    public static void updateEdgeStyle(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "edge.style." + prop.get("scheme").asText();
        String value = prop.get("style").asText();
        chngProp(name, value);
    }

    public static ObjectNode getEdgeStyle(String name) {
        name = "edge.style." + name;
        ObjectNode resultJson = Json.newObject();
        resultJson.put("property_name", name);
        resultJson.put("property_value", getProp(name));
        return resultJson;
    }



    // Functions for the implimentation of setting, updating, and getting edge labels
    public static void addEdgeLabel(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "edge.label." + prop.get("scheme").asText();
        String value = prop.get("label").asText();
        setProp(name, value);
    }

    public static void updateEdgeLabel(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = "edge.label." + prop.get("scheme").asText();
        String value = prop.get("label").asText();
        chngProp(name, value);
    }

    public static ObjectNode getEdgeLabel(String name) {
        name = "edge.label." + name;
        ObjectNode resultJson = Json.newObject();
        resultJson.put("property_name", name);
        resultJson.put("property_value", getProp(name));
        return resultJson;
    }



    // Function to remove a preference
    public static void removeProperty(JsonNode prop) throws IOException, SQLException, DataAccessException {
        String name = prop.get("name").asText();
        remProp(name);
    }
}