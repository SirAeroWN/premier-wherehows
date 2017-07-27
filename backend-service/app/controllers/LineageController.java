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
import models.daos.LineageDaoLite;
import utils.ContrUtil;
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

  @BodyParser.Of(BodyParser.Json.class)
  public static Result addJobLineage() {
    JsonNode lineage = request().body().asJson();
    ObjectNode resultJson = Json.newObject();

    try {

      LineageDaoLite.insertLineage(lineage);

      resultJson.put("return_code", 200);
      resultJson.put("message", "Lineage inserted!");
      Logger.info("lineage inserted");
    } catch (Exception e) {
      
      ContrUtil.failure(resultJson, e.getMessage());

      Logger.error("caught exception", e);
      Logger.info("Post JSON for insertion: " + lineage.toString());
    }
    return ok(resultJson);
  }
}
