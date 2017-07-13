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
import models.daos.DatasetDao;
import models.daos.UserDao;
import utils.Urn;
import org.springframework.dao.EmptyResultDataAccessException;
import play.Logger;
import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;


/**
 * Created by zechen on 10/12/15.
 */
public class DatasetController extends Controller {

  public static Result getDatasetWatchers(String datasetName)
      throws SQLException {
    ObjectNode resultJson = Json.newObject();
    if (datasetName != null) {
      ObjectNode result = UserDao.getWatchers(datasetName);
      resultJson.put("return_code", 200);
      resultJson.set("watchers", result);
    }
    return ok(resultJson);
  }

  public static Result getDatasetInfo() throws SQLException {
    ObjectNode resultJson = Json.newObject();
    String datasetIdString = request().getQueryString("datasetId");
    if (datasetIdString != null) {
      int datasetId = Integer.valueOf(datasetIdString);

      try {
        Map<String, Object> dataset = DatasetDao.getDatasetById(datasetId);
        resultJson.put("return_code", 200);
        resultJson.set("dataset", Json.toJson(dataset));
      } catch (EmptyResultDataAccessException e) {
        e.printStackTrace();
        resultJson.put("return_code", 404);
        resultJson.put("error_message", "dataset can not find!");
      }
      return ok(resultJson);
    }

    String urn = request().getQueryString("urn");
    if (urn != null) {
      if (!Urn.validateUrn(urn)) {
        resultJson.put("return_code", 400);
        resultJson.put("error_message", "Urn format wrong!");
        return ok(resultJson);
      }
      try {
        Map<String, Object> dataset = DatasetDao.getDatasetByUrn(urn);
        resultJson.put("return_code", 200);
        resultJson.set("dataset", Json.toJson(dataset));
      } catch (EmptyResultDataAccessException e) {
        e.printStackTrace();
        resultJson.put("return_code", 404);
        resultJson.put("error_message", "dataset can not find!");
      }
      return ok(resultJson);
    }

    // if no parameter, return an error message
    resultJson.put("return_code", 400);
    resultJson.put("error_message", "No parameter provided");
    return ok(resultJson);
  }

  @BodyParser.Of(BodyParser.Json.class)
  public static Result addDataset() {
    JsonNode dataset = request().body().asJson();
    ObjectNode resultJson = Json.newObject();
    try {
      DatasetDao.setDatasetRecord(dataset);
      resultJson.put("return_code", 200);
      resultJson.put("message", "Dataset inserted!");
    } catch (Exception e) {
      e.printStackTrace();
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }

    return ok(resultJson);
  }

  @BodyParser.Of(BodyParser.Json.class)
  public static Result getDatasetDependency() {
    String queryString = request().getQueryString("query");
    JsonNode input = Json.parse(queryString);
    ObjectNode resultJson = Json.newObject();

    try {
      resultJson = DatasetDao.getDatasetDependency(input);
    } catch (Exception e) {
      Logger.error(e.getMessage());
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }

    return ok(resultJson);
  }

  public static Result getDatasetUrns(String propertiesLike)
      throws SQLException {
    ObjectNode resultJson = Json.newObject();
    try {
      if (propertiesLike != null) {
        ObjectNode result = DatasetDao.getDatasetUrnForPropertiesLike(propertiesLike);
        resultJson.put("return_code", 200);
        resultJson.set("dataset_urns", result);
      }
    } catch (Exception e) {
      e.printStackTrace();
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }

    return ok(resultJson);
  }

  public static Result getDatasetDependentsById(Long datasetId)
      throws SQLException {
    ObjectNode resultJson = Json.newObject();
    if (datasetId > 0) {
      try {
        List<Map<String, Object>> dependents = DatasetDao.getDatasetDependents(datasetId);
        resultJson.put("return_code", 200);
        resultJson.set("dependents", Json.toJson(dependents));
      } catch (EmptyResultDataAccessException e) {
        e.printStackTrace();
        resultJson.put("return_code", 404);
        resultJson.put("error_message", "no dependent datasets can be found!");
      }
      return ok(resultJson);
    }
    // if no parameter, return an error message
    resultJson.put("return_code", 400);
    resultJson.put("error_message", "Dataset Id is not provided or invalid");
    return ok(resultJson);
  }

  public static Result getDatasetDependentsByUri(String datasetUri)
      throws SQLException {
    /* expect
     * hive:///db_name.table_name
     * hive:///db_name/table_name
     * dalids:///db_name.table_name
     * dalids:///db_name/table_name
     * hdfs:///dir1/dir2/dir3/dir4
     * teradata:///db_name/table_name
     */
    ObjectNode resultJson = Json.newObject();
    String[] uri_parts = datasetUri.split(":");
    if (uri_parts.length != 2) {
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "Invalid dataset URI");
      return ok(resultJson);
    }
    String dataset_type = uri_parts[0];
    String dataset_path = uri_parts[1].substring(2);  // start from the 3rd slash
    if (dataset_path.indexOf(".") > 0) {
      dataset_path = dataset_path.replace(".", "/");
    }

    if (dataset_path != null) {
      try {
        List<Map<String, Object>> dependents = DatasetDao.getDatasetDependents(dataset_type, dataset_path);
        resultJson.put("return_code", 200);
        resultJson.set("dependents", Json.toJson(dependents));
      } catch (EmptyResultDataAccessException e) {
        e.printStackTrace();
        resultJson.put("return_code", 404);
        resultJson.put("error_message", "No dependent dataset can be found!");
      }
      return ok(resultJson);
    }

    // if no parameter, return an error message
    resultJson.put("return_code", 400);
    resultJson.put("error_message", "No parameter provided");
    return ok(resultJson);
  }
  // get the latest edited dataset (or job or db, but intended fordatasets) based on the scheme passed as type
  public static Result getLatestOfType(String type) {
    ObjectNode resultJson = Json.newObject();
      try {
        if (type == null) {
          resultJson.put("return_code", 400);
          resultJson.put("error_message", "no type provided");
          return ok(resultJson);
        } else {
          resultJson = DatasetDao.getLatestOfType(type);
          return ok(resultJson);
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      return ok("there was a problem");
  }

  // same as getLatestOfType(), but only gets entities who's last modified time was after a specified unix time in seconds
  // this is a good way to check if the current dataset in use is the newest or not
  public static Result getLatestAfter(String type, long time) {
    ObjectNode resultJson = Json.newObject();
    try {
      if (type == null) {
        resultJson.put("return_code", 400);
        resultJson.put("error_message", "type not provided");
        return ok(resultJson);
      } else {
        resultJson = DatasetDao.getLatestAfter(type, time);
        return ok(resultJson);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    resultJson.put("message", "there was a problem");
    return ok(resultJson);
  }

  // same as getLatestOfType(), but only gets entities who's last modified time was before a specified unix time in seconds
  public static Result getLatestBefore(String type, long time) {
    ObjectNode resultJson = Json.newObject();
    try {
      if (type == null) {
        resultJson.put("return_code", 400);
        resultJson.put("error_message", "type not provided");
        return ok(resultJson);
      } else {
        resultJson = DatasetDao.getLatestBefore(type, time);
        return ok(resultJson);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    resultJson.put("message", "there was a problem");
    return ok(resultJson);
  }

  // same as getLatestOfType(), but only gets entities who's last modified time was between the two given times, good for replicating a past run
  public static Result getLatestBetween(String type, long firsttime, long secondtime) {
    ObjectNode resultJson = Json.newObject();
    try {
      if (type == null) {
        resultJson.put("return_code", 400);
        resultJson.put("error_message", "type not provided");
        return ok(resultJson);
      } else {
        if (firsttime > secondtime) {
          long temp = secondtime;
          secondtime = firsttime;
          firsttime = temp;
        }
        resultJson = DatasetDao.getLatestBetween(type, firsttime, secondtime);
        return ok(resultJson);
      }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    resultJson.put("message", "there was a problem");
    return ok(resultJson);
  }

  // gets an entity of the given time at the specific time given, if it exists
  public static Result getAtTime(String type, long time) {
    ObjectNode resultJson = Json.newObject();
    try {
      if (type == null) {
        resultJson.put("return_code", 400);
        resultJson.put("error_message", "type not provided");
        return ok(resultJson);
      } else {
        resultJson = DatasetDao.getAtTime(type, time);
        return ok(resultJson);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    resultJson.put("message", "there was a problem");
    return ok(resultJson);
  }

  // works like getAtTime() but the time is +- the given window
  public static Result getAtTimeWindow(String type, long time, long window) throws SQLException {
    ObjectNode resultJson = Json.newObject();
    try {
      if (type == null) {
        resultJson.put("return_code", 400);
        resultJson.put("error_message", "type not provided");
        return ok(resultJson);
      } else {
        long firsttime = time - window;
        long secondtime = time + window;
        resultJson = DatasetDao.getLatestBetween(type, firsttime, secondtime);
        return ok(resultJson);
      }
    } catch (SQLException e) {
      Logger.error("Exception getting latest of type " + type, e);
    }
    resultJson.put("message", "there was a problem");
    return ok(resultJson);
  }

  // allows for adding or changing vaues in the properties field
  public static Result updateProperties() {
    JsonNode propChanges = request().body().asJson();
    ObjectNode resultJson = Json.newObject();

    try {
      DatasetDao.updateProperties(propChanges);
    } catch (Exception e) {
      Logger.error(e.getMessage());
      e.printStackTrace();
      Logger.error(propChanges.toString());
      resultJson.put("return_code", 400);
      resultJson.put("error_message", e.getMessage());
    }
    resultJson.put("return_code", 200);
    resultJson.put("message", "properties updated");
    return ok(resultJson);
  }

  // allows for setting the validity of an entity as true or false, stored in properties field
  public static Result setValidity() {
    JsonNode validNode = request().body().asJson();
    ObjectNode resultJson = Json.newObject();
    try {
      DatasetDao.updateProperties(validNode);
      resultJson.put("return_code", 200);
      resultJson.put("message", "validity updated");
      return ok(resultJson);
    } catch (SQLException e) {
      Logger.error("sql exception in setValidity", e);
      resultJson.put("return_code", 400);
      resultJson.put("error_message", e.getMessage());
    } catch (Exception e) {
      Logger.error("sql exception in setValidity", e);
      resultJson.put("return_code", 400);
      resultJson.put("error_message", e.getMessage());
    }
    resultJson.put("message", "there was a problem");
    return ok(resultJson);
  }

  // returns json with either error message, error message + empty, or common parent(s)
  public static Result getCommonParents() {
    // Li wherehows does not have an implimentation for this, so error
    if (Play.application().configuration().getString("diet").equals("true")) {
      Logger.error("Trying to run a function not implimented in LinkedIn wherehows");
      ObjectNode resultJson = Json.newObject();
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "LinkedIn WhereHows does not impliment this call");
      return ok(resultJson);
    }

    ObjectNode resultJson = Json.newObject();
    String urnOne = request().getQueryString("urnOne");
    String urnTwo = request().getQueryString("urnTwo");
    if (urnOne != null && urnTwo != null) {
      try {
        resultJson = DatasetDao.getCommonParents(urnOne, urnTwo);
        return ok(resultJson);
        } catch (Exception e) {
        Logger.error("sql exception in getCommonParents", e);
        resultJson.put("return_code", 400);
        resultJson.put("error_message", e.getMessage());
        }
    } else {
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "not all required arguments passed");
    }
    return ok(resultJson);
  }

  // remove a dataset by urn
  @BodyParser.Of(BodyParser.Json.class)
  public static Result removeDataset() {
    JsonNode dataset = request().body().asJson();
    ObjectNode resultJson = Json.newObject();
    try {
      DatasetDao.removeDataset(dataset);
      resultJson.put("return_code", 200);
      resultJson.put("message", "dataset removed");
    } catch (Exception e) {
      Logger.error("exception when trying to remove dataset:", e);
      resultJson.put("return_code", 400);
      resultJson.put("error_message", e.getMessage());
    }
    return ok(resultJson);
  }

}
