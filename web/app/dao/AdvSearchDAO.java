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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Dataset;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import play.Logger;
import play.Play;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.*;

import java.util.*;

public class AdvSearchDAO extends AbstractMySQLOpenSourceDAO
{
	public final static String GET_DATASET_SOURCES = "SELECT source " +
			"FROM dict_dataset GROUP BY 1 ORDER BY count(*) DESC";

	public final static String GET_FLOW_APPCODES = "SELECT DISTINCT app_code " +
			"FROM cfg_application GROUP BY 1 ORDER BY 1";

	public final static String GET_DATASET_SCOPES = "SELECT DISTINCT parent_name " +
			"FROM dict_dataset WHERE parent_name is not null order by 1;";

	public final static String GET_DATASET_TABLE_NAMES_BY_SCOPE = "SELECT DISTINCT name " +
			"FROM dict_dataset WHERE parent_name in (:scopes)";

	public final static String GET_FLOW_NAMES_BY_APP = "SELECT DISTINCT f.flow_name " +
			"FROM flow f JOIN cfg_application a on f.app_id = a.app_id WHERE app in (:apps)";

	public final static String GET_DATASET_TABLE_NAMES = "SELECT DISTINCT name FROM dict_dataset ORDER BY 1";

	public final static String GET_FLOW_NAMES = "SELECT DISTINCT flow_name FROM flow ORDER BY 1";

	public final static String GET_JOB_NAMES = "SELECT DISTINCT job_name " +
			"FROM flow_job GROUP BY 1 ORDER BY 1";

	public final static String GET_DATASET_FIELDS = "SELECT DISTINCT field_name " +
			"FROM dict_field_detail ORDER BY 1";

	public final static String GET_DATASET_FIELDS_BY_TABLE_NAMES = "SELECT DISTINCT f.field_name " +
			"FROM dict_field_detail f join dict_dataset d on f.dataset_id = d.id where d.name regexp";

	public final static String SEARCH_DATASETS_BY_COMMENTS_WITH_PAGINATION = "SELECT SQL_CALC_FOUND_ROWS " +
			"id, name, source, urn, `schema` FROM dict_dataset where id in ( " +
			"SELECT dataset_id FROM comments WHERE MATCH(text) against ('*$keyword*' in BOOLEAN MODE) ) " +
			"UNION ALL SELECT id, name, source, urn, `schema` from dict_dataset " +
			"WHERE id in ( SELECT DISTINCT dataset_id FROM " +
			"dict_dataset_field_comment WHERE comment_id in " +
			"(SELECT id FROM field_comments where MATCH(comment) against ('*$keyword*' in BOOLEAN MODE))) " +
			"ORDER BY 2 LIMIT ?, ?";

	public final static String ADVSEARCH_RANK_CLAUSE = " ORDER BY CASE WHEN $condition1 THEN 0 " +
			"WHEN $condition2 THEN 2 WHEN $condition3 THEN 3 WHEN $condition4 THEN 4 ELSE 9 END, " +
			"CASE WHEN urn LIKE 'teradata://DWH_%' THEN 2 WHEN urn LIKE 'hdfs://data/tracking/%' THEN 1 " +
			"WHEN urn LIKE 'teradata://DWH/%' THEN 3 WHEN urn LIKE 'hdfs://data/databases/%' THEN 4 " +
			"WHEN urn LIKE 'hdfs://data/dervied/%' THEN 5 ELSE 99 END, urn";

	public final static String DATASET_BY_COMMENT_PAGINATION_IN_CLAUSE = "SELECT SQL_CALC_FOUND_ROWS " +
			"id, name, source, `schema`, urn, FROM_UNIXTIME(source_modified_time) as modified " +
			"FROM dict_dataset WHERE id IN ( " +
			"SELECT dataset_id FROM comments WHERE MATCH(text) " +
			"AGAINST ('*$keyword*' in BOOLEAN MODE) and dataset_id in ($id_list)) " +
			"UNION ALL SELECT id, name, source, `schema`, urn, FROM_UNIXTIME(source_modified_time) as modified " +
			"FROM dict_dataset WHERE id IN (SELECT DISTINCT dataset_id FROM " +
			"dict_dataset_field_comment WHERE comment_id in " +
			"(SELECT id FROM field_comments where MATCH(comment) against ('*$keyword*' in BOOLEAN MODE)) ) " +
			"ORDER BY 2 LIMIT ?, ?;";

	public final static String ADV_SEARCH_FLOW = "SELECT SQL_CALC_FOUND_ROWS " +
			"a.app_code, f.flow_id, f.flow_name, f.flow_path, f.flow_group FROM flow f " +
			"JOIN cfg_application a on f.app_id = a.app_id ";

	public final static String ADV_SEARCH_JOB = "SELECT SQL_CALC_FOUND_ROWS " +
			"a.app_code, f.flow_name, f.flow_path, f.flow_group, j.flow_id, j.job_id, " +
			"j.job_name, j.job_path, j.job_type " +
			"FROM flow_job j JOIN flow f on j.app_id = f.app_id  AND j.flow_id = f.flow_id " +
			"JOIN cfg_application a on j.app_id = a.app_id ";

	public final static String ADV_SEARCH_METRIC = "SELECT SQL_CALC_FOUND_ROWS metric_id, " +
			"metric_name, metric_description, dashboard_name, metric_group, metric_category, " +
			"metric_sub_category, metric_level, metric_source_type, metric_source, " +
			"metric_source_dataset_id, metric_ref_id_type, metric_ref_id, metric_type, metric_grain, " +
			"metric_display_factor, metric_display_factor_sym, metric_good_direction, " +
			"metric_formula, dimensions, owners, tags, urn, metric_url, wiki_url, scm_url, 0 as watch_id " +
			"FROM dict_business_metric ";

	public static List<String> getDatasetSources()
	{
        //Logger.debug("Entering AdvSearch.java:getDatasetSources()");
		return getJdbcTemplate().queryForList(GET_DATASET_SOURCES, String.class);
	}

	public static List<String> getDatasetScopes()
	{
        //Logger.debug("Entering AdvSearch.java:getDatasetScopes()");
		return getJdbcTemplate().queryForList(GET_DATASET_SCOPES, String.class);
	}

	public static List<String> getTableNames(String scopes)
	{
        //Logger.debug("Entering AdvSearch.java:getTableNames()");
		List<String> tables = null;
		if (StringUtils.isNotBlank(scopes))
		{
			String[] scopeArray = scopes.split(",");
			List<String> scopeList = Arrays.asList(scopeArray);
			Map<String, List> param = Collections.singletonMap("scopes", scopeList);
			NamedParameterJdbcTemplate namedParameterJdbcTemplate = new
					NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());
			tables = namedParameterJdbcTemplate.queryForList(
					GET_DATASET_TABLE_NAMES_BY_SCOPE, param, String.class);
		}
		else
		{
			tables = getJdbcTemplate().queryForList(GET_DATASET_TABLE_NAMES, String.class);
		}

		return tables;
	}

	public static List<String> getFields(String tables)
	{
        //Logger.debug("Entering AdvSearch.java:getFields()");
		String query = null;
		if (StringUtils.isNotBlank(tables))
		{
			String[] tableArray = tables.split(",");
			query = GET_DATASET_FIELDS_BY_TABLE_NAMES;
			query += "'";
			for(int i = 0; i < tableArray.length; i++)
			{
				if (i == 0)
				{
					query += tableArray[i];
				}
				else
				{
					query += "|" + tableArray[i];
				}
			}
			query += "' order by 1";
		}
		else
		{
			query = GET_DATASET_FIELDS;
		}

		return getJdbcTemplate().queryForList(query, String.class);
	}

	public static List<String> getFlowApplicationCodes()
	{
		return getJdbcTemplate().queryForList(GET_FLOW_APPCODES, String.class);
	}

	public static List<String> getFlowNames(String applications)
	{
        //ogger.debug("Entering AdvSearch.java:getFlowNames()");
		List<String> flowNames = null;
		if (StringUtils.isNotBlank(applications))
		{
			String[] appArray = applications.split(",");
			List<String> appList = Arrays.asList(appArray);
			Map<String, List> param = Collections.singletonMap("apps", appList);
			NamedParameterJdbcTemplate namedParameterJdbcTemplate = new
					NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());
			flowNames = namedParameterJdbcTemplate.queryForList(
					GET_FLOW_NAMES_BY_APP, param, String.class);
		}
		else
		{
			flowNames = getJdbcTemplate().queryForList(GET_FLOW_NAMES, String.class);
		}

		return flowNames;
	}

	public static List<String> getFlowJobNames()
	{
        //Logger.debug("Entering AdvSearch.java:getFlowJobNames()");
		return getJdbcTemplate().queryForList(GET_JOB_NAMES, String.class);
	}

	public static ObjectNode elasticSearch(JsonNode searchOpt, int page, int size)
	{
        //Logger.debug("Entering AdvSearch.java:elasticSearch()");
		ObjectNode resultNode = Json.newObject();
		Long count = 0L;
		List<Dataset> pagedDatasets = new ArrayList<>();
		ObjectNode queryNode = Json.newObject();
		queryNode.put("from", (page-1)*size);
		queryNode.put("size", size);

		JsonNode searchNode = utils.Search.generateDatasetAdvSearchQueryString(searchOpt);

		if (searchNode != null && searchNode.isContainerNode())
		{
			queryNode.set("query", searchNode);
		}

		Logger.info(" === AdvSearchDAO::elasticSearch === The query sent to Elastic Search is: " + queryNode.toString());

		Promise<WSResponse> responsePromise = WS.url(
				Play.application().configuration().getString(
						SearchDAO.ELASTICSEARCH_DATASET_URL_KEY)).post(queryNode);
		JsonNode responseNode = responsePromise.get(1000).asJson();

		resultNode.put("page", page);
		resultNode.put("category", "Datasets");
		resultNode.put("itemsPerPage", size);

		if (responseNode != null && responseNode.isContainerNode() && responseNode.has("hits")) {
			JsonNode hitsNode = responseNode.get("hits");
			if (hitsNode != null) {
				if (hitsNode.has("total")) {
					count = hitsNode.get("total").asLong();
				}
				if (hitsNode.has("hits")) {
					JsonNode dataNode = hitsNode.get("hits");
					if (dataNode != null && dataNode.isArray()) {
						Iterator<JsonNode> arrayIterator = dataNode.elements();
						if (arrayIterator != null) {
							while (arrayIterator.hasNext()) {
								JsonNode node = arrayIterator.next();
								if (node.isContainerNode() && node.has("_id")) {
									Dataset dataset = new Dataset();
									dataset.id = node.get("_id").asLong();
									if (node.has("_source")) {
										JsonNode sourceNode = node.get("_source");
										if (sourceNode != null) {
											if (sourceNode.has("name")) {
												dataset.name = sourceNode.get("name").asText();
											}
											if (sourceNode.has("source")) {
												dataset.source = sourceNode.get("source").asText();
											}
											if (sourceNode.has("urn")) {
												dataset.urn = sourceNode.get("urn").asText();
											}
											if (sourceNode.has("schema")) {
												dataset.schema = sourceNode.get("schema").asText();
											}
										}
									}
									pagedDatasets.add(dataset);
								}
							}
						}
					}
				}
			}
		}
		resultNode.put("count", count);
		resultNode.put("totalPages", (int)Math.ceil(count/((double)size)));
		resultNode.set("data", Json.toJson(pagedDatasets));
		return resultNode;
	}

	public static ObjectNode search(JsonNode searchOpt, int page, int size)
	{
		//Logger.debug("Entering AdvSearchDAO.java:search() " + searchOpt.asText());
		ObjectNode resultNode = Json.newObject();
		int count = 0;
		List<String> scopeInList = new ArrayList<String>();
		List<String> scopeNotInList = new ArrayList<String>();
		List<String> tableInList = new ArrayList<String>();
		List<String> tableNotInList = new ArrayList<String>();
		List<String> fieldAnyList = new ArrayList<String>();
		List<String> fieldAllList = new ArrayList<String>();
		List<String> fieldNotInList = new ArrayList<String>();
		String fieldAllIDs = "";
		String comments = "";

		if (searchOpt != null && (searchOpt.isContainerNode()))
		{
			if (searchOpt.has("scope")) {
				JsonNode scopeNode = searchOpt.get("scope");
				if (scopeNode != null && scopeNode.isContainerNode())
				{
					if (scopeNode.has("in"))
					{
						JsonNode scopeInNode = scopeNode.get("in");
						if (scopeInNode != null)
						{
							String scopeInStr = scopeInNode.asText();
							if (StringUtils.isNotBlank(scopeInStr))
							{
								String[] scopeInArray = scopeInStr.split(",");
								if (scopeInArray != null)
								{
									for(String value : scopeInArray)
									{
										if (StringUtils.isNotBlank(value))
										{
											scopeInList.add(value.trim());
										}
									}
								}
							}
						}
					}
					if (scopeNode.has("not"))
					{
						JsonNode scopeNotInNode = scopeNode.get("not");
						if (scopeNotInNode != null)
						{
							String scopeNotInStr = scopeNotInNode.asText();
							if (StringUtils.isNotBlank(scopeNotInStr))
							{
								String[] scopeNotInArray = scopeNotInStr.split(",");
								if (scopeNotInArray != null)
								{
									for(String value : scopeNotInArray)
									{
										if (StringUtils.isNotBlank(value))
										{
											scopeNotInList.add(value.trim());
										}
									}
								}
							}
						}
					}
				}
			}

			if (searchOpt.has("table")) {
				JsonNode tableNode = searchOpt.get("table");
				if (tableNode != null && tableNode.isContainerNode())
				{
					if (tableNode.has("in"))
					{
						JsonNode tableInNode = tableNode.get("in");
						if (tableInNode != null)
						{
							String tableInStr = tableInNode.asText();
							if (StringUtils.isNotBlank(tableInStr))
							{
								String[] tableInArray = tableInStr.split(",");
								if (tableInArray != null)
								{
									for(String value : tableInArray)
									{
										if (StringUtils.isNotBlank(value))
										{
											tableInList.add(value.trim());
										}
									}
								}
							}
						}
					}
					if (tableNode.has("not"))
					{
						JsonNode tableNotInNode = tableNode.get("not");
						if (tableNotInNode != null)
						{
							String tableNotInStr = tableNotInNode.asText();
							if (StringUtils.isNotBlank(tableNotInStr))
							{
								String[] tableNotInArray = tableNotInStr.split(",");
								if (tableNotInArray != null)
								{
									for(String value : tableNotInArray)
									{
										if (StringUtils.isNotBlank(value))
										{
											tableNotInList.add(value.trim());
										}
									}
								}
							}
						}
					}
				}
			}

			if (searchOpt.has("fields")) {
				JsonNode fieldNode = searchOpt.get("fields");
				if (fieldNode != null && fieldNode.isContainerNode())
				{
					if (fieldNode.has("any"))
					{
						JsonNode fieldAnyNode = fieldNode.get("any");
						if (fieldAnyNode != null)
						{
							String fieldAnyStr = fieldAnyNode.asText();
							if (StringUtils.isNotBlank(fieldAnyStr))
							{
								String[] fieldAnyArray = fieldAnyStr.split(",");
								if (fieldAnyArray != null)
								{
									for(String value : fieldAnyArray)
									{
										if (StringUtils.isNotBlank(value))
										{
											fieldAnyList.add(value.trim());
										}
									}
								}
							}
						}
					}
					if (fieldNode.has("all"))
					{
						JsonNode fieldAllNode = fieldNode.get("all");
						if (fieldAllNode != null)
						{
							String fieldAllStr = fieldAllNode.asText();
							if (StringUtils.isNotBlank(fieldAllStr))
							{
								String[] fieldAllArray = fieldAllStr.split(",");
								if (fieldAllArray != null)
								{
									for(String value : fieldAllArray)
									{
										if (StringUtils.isNotBlank(value))
										{
											fieldAllList.add(value.trim());
										}
									}
								}
							}
						}
					}
					if (fieldNode.has("not"))
					{
						JsonNode fieldNotInNode = fieldNode.get("not");
						if (fieldNotInNode != null)
						{
							String fieldNotInStr = fieldNotInNode.asText();
							if (StringUtils.isNotBlank(fieldNotInStr))
							{
								String[] fieldNotInArray = fieldNotInStr.split(",");
								if (fieldNotInArray != null)
								{
									for(String value : fieldNotInArray)
									{
										if (StringUtils.isNotBlank(value))
										{
											fieldNotInList.add(value.trim());
										}
									}
								}
							}
						}
					}
				}
			}

			String datasetSources = "";
			if (searchOpt.has("sources")) {
				JsonNode sourcesNode = searchOpt.get("sources");
				if (sourcesNode != null)
				{
					datasetSources = sourcesNode.asText();
				}
			}

			boolean needAndKeyword = false;
			int fieldQueryIndex = 0;
			if (fieldAllList.size() > 0)
			{
				String fieldAllQuery = "SELECT DISTINCT f1.dataset_id FROM dict_field_detail f1 ";
				String fieldWhereClause = " WHERE ";
				for (String field : fieldAllList)
				{
					fieldQueryIndex++;
					if (fieldQueryIndex == 1)
					{
						fieldWhereClause += "f1.field_name LIKE '%" + field + "%' ";
					}
					else
					{
						fieldAllQuery += "JOIN dict_field_detail f" + fieldQueryIndex + " ON f" +
								(fieldQueryIndex-1) + ".dataset_id = f" + fieldQueryIndex + ".dataset_id ";
						fieldWhereClause += " and f" + fieldQueryIndex + ".field_name LIKE '%" + field + "%' ";

					}
				}
				fieldAllQuery += fieldWhereClause;
				List<Map<String, Object>> rows = getJdbcTemplate().queryForList(fieldAllQuery);
				for (Map row : rows) {

					fieldAllIDs += (Long)row.get("dataset_id") + ",";
				}
				if (fieldAllIDs.length() > 0)
				{
					fieldAllIDs = fieldAllIDs.substring(0, fieldAllIDs.length()-1);
				}
				if (StringUtils.isBlank(fieldAllIDs))
				{
					fieldAllIDs = Integer.toString(0);

				}
			}

			List<Dataset> pagedDatasets = new ArrayList<Dataset>();
			final JdbcTemplate jdbcTemplate = getJdbcTemplate();
			javax.sql.DataSource ds = jdbcTemplate.getDataSource();
			DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);

			TransactionTemplate txTemplate = new TransactionTemplate(tm);

			ObjectNode result;

			if (searchOpt.has("comments"))
			{
				JsonNode commentsNode = searchOpt.get("comments");
				if (commentsNode != null)
				{
					comments = commentsNode.asText();
					if (StringUtils.isNotBlank(comments))
					{
						if (scopeInList.size() == 0 && scopeNotInList.size() == 0
								&& tableInList.size() == 0 && tableNotInList.size() == 0
								&& fieldAllList.size() == 0 && fieldAnyList.size() == 0 && fieldNotInList.size() == 0)
						{
							final String commentsQueryStr =
									SEARCH_DATASETS_BY_COMMENTS_WITH_PAGINATION.replace("$keyword", comments);

							result = txTemplate.execute(new TransactionCallback<ObjectNode>()
							{
								public ObjectNode doInTransaction(TransactionStatus status)
								{
									List<Map<String, Object>> rows = null;
									rows = jdbcTemplate.queryForList(commentsQueryStr, (page-1)*size, size);

									for (Map row : rows) {

										Dataset ds = new Dataset();
										ds.id = (Long)row.get("id");
										ds.name = (String)row.get("name");
										ds.source = (String)row.get("source");
										ds.urn = (String)row.get("urn");
										ds.schema = (String)row.get("schema");
										pagedDatasets.add(ds);
									}
									long count = 0;
									try {
										count = jdbcTemplate.queryForObject(
												"SELECT FOUND_ROWS()",
												Long.class);
									}
									catch(EmptyResultDataAccessException e)
									{
										Logger.error("Exception = " + e.getMessage());
									}

									ObjectNode resultNode = Json.newObject();
									resultNode.put("count", count);
									resultNode.put("page", page);
									resultNode.put("itemsPerPage", size);
									resultNode.put("totalPages", (int)Math.ceil(count/((double)size)));
									resultNode.set("data", Json.toJson(pagedDatasets));

									return resultNode;
								}
							});
							return result;
						}
					}
				}
			}

			String query = "";
			if (StringUtils.isNotBlank(comments))
			{
				query = "SELECT DISTINCT d.id FROM dict_dataset d";
			}
			else
			{
				query = "SELECT SQL_CALC_FOUND_ROWS " +
						"DISTINCT d.id, d.name, d.schema, d.source, d.urn, " +
						"FROM_UNIXTIME(d.source_modified_time) as modified FROM dict_dataset d";
			}
			if (fieldAllList.size() > 0 || fieldAnyList.size() > 0 || fieldNotInList.size() > 0)
			{
				String fieldQuery = "SELECT DISTINCT dataset_id FROM dict_field_detail f WHERE (";
				query += " WHERE d.id IN ( ";
				query += fieldQuery;
				String whereClause = "";
				boolean fieldNeedAndKeyword = false;
				if (fieldAnyList.size() > 0)
				{
					whereClause = " (";
					int indexForAnyList = 0;
					for (String field : fieldAnyList)
					{
						if (indexForAnyList == 0)
						{
							whereClause += "f.field_name LIKE '%" + field + "%'";
						}
						else
						{
							whereClause += " or f.field_name LIKE '%" + field + "%'";
						}
						indexForAnyList++;
					}
					whereClause += " ) ";
					fieldNeedAndKeyword = true;
					query += whereClause;
				}
				if (fieldAllList.size() > 0 && StringUtils.isNotBlank(fieldAllIDs))
				{
					if (fieldNeedAndKeyword)
					{
						whereClause = " and (";
					}
					else
					{
						whereClause = " (";
					}
					whereClause += "f.dataset_id IN (" + fieldAllIDs + ")";
					whereClause += " ) ";
					query += whereClause;
					fieldNeedAndKeyword = true;
				}
				if (fieldNotInList.size() > 0)
				{
					if (fieldNeedAndKeyword)
					{
						whereClause = " and ( f.dataset_id not in (select dataset_id from dict_field_detail where";
					}
					else
					{
						whereClause = " ( f.dataset_id not in (select dataset_id from dict_field_detail where";
					}
					int indexForNotInList = 0;
					for (String field : fieldNotInList)
					{
						if (indexForNotInList == 0)
						{
							whereClause += " field_name LIKE '%" + field + "%'";
						}
						else
						{
							whereClause += " or field_name LIKE '%" + field + "%'";
						}
						indexForNotInList++;
					}
					whereClause += " )) ";
					query += whereClause;
					fieldNeedAndKeyword = true;
				}
				needAndKeyword = true;
				query += ") )";
			}

			if (scopeInList.size() > 0 || scopeNotInList.size() > 0)
			{
				if (needAndKeyword)
				{
					query += " and";
				}
				else
				{
					query += " where";
				}
				boolean scopeNeedAndKeyword = false;
				if (scopeInList.size() > 0)
				{
					query += " d.parent_name in (";
					scopeNeedAndKeyword = true;
					int indexForScopeInList = 0;
					for (String scope : scopeInList)
					{
						if (indexForScopeInList == 0)
						{
							query += "'" + scope + "'";
						}
						else
						{
							query += ", '" + scope + "'";
						}
						indexForScopeInList++;
					}
					query += ") ";
				}
				if (scopeNotInList.size() > 0)
				{
					if (scopeNeedAndKeyword)
					{
						query += " and d.parent_name not in (";
					}
					else
					{
						query += " d.parent_name not in (";
					}
					int indexForScopeNotInList = 0;
					for (String scope : scopeNotInList)
					{
						if (indexForScopeNotInList == 0)
						{
							query += "'" + scope + "'";
						}
						else
						{
							query += ", '" + scope + "'";
						}
						indexForScopeNotInList++;
					}
					query += ") ";
				}
				needAndKeyword = true;
			}
			String condition1 = "";
			String condition2 = "";
			String condition3 = "";
			String condition4 = "";

			if (tableInList.size() > 0 || tableNotInList.size() > 0)
			{
				if (needAndKeyword)
				{
					query += " and";
				}
				else
				{
					query += " where";
				}
				boolean tableNeedAndKeyword = false;
				if (tableInList.size() > 0)
				{
					query += " (";
					int indexForTableInList = 0;
					for (String table : tableInList)
					{
						if (indexForTableInList == 0)
						{
							query += "d.name LIKE '%" + table + "%'";
						}
						else
						{
							condition1 += " or ";
							condition2 += " or ";
							condition3 += " or ";
							condition4 += " or ";
							query += " or d.name LIKE '%" + table + "%'";
						}
						condition1 += "name = '" + table + "'";
						condition2 += "name LIKE '" + table + "%'";
						condition3 += "name LIKE '%" + table + "'";
						condition4 += "name LIKE '%" + table + "%'";
						indexForTableInList++;
					}
					query += " ) ";
					tableNeedAndKeyword = true;
				}
				if (tableNotInList.size() > 0)
				{
					if (tableNeedAndKeyword)
					{
						query += " and (";
					}
					else
					{
						query += " (";
					}
					int indexForTableNotInList = 0;
					for (String table : tableNotInList)
					{
						if (indexForTableNotInList == 0)
						{
							query += "d.name NOT LIKE '%" + table + "%'";
						}
						else
						{
							query += " and d.name NOT LIKE '%" + table + "%'";
						}
						indexForTableNotInList++;
					}
					query += " ) ";
				}
				needAndKeyword = true;
			}

			if (StringUtils.isNotBlank(datasetSources))
			{
				if (needAndKeyword)
				{
					query += " and";
				}
				else
				{
					query += " WHERE";
				}
				query += " d.source in (";
				String[] dataestSourceArray = datasetSources.split(",");
				for(int i = 0; i < dataestSourceArray.length; i++)
				{
					query += "'" + dataestSourceArray[i] + "'";
					if (i != (dataestSourceArray.length -1))
					{
						query += ",";
					}
				}
				query += ")";
			}
			if ((tableInList.size() > 0 || tableNotInList.size() > 0) &&
					StringUtils.isNotBlank(condition1) &&
					StringUtils.isNotBlank(condition2) &&
					StringUtils.isNotBlank(condition3) &&
					StringUtils.isNotBlank(condition4))
			{
				query += ADVSEARCH_RANK_CLAUSE.replace("$condition1", condition1)
						.replace("$condition2", condition2)
						.replace("$condition3", condition3)
						.replace("$condition4", condition4);
			}
			else
			{
				query += " ORDER BY CASE WHEN urn LIKE 'teradata://DWH_%' THEN 2 " +
						"WHEN urn LIKE 'hdfs://data/tracking/%' THEN 1 " +
						"WHEN urn LIKE 'teradata://DWH/%' THEN 3 " +
						"WHEN urn LIKE 'hdfs://data/databases/%' THEN 4 " +
						"WHEN urn LIKE 'hdfs://data/dervied/%' THEN 5 ELSE 99 end, urn";
			}
			if (StringUtils.isBlank(comments))
			{
				query += " LIMIT " + (page-1)*size + ", " + size;
				final String queryString = query;

				result = txTemplate.execute(new TransactionCallback<ObjectNode>()
				{
					public ObjectNode doInTransaction(TransactionStatus status)
					{
						List<Map<String, Object>> rows = null;
						rows = jdbcTemplate.queryForList(queryString);

						for (Map row : rows) {

							Dataset ds = new Dataset();
							ds.id = (Long)row.get("id");
							ds.name = (String)row.get("name");
							ds.source = (String)row.get("source");
							ds.urn = (String)row.get("urn");
							ds.schema = (String)row.get("schema");
							pagedDatasets.add(ds);
						}
						long count = 0;
						try {
							count = jdbcTemplate.queryForObject(
									"SELECT FOUND_ROWS()",
									Long.class);
						}
						catch(EmptyResultDataAccessException e)
						{
							Logger.error("Exception = " + e.getMessage());
						}

						ObjectNode resultNode = Json.newObject();
						resultNode.put("count", count);
						resultNode.put("page", page);
						resultNode.put("itemsPerPage", size);
						resultNode.put("totalPages", (int)Math.ceil(count/((double)size)));
						resultNode.set("data", Json.toJson(pagedDatasets));

						return resultNode;
					}
				});
				return result;
			}
			else
			{
				String datasetIDStr = "";
				final String queryString = query;

				datasetIDStr = txTemplate.execute(new TransactionCallback<String>()
				{
					public String doInTransaction(TransactionStatus status)
					{
						List<Map<String, Object>> rows = null;
						rows = jdbcTemplate.queryForList(queryString);
						String idsString = "";

						for (Map row : rows) {

							Long id = (Long)row.get("id");
							idsString += Long.toString(id) + ",";
						}
						if (StringUtils.isNotBlank(idsString))
						{
							idsString = idsString.substring(0, idsString.length()-1);
						}
						return idsString;
					}
				});
				if (StringUtils.isBlank(datasetIDStr))
				{
					resultNode.put("count", 0);
					resultNode.put("page", page);
					resultNode.put("itemsPerPage", size);
					resultNode.put("totalPages", 0);
					resultNode.set("data", Json.toJson(""));
					return resultNode;
				}
				final String commentsQueryWithConditionStr = DATASET_BY_COMMENT_PAGINATION_IN_CLAUSE.replace("$keyword", comments).
						replace("$id_list", datasetIDStr);
				result = txTemplate.execute(new TransactionCallback<ObjectNode>()
				{
					public ObjectNode doInTransaction(TransactionStatus status)
					{
						List<Map<String, Object>> rows = null;
						rows = jdbcTemplate.queryForList(commentsQueryWithConditionStr, (page-1)*size, size);

						for (Map row : rows) {

							Dataset ds = new Dataset();
							ds.id = (Long)row.get("id");
							ds.name = (String)row.get("name");
							ds.source = (String)row.get("source");
							ds.urn = (String)row.get("urn");
							ds.schema = (String)row.get("schema");
							pagedDatasets.add(ds);
						}
						long count = 0;
						try {
							count = jdbcTemplate.queryForObject(
									"SELECT FOUND_ROWS()",
									Long.class);
						}
						catch(EmptyResultDataAccessException e)
						{
							Logger.error("Exception = " + e.getMessage());
						}

						ObjectNode resultNode = Json.newObject();
						resultNode.put("count", count);
						resultNode.put("page", page);
						resultNode.put("itemsPerPage", size);
						resultNode.put("totalPages", (int)Math.ceil(count/((double)size)));
						resultNode.set("data", Json.toJson(pagedDatasets));

						return resultNode;
					}
				});
				return result;
			}
		}
		resultNode.put("count", 0);
		resultNode.put("page", page);
		resultNode.put("itemsPerPage", size);
		resultNode.put("totalPages", 0);
		resultNode.set("data", Json.toJson(""));
		return resultNode;
	}

}
