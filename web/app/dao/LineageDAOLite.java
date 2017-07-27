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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import play.Logger;
import play.Play;
import play.libs.Json;
import utils.Lineage;
import utils.Property;

public class LineageDAOLite extends AbstractMySQLOpenSourceDAO {

    private final static String GET_PARENTS = "SELECT parent_urn FROM family WHERE child_urn = ?";

    private final static String GET_CHILDREN = "SELECT child_urn FROM family WHERE parent_urn = ?";

    private final static String GET_DATA_ATTR = "SELECT * FROM dict_dataset WHERE urn = :urn";

    private final static String GET_APP_ATTR = "SELECT * FROM cfg_application WHERE app_id = :urn";

    private final static String GET_DB_ATTR = "SELECT * FROM cfg_database WHERE uri = :urn";

    private final static String GET_PROPERTY = "SELECT property_value FROM wh_property WHERE property_name = ?";

    public static JsonNode getObjectAdjacnet(String urn, int upLevel, int downLevel, int lookBackTime) {
        // lineage is stored in an ajacency table with columns: id | parent_urn | child_urn
        // ignores look back time

        int level = 0; // level goes up for parents, down for children

        // lists of nodes and edges
        List<LineageNodeLite> nodes = new ArrayList<LineageNodeLite>();
        List<LineageEdgeLite> edges = new ArrayList<LineageEdgeLite>();

        // create jsonNode
        ObjectNode resultNode = Json.newObject();

        // create a single node and add it to list of nodes
        // dislike that this code is repeated in the function...
        LineageNodeLite node = new LineageNodeLite();
        node.id = nodes.size();
        node.level = level;
        node.urn = urn;
        node._sort_list = new ArrayList<String>();
        switch (getNodeType(urn).toLowerCase()) {
            case "app":
                node.node_type = "app";
                assignGeneral(node);
                if (assignPrefs(node) == false) {
                    assignApp(node);
                    Logger.debug("using default app assigner for " + node.urn);
                }
                break;
            case "data":
                node.node_type = "data";
                assignGeneral(node);
                if (assignPrefs(node) == false) {
                    assignData(node);
                    Logger.debug("using default data assigner for " + node.urn);
                }
                node.abstracted_path = getPostfix(node.urn);
                break;
            case "db":
                node.node_type = "db";
                assignGeneral(node);
                if (assignPrefs(node) == false) {
                    assignDB(node);
                    Logger.debug("using default db assigner for " + node.urn);
                }
                break;
            default:
                node.node_type = "general";
                assignGeneral(node);
                assignPrefs(node);
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
        resultNode.put("diet", "true");
        resultNode.put("message", "Gee, I hope this works");
        //Logger.debug(resultNode.toString());
        return resultNode;
    }

    private static String getPrefix(String urn) {
        return Property.getPrefix(urn);
    }

    private static String getPostfix(String urn) {
        return Property.getPostfix(urn);
    }

    private static String getNodeType(String urn) {
        return getProp("node.type." + getPrefix(urn));
    }

    private static String getNodeColor(String urn, String node_type) {
        String color = getProp("node.color." + getPrefix(urn));
        if (color == null || color == "default") {
            // color names come from SVG color pallete at http://www.graphviz.org/doc/info/colors.html
            switch (node_type.toLowerCase()) {
                case "data":
                    return "thistle";
                case "db":
                    return "cyan";
                case "app":
                    return "tomato";
                default:
                    return "olive";
            }
        }
        return color;
    }

    private static String getProp(String propName) {
        return Property.getProp(propName);
    }

    private static void getRelativeGraph(List<LineageNodeLite> nodes, List<LineageEdgeLite> edges, int maxDepth, int direction, LineageNodeLite currNode) {
        //LineageNodeLite currNode = nodes.get(nodes.size() - 1);
        if (Math.abs(currNode.level) <= Math.abs(maxDepth)) {
            // do our thing
            List<String> relatives = getRelatives(currNode.urn, direction);
            //Logger.debug("relatives: " + relatives.toString());
            for (String relative : relatives) {
                boolean old = false;
                int id = 0;
                LineageNodeLite node = null;
                LineageEdgeLite edge = new LineageEdgeLite();
                edge.id = edges.size();
                //
                //
                // we should check if the node already exists in our list here
                //
                //
                // check that nodes doesn't already have a node with the same urn
                for (LineageNodeLite n : nodes) {
                    if (relative.equals(n.urn)) {
                        // this new node already exists, we should skip the provisioning part and assign this id in edge
                        old = true;
                        node = n;
                        break;
                    }
                }

                // check that the relative urn isn't already attached to an existant node
                if (!old) {
                    node = new LineageNodeLite();
                    node.id = nodes.size();
                    node.level = currNode.level + direction;
                    node.urn = relative;
                    node._sort_list = new ArrayList<String>();
                    switch (getNodeType(relative).toLowerCase()) {
                        case "app":
                            // do assignment stuff
                            node.node_type = "app";

                            assignGeneral(node);
                            if (assignPrefs(node) == false) {
                                assignApp(node);
                                Logger.debug("using default app assigner for " + node.urn);
                            }

                            nodes.add(node);
                            edges.add(edge);
                            getRelativeGraph(nodes, edges, maxDepth, direction, nodes.get(nodes.size() - 1));
                            break;
                        case "data":
                            // do assignment stuff
                            node.node_type = "data";

                            assignGeneral(node);
                            if (assignPrefs(node) == false) {
                                assignData(node);
                                Logger.debug("using default data assigner for " + node.urn);
                            }
                            node.abstracted_path = getPostfix(node.urn);

                            nodes.add(node);
                            edges.add(edge);
                            getRelativeGraph(nodes, edges, maxDepth, direction, nodes.get(nodes.size() - 1));
                            break;
                        case "db":
                            // do assignment stuff
                            node.node_type = "db";

                            assignGeneral(node);
                            if (assignPrefs(node) == false) {
                                assignDB(node);
                                Logger.debug("using default db assigner for " + node.urn);
                            }

                            nodes.add(node);
                            edges.add(edge);
                            getRelativeGraph(nodes, edges, maxDepth, direction, nodes.get(nodes.size() - 1));
                            break;
                        default:
                            node.node_type = "general";
                            assignGeneral(node);
                            assignPrefs(node);
                            nodes.add(node);
                            edges.add(edge);
                            getRelativeGraph(nodes, edges, maxDepth, direction, nodes.get(nodes.size() - 1));
                            Logger.error("parsing failed for relative URN: " + relative);
                    }
                }
                if (direction > 0) {
                    edge.target = currNode.id;
                    edge.source = node.id;
                    setEdgeAttr(edge, node, currNode);
                } else {
                    edge.target = node.id;
                    edge.source = currNode.id;
                    setEdgeAttr(edge, currNode, node);
                }
                if (old) {
                    edges.add(edge);
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
            //Logger.error("couldn't find any parents for URN: " + urn);
        }
        return parents;
    }

    private static List<String> getChildren(String urn) {
        List<String> children = getJdbcTemplate().queryForList(GET_CHILDREN, String.class, urn);
        if (children == null || children.size() == 0) {
            //Logger.error("couldn't find any children for URN: " + urn);
        }
        return children;
    }

    private static void setEdgeAttr(LineageEdgeLite edge, LineageNodeLite source, LineageNodeLite target) {
        setEdgeLabel(edge, source, target);
        setEdgeType(edge, source, target);
        setEdgeStyle(edge, source, target);
    }

    private static void setEdgeLabel(LineageEdgeLite edge, LineageNodeLite source, LineageNodeLite target) {
        List<String> labelqueries = new ArrayList<String>();
        String label = "";
        attrQueries(labelqueries, "label", source, target);
        for (int i = 0; i < labelqueries.size(); i++) {
            label = getProp(labelqueries.get(i));
            if (label != "default") {
                break;
            }
        }
        if (label != "default") {
            edge.label = label;
        } else {
            edge.label = edgeLabelDefaults(source.node_type, target.node_type);
        }
    }

    private static void setEdgeType(LineageEdgeLite edge, LineageNodeLite source, LineageNodeLite target) {
        List<String> typequeries = new ArrayList<String>();
        String type = "";
        attrQueries(typequeries, "type", source, target);
        for (int i = 0; i < typequeries.size(); i++) {
            type = getProp(typequeries.get(i));
            //Logger.info("type for source " + getPrefix(source.urn) + " is " + type);
            if (type != "default") {
                break;
            }
        }
        if (type != "default") {
            edge.type = type;
        } else if (source.node_type == "app") {
            edge.type = "job";
        } else {
            edge.type = "default";
        }
    }

    private static void setEdgeStyle(LineageEdgeLite edge, LineageNodeLite source, LineageNodeLite target) {
        List<String> stylequeries = new ArrayList<String>();
        String style = "";
        attrQueries(stylequeries, "style", source, target);
        for (int i = 0; i < stylequeries.size(); i++) {
            style = getProp(stylequeries.get(i));
            if (style != "default") {
                break;
            }
        }
        if (style != "default") {
            edge.style = style;
        } else {
            edge.style = "";
        }
    }

    private static String edgeLabelDefaults(String source, String target) {
        if ((source == "data" && target == "data") || (source == "data" && target == "db") || (source == "db" && target == "db") || (source == "db" && target == "data")) {
            return "source for";
        } else if (source == "data" && target == "app") {
            return "read by";
        } else if (source == "app" && target == "app") {
            return "spawned";
        } else if ((source == "app" && target == "data") || (source == "app" && target == "db")) {
            return "created";
        } else if (source == "db" && target == "app") {
            return "imported by";
        } else {
            return "influenced";
        }
    }

    private static void attrQueries(List<String> queries, String attr, LineageNodeLite source, LineageNodeLite target) {
        queries.add("edge." + attr + ".between." + getPrefix(source.urn) + "." + getPrefix(target.urn));
        queries.add("edge." + attr + ".from." + getPrefix(source.urn));
        queries.add("edge." + attr + ".to." + getPrefix(target.urn));
        queries.add("edge." + attr + ".between." + source.node_type + "." + target.node_type);
        queries.add("edge." + attr + ".from." + source.node_type);
        queries.add("edge." + attr + ".to." + target.node_type);
    }


    private static void assignApp(LineageNodeLite node) {
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_DATA_ATTR, parameters);

        for (Map<String, Object> row : rows) {
            // node only knows id, level, and urn, assign all other attributes

            // stored in dict_dataset, so has those fields
            JsonNode prop = Json.parse((String) row.get("properties"));

            // properties is a JsonNode, extract what we want out of it
            node.description = (prop.has("description")) ? prop.get("description").asText() : "null";
            node.app_code = (prop.has("app_code")) ? prop.get("app_code").asText() : "null";

            // check wh_property for a user specified color, use some generic defaults if nothing found
            //node.color = getColor(node.urn, node.node_type);

            // set things to show up in tooltip
            node._sort_list.add("app_code");
            node._sort_list.add("description");
        }
    }

    private static void assignData(LineageNodeLite node) {
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_DATA_ATTR, parameters);

        for (Map<String, Object> row : rows) {
            // node only knows id, level, and urn, assign all other attributes
            JsonNode prop = Json.parse((String) row.get("properties"));
            node.description = (prop.has("description")) ? prop.get("description").asText() : "null";
            node.source = (String) row.get("source");
            node.storage_type = (String) row.get("dataset_type"); // what the js calls storage_type, the sql calls dataset_type
            node.dataset_type = (String) row.get("dataset_type");

            // check wh_property for a user specified color, use some generic defaults if nothing found
            //node.color = getColor(node.urn, node.node_type);

            //node.abstracted_path = getPostfix(node.urn);

            // set things to show up in tooltip
            node._sort_list.add("abstracted_path");
            node._sort_list.add("storage_type");
        }
    }

    private static void assignDB(LineageNodeLite node) {
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_DATA_ATTR, parameters);
        // node only knows id, level, and urn, assign all other attributes

        for (Map<String, Object> row : rows) {
            JsonNode prop = Json.parse((String) row.get("properties"));
            node.description = (prop.has("description")) ? prop.get("description").asText() : "null";
            node.jdbc_url = (prop.has("jdbc_url")) ? prop.get("jdbc_url").asText() : "null";
            node.db_code = (prop.has("db_code")) ? prop.get("db_code").asText() : "null";

            // check wh_property for a user specified color, use some generic defaults if nothing found
            //node.color = getColor(node.urn, node.node_type);

            // set things to show up in tooltip
            node._sort_list.add("db_code");
            //node._sort_list.add("last_modified");
        }
    }

    private static void assignGeneral(LineageNodeLite node) {
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_DATA_ATTR, parameters);


        for (Map<String, Object> row : rows) {
            node.name = (String) row.get("name");
            node.schema = (String) row.get("schema");

            // check wh_property for a user specified color, use some generic defaults if nothing found
            node.color = getNodeColor(node.urn, node.node_type);

            // set things to show up in tooltip
            node._sort_list.add("urn");
            node._sort_list.add("name");
        }
    }

    private static Boolean assignPrefs(LineageNodeLite node) {
        // first try to get property list
        String properties = getProp("prop." + getPrefix(node.urn));
        if (properties == "default") {
            //Logger.info("no properties for " + getPrefix(node.urn));
            properties = getProp("prop." + node.node_type);
            if (properties == "default") {
                //Logger.info("no properties for " + node.node_type);
                return false;
            }
        }
        List<String> propList = Arrays.asList(properties.split(","));
        //Logger.debug("propList: " + propList);

        // now get all the values that dict_dataset has
        List<Map<String, Object>> rows = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", node.urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        rows = namedParameterJdbcTemplate.queryForList(GET_DATA_ATTR, parameters);

        for (Map<String, Object> row : rows) {
            JsonNode prop = Json.parse((String) row.get("properties"));
            for (String p : propList) {
                try {
                    if (p.substring(0, 5) == "prop/" && prop != null) {
                        node.setStringField(p, (String) prop.get(p.substring(5)).asText());
                    } else {
                        node.setStringField(p, (String) row.get(p));
                    }
                } catch (NoSuchFieldException fe) {
                    Logger.error("field " + p + " is non exsistant");
                } catch (IllegalAccessException e) {
                    Logger.error("field " + p + " is  private");
                }
            }
        }

        String sortsattr = getProp("prop.sortlist." + getPrefix(node.urn));
        if (sortsattr == "default") {
            //Logger.info("no sortlist for " + getPrefix(node.urn));
            sortsattr = getProp("prop.sortlist." + node.node_type);
            if (sortsattr == "default") {
                //Logger.info("no sortlist for " + node.node_type);
                return false;
            }
        }
        List<String> sortList = Arrays.asList(sortsattr.split(","));
        Logger.debug("sortList: " + sortList);
        for (String sl : sortList) {
            node._sort_list.add(sl);
        }
        return true;
    }

    // assigns attributes to ImpactDataset instance
    private static ImpactDataset assignImpactDataset(String idurn) {
        int level = Integer.parseInt(idurn.substring(0, idurn.indexOf("****")));
        String urn = idurn.substring(idurn.indexOf("****") + 4);
        ImpactDataset impD = new ImpactDataset();

        Map<String, Object> row = null;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("urn", urn);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());

        try {
            row = namedParameterJdbcTemplate.queryForMap(GET_DATA_ATTR, parameters);
        } catch (IncorrectResultSizeDataAccessException irsdae) {
            Logger.error("Incorrect result size ", irsdae);
        } catch (DataAccessException dae) {
            Logger.error("SQL query failed ", dae);
        }

        impD.urn = urn;
        impD.level = level;
        impD.name = (String) row.get("name");
        impD.id = (Long) row.get("id");
        impD.datasetUrl = "#/datasets/" + impD.id;
        JsonNode prop = Json.parse((String) row.get("properties"));
        if (prop.has("valid") && (prop.get("valid").asText()) == "true") {
            impD.isValidDataset = true;
        } else {
            impD.isValidDataset = false;
        }
        return impD;
    }

    public static ObjectNode getFlowLineage(String application, String project, Long flowId) {
        ObjectNode resultNode = Json.newObject();
        return resultNode;
    }

    // more or less gets all children which are datasets
    private static void getImpactDatasets(List<String> searchUrnList, List<ImpactDataset> impactDatasets) {
        if (searchUrnList != null && searchUrnList.size() > 0) {
            if (impactDatasets == null) {
                impactDatasets = new ArrayList<>();
            }

            boolean used = false;
            int level = 0;
            List<String> impactedUrns = new ArrayList<>();
            List<String> nextSearchUrnList = new ArrayList<>();
            List<String> nextSearchUrnListPreview = new ArrayList<>();

            while (searchUrnList.size() > 0) {
                for (String surn : searchUrnList) {


                    // there has got to be  better way to avoid dupes, right?
                    nextSearchUrnListPreview = new ArrayList<>(getChildren(surn));
                    for (int i = 0; i < nextSearchUrnListPreview.size(); i++) {
                        used = false;
                        for (int j = i + 1; j < nextSearchUrnListPreview.size(); j++) {
                            if (nextSearchUrnListPreview.get(i).equals(nextSearchUrnListPreview.get(j))) {
                                used = true;
                                break;
                            }
                        }
                        if (!used) {
                            nextSearchUrnList.add(nextSearchUrnListPreview.get(i));
                        }
                    }
                    

                    //Logger.info(surn + " is marked as " + getNodeType(surn));
                    if (getNodeType(surn).equals("data")) {
                        //Logger.debug(surn + " is marked as data");
                        // if the urn is already in the list ofimpacted datasets, then we don't want to add it to impactedUrns
                        used = false;
                        for (String imp : impactedUrns) {
                            if (imp.substring(imp.indexOf("****") + 4).equals(surn)) {
                                used = true;
                                break;
                            }
                        }
                        if (!used) {
                            impactedUrns.add(level + "****" + surn); // the astricks are used to seperate the encoded level for easier decoding
                        }
                    }
                }


                searchUrnList = new ArrayList<>(nextSearchUrnList);
                nextSearchUrnList = new ArrayList<>();
                level++;
            }


            for (String idurn: impactedUrns) {
                impactDatasets.add(assignImpactDataset(idurn));
            }


            Collections.sort(impactDatasets, new Comparator<ImpactDataset>() {
                public int compare(ImpactDataset imp1, ImpactDataset imp2) {
                    if (imp1.level < imp2.level) return -1;
                    if (imp1.level > imp2.level) return 1;
                    int order = imp1.name.compareTo(imp2.name);
                    if (order < 0) return -1;
                    if (order > 0) return 1;
                    return 0;
                }
            });


        }
    }

    // essentially a wrapper for getImpactDatasets
    public static List<ImpactDataset> getImpactDatasetsByUrn(String urn) {
        List<ImpactDataset> impactDatasetList = new ArrayList<ImpactDataset>();

         if (StringUtils.isNotBlank(urn))
         {
             List<String> searchUrnList = new ArrayList<String>();
             searchUrnList.add(urn);
             getImpactDatasets(searchUrnList, impactDatasetList);
         }

        return impactDatasetList;

    }
}
