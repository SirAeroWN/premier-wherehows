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
package dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.lang.Math.*;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import play.Logger;
import play.Play;
import play.libs.Json;
import utils.Lineage;

public class LineageDAO extends AbstractMySQLOpenSourceDAO {

    private final static String GET_PARENTS = "SELECT parent_urn FROM family WHERE child_urn = ?";

    private final static String GET_CHILDREN = "SELECT child_urn FROM family WHERE parent_urn = ?";

    private final static String GET_DATA_ATTR = "SELECT * FROM dict_dataset WHERE urn = :urn";

    private final static String GET_APP_ATTR = "SELECT * FROM cfg_application WHERE app_id = :urn";

    private final static String GET_DB_ATTR = "SELECT * FROM cfg_database WHERE uri = :urn";

    public static JsonNode getObjectAdjacnet(String urn, int upLevel, int downLevel, int lookBackTime) {
        // lineage is stored in an ajacency table with columns: id | parent_urn | child_urn
        // ignores look back time

        int level = 0; // level goes up for parents, down for children

        // lists of nodes and edges
        List<LineageNode> nodes = new ArrayList<LineageNode>();
        List<LineageEdge> edges = new ArrayList<LineageEdge>();

        // create jsonNode
        ObjectNode resultNode = Json.newObject();

        // create a single node and add it to list of nodes
        // dislike that this code is repeated in the function...
        LineageNode node = new LineageNode();
        node.id = nodes.size();
        node.level = level;
        node.urn = urn;
        node._sort_list = new ArrayList<String>();
        switch (parseURN(urn)) {
            case "app":
                node.node_type = "app";
                // do assignment stuff
                assignApp(node);
                break;
            case "data":
                node.node_type = "data";
                // do assignment stuff
                assignData(node);
                break;
            case "DB":
                node.node_type = "DB";
                // do assignment stuff
                assignDB(node);
                break;
            default:
                Logger.error("parsing failed for origin URN: " + urn);
        }
        nodes.add(node);

        // add all ancestors of origin to nodes & edges
        getRelativeGraph(nodes, edges, upLevel, 1, node);
        getRelativeGraph(nodes, edges, downLevel, -1, node);

        // now stick nodes and edges into the JsonNode
        resultNode.set("nodes", Json.toJson(nodes));
        resultNode.set("links", Json.toJson(edges));
        resultNode.put("urn", urn);
        resultNode.put("message", "Gee, I hope this works");
        return resultNode;
    }

    private static String parseURN(String urn) {
            String file_type = urn.substring(0, urn.indexOf("://")).toLowerCase();
            switch (file_type) {
                case "raw-parquet":
                case "domain-parquet":
                case "opportunity-parquet":
                case "parquet":
                case "supply-druid":
                case "purchase-druid":
                case "druid":
                case "lucene":
                    // this is a dataset so we call the dataset handling function
                    return "data";
                case "natezza":
                case "pulse":
                case "qa":
                case "sa":
                case "pim":
                case "datamgt":
                    // this is a database so call the database handling function
                    return "DB";
                case "moveit-extract":
                case "moveit-transform":
                    return "app";
                default:
                    // must be an error if we got here
                    Logger.error("could not parse URN, assuming job: " + urn);
                    return "app";
            }
    }

    private static void getRelativeGraph(List<LineageNode> nodes, List<LineageEdge> edges, int maxDepth, int direction, LineageNode currNode) {
        //LineageNode currNode = nodes.get(nodes.size() - 1);
        if (Math.abs(currNode.level) <= Math.abs(maxDepth)) {
            // do our thing
            List<String> relatives = getRelatives(currNode.urn, direction);
            Logger.debug(relatives.toString());
            for (String relative : relatives) {
                LineageNode node = new LineageNode();
                node.id = nodes.size();
                node.level = currNode.level + direction;
                node.urn = relative;
                node._sort_list = new ArrayList<String>();
                LineageEdge edge = new LineageEdge();
                edge.id = edges.size();
                switch (parseURN(relative)) {
                    case "app":
                        // do assignment stuff
                        node.node_type = "app";
                        assignApp(node);
                        nodes.add(node);
                        edges.add(edge);
                        getRelativeGraph(nodes, edges, maxDepth, direction, nodes.get(nodes.size() - 1));
                        break;
                    case "data":
                        // do assignment stuff
                        node.node_type = "data";
                        assignData(node);
                        nodes.add(node);
                        edges.add(edge);
                        getRelativeGraph(nodes, edges, maxDepth, direction, nodes.get(nodes.size() - 1));
                        break;
                    case "DB":
                        // do assignment stuff
                        node.node_type = "DB";
                        assignDB(node);
                        nodes.add(node);
                        edges.add(edge);
                        getRelativeGraph(nodes, edges, maxDepth, direction, nodes.get(nodes.size() - 1));
                        break;
                    default:
                        Logger.error("parsing failed for relative URN: " + relative);
                }
                String target = "";
                String source = "";
                if (direction > 0) {
                    edge.target = currNode.id;
                    target = currNode.node_type;
                    edge.source = node.id;
                    source = node.node_type;
                } else {
                    edge.target = node.id;
                    target = node.node_type;
                    edge.source = currNode.id;
                    source = currNode.node_type;
                }
                if ((source == "data" && target == "data") || (source == "data" && target == "DB") || (source == "DB" && target == "DB") || (source == "DB" && target == "data")) {
                    edge.label = "source";
                } else if (source == "data" && target == "app") {
                    edge.label = "read";
                } else if (source == "app" && target == "app") {
                    edge.label = "spawned";
                    edge.type = "job";
                } else if ((source == "app" && target == "data") || (source == "app" && target == "DB")) {
                    edge.label = "created";
                    edge.type = "job";
                } else if (source == "DB" && target == "app") {
                    edge.label = "imported";
                } else {
                    edge.label = "influenced";
                }
            }
        }
        // we have reached maximum requested depth, let's peace out
    }

    private static List<String> getRelatives(String urn, int direction) {
        if (direction > 0) {
            return getParents(urn);
        } else if (direction < 0) {
            return getChildren(urn);
        } else {
            Logger.error("Direction cannot equal 0");
            return null;
        }
    }

    private static List<String> getParents(String urn) {
        List<String> parents = getJdbcTemplate().queryForList(GET_PARENTS, String.class, urn);
        if (parents == null || parents.size() == 0) {
            Logger.error("couldn't find any parents for URN: " + urn);
        }
        return parents;
    }

    private static List<String> getChildren(String urn) {
        List<String> children = getJdbcTemplate().queryForList(GET_CHILDREN, String.class, urn);
        if (children == null || children.size() == 0) {
            Logger.error("couldn't find any children for URN: " + urn);
        }
        return children;
    }

    /*
    TODO: assignXXX methods need to be modified to reflect the fact that ds, db, and apps are now all stored in dict_dataset
    this also means they probably need to parse the properties field to get some of the info
    */
    private static void assignApp(LineageNode node) {
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_APP_ATTR, parameters);

        for (Map row : rows) {
            // node only knows id, level, and urn, assign all other attributes
            node.app_code = (String) row.get("app_code");
            node.description = (String) row.get("description");
            node.tech_matrix_id = (int) row.get("tech_matrix_id");
            node.doc_url = (String) row.get("doc_url");
            //node.parent_app_id = (int) row.get("parent_app_id");
            node.app_status = (String) row.get("app_status");
            //node.last_modified = (String) row.get("last_modified");
            node.is_logical = (String) row.get("is_logical");
            node.uri_type = (String) row.get("uri_type");
            node.uri = (String) row.get("uri");
            node.lifecycle_layer_id = (String) row.get("lifecycle_layer_id");
            node.short_connection_string = (String) row.get("short_connection_string");
            node._sort_list.add("urn");
            node._sort_list.add("app_code");
            node._sort_list.add("description");
            //node._sort_list.add("last_modified");
        }
    }

    private static void assignData(LineageNode node) {
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_DATA_ATTR, parameters);

        for (Map row : rows) {
            // node only knows id, level, and urn, assign all other attributes
            node.name = (String) row.get("name");
            node.schema = (String) row.get("schema");
            node.properties = (String) row.get("properties");
            node.source = (String) row.get("source");
            node.storage_type = (String) row.get("dataset_type"); // what the js calls storage_type, the sql calls dataset_type
            node.dataset_type = (String) row.get("dataset_type");
            //node.source_created_time = (String) row.get("source_created_time");
            //node.source_modified_time = (String) row.get("source_modified_time");
            //node.created_time = (String) row.get("created_time");
            //node.modified_time = (String) row.get("modified_time");
            node.abstracted_path = node.urn.substring(node.urn.indexOf("://") + 3, node.urn.length()).toLowerCase();
            try {
                node._sort_list.add("abstracted_path");
                node._sort_list.add("storage_type");
                node._sort_list.add("urn");
            } catch (NullPointerException e) {
                if (node == null) {
                    Logger.debug("it's node");
                } else if (node._sort_list == null) {
                    Logger.debug("it's sort list|" + Math.abs(node.level));
                }
            }
        }
    }

    private static void assignDB(LineageNode node) {
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_DB_ATTR, parameters);
        // node only knows id, level, and urn, assign all other attributes

        for (Map row : rows) {
            node.db_code = (String) row.get("db_code");
            node.primary_dataset_type = (String) row.get("primary_dataset_type");
            node.description = (String) row.get("description");
            node.is_logical = (String) row.get("is_logical");
            node.deployment_tier = (String) row.get("deployment_tier");
            node.data_center = (String) row.get("data_center");
            //node.associated_dc_num = (int) row.get("associated_dc_num");
            node.cluster = (String) row.get("cluster");
            //node.cluster_size = (int) row.get("cluster_size");
            node.extra_deployment_tag1 = (String) row.get("extra_deployment_tag1");
            node.extra_deployment_tag2 = (String) row.get("extra_deployment_tag2");
            node.extra_deployment_tag3 = (String) row.get("extra_deployment_tag3");
            node.replication_role = (String) row.get("replication_role");
            node.jdbc_url = (String) row.get("jdbc_url");
            node._sort_list.add("urn");
            node._sort_list.add("db_code");
            //node._sort_list.add("last_modified");
        }
    }

    public static ObjectNode getFlowLineage(String application, String project, Long flowId) {
        ObjectNode resultNode = Json.newObject();
        return resultNode;
    }

    public static List<ImpactDataset> getImpactDatasetsByUrn(String urn) {
        List<ImpactDataset> impactDatasetList = new ArrayList<ImpactDataset>();

        // if (StringUtils.isNotBlank(urn))
        // {
        //     List<String> searchUrnList = new ArrayList<String>();
        //     searchUrnList.add(urn);
        //     getImpactDatasets(searchUrnList, 1, impactDatasetList);
        // }

        return impactDatasetList;

    }
}