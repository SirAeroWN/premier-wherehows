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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import utils.Urn;
import utils.JdbcUtil;
import play.Logger;
import wherehows.common.schemas.LineageRecord;
import wherehows.common.utils.PartitionPatternMatcher;
import wherehows.common.writers.DatabaseWriter;


/**
 * Find jobs that use the urn as a source in recent 'period' days. Only find Azkaban jobs.
 * Created by zsun on 4/5/15.
 * Modified by zechen on 10/12/15.
 */
public class LineageDao {
  public static final String FIND_JOBS_BY_DATASET =
    " select distinct ca.short_connection_string, jedl.job_name, jedl.flow_path "
    + " from job_execution_data_lineage jedl "
    + " join cfg_application ca on ca.app_id = jedl.app_id "
    + " join cfg_database cd on cd.db_id = jedl.db_id "
    + " where source_target_type = :source_target_type "
    + " and jedl.abstracted_object_name = :abstracted_object_name "
    + " and jedl.job_finished_unixtime > UNIX_TIMESTAMP(subdate(current_date, :period)) "
    + " and ca.short_connection_string like :instance "
    + " and cd.short_connection_string like :cluster ";

  public static final String FIND_DATASETS_BY_JOB =
    " select distinct jedl.abstracted_object_name, jedl.db_id, jedl.partition_start, jedl.partition_end, "
      + " jedl.storage_type, jedl.record_count, jedl.insert_count, jedl.update_count, jedl.delete_count "
      + " from job_execution_data_lineage jedl join "
      + " (select jedl.app_id, max(jedl.job_exec_id) job_exec_id from job_execution_data_lineage jedl "
      + " where jedl.job_name = :job_name "
      + " and jedl.flow_path = :flow_path "
      + " group by app_id) a "
      + " on jedl.app_id = a.app_id and jedl.job_exec_id = a.job_exec_id "
      + " join cfg_application ca on ca.app_id = jedl.app_id "
      + " where jedl.source_target_type = :source_target_type "
      + " and ca.short_connection_string like :instance ";


  public static final String FIND_DATASETS_BY_FLOW_EXEC =
    " select distinct jedl.abstracted_object_name, jedl.db_id, jedl.partition_start, jedl.partition_end, "
      + " jedl.storage_type, jedl.record_count, jedl.insert_count, jedl.update_count, jedl.delete_count "
      + " from job_execution_data_lineage jedl "
      + " join cfg_application ca on ca.app_id = jedl.app_id "
      + " where jedl.job_name = :job_name "
      + " and jedl.flow_exec_id = :flow_exec_id "
      + " and jedl.source_target_type = :source_target_type "
      + " and ca.short_connection_string like :instance ";


  public static final String FIND_DATASETS_BY_JOB_EXEC =
    " select distinct jedl.abstracted_object_name, jedl.db_id, jedl.partition_start, jedl.partition_end, "
      + " jedl.storage_type, jedl.record_count, jedl.insert_count, jedl.update_count, jedl.delete_count "
      + " from job_execution_data_lineage jedl "
      + " join cfg_application ca on ca.app_id = jedl.app_id "
      + " where jedl.job_exec_id = :job_exec_id "
      + " and jedl.source_target_type = :source_target_type "
      + " and ca.short_connection_string like :instance ";


  public static List<Map<String, Object>> getJobsByDataset(String urn, String period, String cluster, String instance, String sourceTargetType)
    throws SQLException {
    Urn u = new Urn(urn);
    Map<String, Object> params = new HashMap<>();
    params.put("abstracted_object_name", u.abstractObjectName);
    params.put("period", period);
    params.put("source_target_type", sourceTargetType);

    if (cluster == null || cluster.isEmpty()) {
      params.put("cluster", "%");
    } else {
      params.put("cluster", cluster);
    }
    if (instance == null || instance.isEmpty()) {
      params.put("instance", "%");
    } else {
      params.put("instance", instance);
    }
    List<Map<String, Object>> jobs = JdbcUtil.wherehowsNamedJdbcTemplate.queryForList(FIND_JOBS_BY_DATASET, params);
    return jobs;
  }

  public static List<Map<String, Object>> getDatasetsByJob(String flowPath, String jobName, String instance, String sourceTargetType) {
    Map<String, Object> params = new HashMap<>();
    params.put("flow_path", flowPath);
    params.put("job_name", jobName);
    params.put("source_target_type", sourceTargetType);
    if (instance == null || instance.isEmpty()) {
      params.put("instance", "%");
    } else {
      params.put("instance", instance);
    }
    List<Map<String, Object>> datasets = JdbcUtil.wherehowsNamedJdbcTemplate.queryForList(FIND_DATASETS_BY_JOB, params);
    return datasets;
  }

  public static List<Map<String, Object>> getDatasetsByFlowExec(Long flowExecId, String jobName, String instance, String sourceTargetType) {
    Map<String, Object> params = new HashMap<>();
    params.put("flow_exec_id", flowExecId);
    params.put("job_name", jobName);
    params.put("source_target_type", sourceTargetType);
    if (instance == null || instance.isEmpty()) {
      params.put("instance", "%");
    } else {
      params.put("instance", instance);
    }
    List<Map<String, Object>> datasets = JdbcUtil.wherehowsNamedJdbcTemplate.queryForList(FIND_DATASETS_BY_FLOW_EXEC, params);
    return datasets;
  }

  public static List<Map<String, Object>> getDatasetsByJobExec(Long jobExecId, String instance, String sourceTargetType) {
    Map<String, Object> params = new HashMap<>();
    params.put("job_exec_id", jobExecId);
    params.put("source_target_type", sourceTargetType);
    if (instance == null || instance.isEmpty()) {
      params.put("instance", "%");
    } else {
      params.put("instance", instance);
    }
    List<Map<String, Object>> datasets = JdbcUtil.wherehowsNamedJdbcTemplate.queryForList(FIND_DATASETS_BY_JOB_EXEC, params);
    return datasets;
  }

  public static void insertLineage(JsonNode lineage) throws Exception {
      List<LineageRecord> records = new ArrayList<LineageRecord>();
      JsonNode parents = lineage.findPath("parent_urn");
      JsonNode children = lineage.findPath("child_urn");
      //Logger.debug(parents.toString());
      //Logger.debug(children.toString());
      if (parents.isArray() && children.isArray()) {
          for (JsonNode parent : parents) {
              for (JsonNode child : children) {
                  //Logger.debug(parent.textValue());
                  //Logger.debug(child.textValue());
                  LineageRecord record = new LineageRecord(parent.textValue(), child.textValue());
                  records.add(record);
              }
          }
      }

    DatabaseWriter dw = new DatabaseWriter(JdbcUtil.wherehowsJdbcTemplate, "family");
    try {
      for (LineageRecord record : records) {
        dw.append(record);
      }
      Logger.debug("before insert");
      boolean temp = dw.insert("parent_urn, child_urn");
      if (!temp) {
          Logger.debug("insert failed mysteriusly");
      }
      Logger.debug("after insert");
    } catch (IOException | SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        dw.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

  }
}
