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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import models.daos.LineageDao;
import models.daos.LineageDaoLite;
import utils.Urn;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.Logger;
import play.Play;


/**
 * Created by zsun on 4/5/15.
 */
public class LineageController extends Controller {

  public static Result getJobsByDataset(String urn) throws SQLException {
    // diet wherehows doesn't have an implimentation for this, so error
    if (Play.application().configuration().getString("diet").equals("true")) {
      Logger.error("Trying to run a function not implimented in diet wherehows");
      ObjectNode resultJson = Json.newObject();
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "Diet WhereHows does not impliment this call");
      return ok(resultJson);
    }

    if (!Urn.validateUrn(urn)) {
      ObjectNode resultJson = Json.newObject();
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "Urn format wrong!");
      return ok(resultJson);
    }

    String period = request().getQueryString("period");
    if (period == null) {
      period = "30"; // default look at recent 30 days
    }

    String cluster = request().getQueryString("cluster");
    String instance = request().getQueryString("instance");
    String direction = request().getQueryString("direction");
    String sourceTargetType = "source";
    if (direction != null && direction.toLowerCase().equals("upstream")) {
      sourceTargetType = "target";
    }
    ObjectNode resultJson = Json.newObject();

    try {
      List<Map<String, Object>> jobs = LineageDao.getJobsByDataset(urn, period, cluster, instance, sourceTargetType);
      resultJson.put("return_code", 200);
      resultJson.set("jobs", Json.toJson(jobs));
    } catch (Exception e) {
      e.printStackTrace();
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }
    return ok(resultJson);
  }

  public static Result getDatasetsByJob(String flowPath, String jobName) {
    // diet wherehows doesn't have an implimentation for this, so error
    if (Play.application().configuration().getString("diet").equals("true")) {
      Logger.error("Trying to run a function not implimented in diet wherehows");
      ObjectNode resultJson = Json.newObject();
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "Diet WhereHows does not impliment this call");
      return ok(resultJson);
    }

    String instance = request().getQueryString("instance");
    String direction = request().getQueryString("direction");
    String sourceTargetType = "target";
    if (direction != null && direction.toLowerCase().equals("upstream")) {
      sourceTargetType = "source";
    }

    ObjectNode resultJson = Json.newObject();
    try {
      List<Map<String, Object>> datasets = LineageDao.getDatasetsByJob(flowPath, jobName, instance, sourceTargetType);
      resultJson.put("return_code", 200);
      resultJson.set("datasets", Json.toJson(datasets));
    } catch (Exception e) {
      e.printStackTrace();
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }
    return ok(resultJson);
  }

  public static Result getDatasetsByFlowExec(Long flowExecId, String jobName) {
    // diet wherehows doesn't have an implimentation for this, so error
    if (Play.application().configuration().getString("diet").equals("true")) {
      Logger.error("Trying to run a function not implimented in diet wherehows");
      ObjectNode resultJson = Json.newObject();
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "Diet WhereHows does not impliment this call");
      return ok(resultJson);
    }

    String instance = request().getQueryString("instance");
    String direction = request().getQueryString("direction");
    String sourceTargetType = "target";
    if (direction != null && direction.toLowerCase().equals("upstream")) {
      sourceTargetType = "source";
    }

    ObjectNode resultJson = Json.newObject();
    try {
      List<Map<String, Object>> datasets = LineageDao.getDatasetsByFlowExec(flowExecId, jobName, instance, sourceTargetType);
      resultJson.put("return_code", 200);
      resultJson.set("datasets", Json.toJson(datasets));
    } catch (Exception e) {
      e.printStackTrace();
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }
    return ok(resultJson);
  }

  public static Result getDatasetsByJobExec(Long jobExecId) {
    // diet wherehows doesn't have an implimentation for this, so error
    if (Play.application().configuration().getString("diet").equals("true")) {
      Logger.error("Trying to run a function not implimented in diet wherehows");
      ObjectNode resultJson = Json.newObject();
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "Diet WhereHows does not impliment this call");
      return ok(resultJson);
    }

    String instance = request().getQueryString("instance");
    String direction = request().getQueryString("direction");
    String sourceTargetType = "target";
    if (direction != null && direction.toLowerCase().equals("upstream")) {
      sourceTargetType = "source";
    }

    ObjectNode resultJson = Json.newObject();
    try {
      List<Map<String, Object>> datasets = LineageDao.getDatasetsByJobExec(jobExecId, instance, sourceTargetType);
      resultJson.put("return_code", 200);
      resultJson.set("datasets", Json.toJson(datasets));
    } catch (Exception e) {
      e.printStackTrace();
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }
    return ok(resultJson);
  }

  @BodyParser.Of(BodyParser.Json.class)
  public static Result addJobLineage() {
    JsonNode lineage = request().body().asJson();
    ObjectNode resultJson = Json.newObject();
    // function implimented in both
    if (Play.application().configuration().getString("diet").equals("true")) {
      // diet wherehows calls

      try {
        LineageDaoLite.insertLineage(lineage);
        resultJson.put("return_code", 200);
        resultJson.put("message", "Lineage inserted!");
      } catch (IOException ioe) {
        Logger.error("caught exception", ioe);
        resultJson.put("return_code", 404);
        resultJson.put("error_message", ioe.getMessage());
      } catch (SQLException sqle) {
        Logger.error("caught exception", sqle);
        resultJson.put("return_code", 404);
        resultJson.put("error_message", sqle.getMessage());
      } catch (Exception e) {
        Logger.error("caught exception", e);
        resultJson.put("return_code", 404);
        resultJson.put("error_message", e.getMessage());
      }
    } else {
      // li wherehows calls

      try {
        LineageDao.insertLineage(lineage);
        resultJson.put("return_code", 200);
        resultJson.put("message", "Lineage inserted!");
      } catch (Exception e) {
        e.printStackTrace();
        resultJson.put("return_code", 404);
        resultJson.put("error_message", e.getMessage());
      }
    }


    return ok(resultJson);
  }

  @BodyParser.Of(BodyParser.Json.class)
  public static Result updateJobExecutionLineage() {
    // diet wherehows doesn't have an implimentation for this, so error
    if (Play.application().configuration().getString("diet").equals("true")) {
      Logger.error("Trying to run a function not implimented in diet wherehows");
      ObjectNode resultJson = Json.newObject();
      resultJson.put("return_code", 400);
      resultJson.put("error_message", "Diet WhereHows does not impliment this call");
      return ok(resultJson);
    }

    JsonNode lineage = request().body().asJson();
    ObjectNode resultJson = Json.newObject();
    try {
      LineageDao.updateJobExecutionLineage(lineage);
      resultJson.put("return_code", 200);
      resultJson.put("message", "Job Execution Lineage Updated!");
    } catch (Exception e) {
      resultJson.put("return_code", 404);
      resultJson.put("error_message", e.getMessage());
    }
    return ok(resultJson);
  }
}
