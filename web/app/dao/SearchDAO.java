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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import play.Logger;
import play.Play;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.*;
import play.cache.Cache;
import models.*;

public class SearchDAO extends AbstractMySQLOpenSourceDAO
{
	public static String ELASTICSEARCH_DATASET_URL_KEY = "elasticsearch.dataset.url";

	public static String ELASTICSEARCH_METRIC_URL_KEY = "elasticsearch.metric.url";

	public static String ELASTICSEARCH_FLOW_URL_KEY = "elasticsearch.flow.url";

	public static String WHEREHOWS_SEARCH_ENGINE__KEY = "search.engine";

	public final static String SEARCH_DATASET_WITH_PAGINATION = "SELECT SQL_CALC_FOUND_ROWS " +
			"id, `name`, `schema`, `source`, `urn`, FROM_UNIXTIME(source_modified_time) as modified, " +
			"rank_01 + rank_02 + rank_03 + rank_04 + rank_05 + rank_06 + rank_07 + rank_08 + rank_09 + " +
			"rank_10 + rank_11 + rank_12 + rank_13 + rank_14 + rank_15 as rank " +
			"FROM (SELECT id, `name`, `schema`, `source`, `urn`, source_modified_time, " +
			"CASE WHEN `name` = '$keyword' THEN 3000 ELSE 0 END rank_01, " +
			"CASE WHEN `name` like '$keyword%' THEN 2000 ELSE 0 END rank_02, " +
			"CASE WHEN `name` like '%$keyword%' THEN 1000 ELSE 0 END rank_03, " +
			"CASE WHEN `urn` = '$keyword' THEN 300 ELSE 0 END rank_04, " +
			"CASE WHEN `urn` like '$keyword%' THEN 200 ELSE 0 END rank_05, " +
			"CASE WHEN `urn` like '%$keyword%' THEN 100 ELSE 0 END rank_06, " +
			"CASE WHEN `schema` = '$keyword' THEN 30 ELSE 0 END rank_07, " +
			"CASE WHEN `schema` like '$keyword%' THEN 20 ELSE 0 END rank_08, " +
			"CASE WHEN `schema` like '%$keyword%' THEN 10 ELSE 0 END rank_09, " +
			"CASE WHEN `dataset_type` = '$keyword' THEN 30 ELSE 0 END rank_10, " +
			"CASE WHEN `dataset_type` LIKE '$keyword%' THEN 20 ELSE 0 END rank_11, " +
			"CASE WHEN `dataset_type` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_12, " +
			"CASE WHEN `fields` = '$keyword' THEN 30 ELSE 0 END rank_13, " +
			"CASE WHEN `fields` LIKE '$keyword%' THEN 20 ELSE 0 END rank_14, " +
			"CASE WHEN `fields` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_15 " +
			"FROM dict_dataset WHERE MATCH(`name`, `schema`,  `properties`, `urn`, `dataset_type`, `fields`)" +
			" AGAINST ('*$keyword* *v_$keyword* \"$keyword\"' IN BOOLEAN MODE) ) t " +
			"ORDER BY rank DESC, `name`, `urn` LIMIT ?, ?;";

	public final static String SEARCH_DATASET_WITH_PAGINATION_AND_TYPE = "SELECT SQL_CALC_FOUND_ROWS " +
			"id, `name`, `schema`, `source`, `urn`, FROM_UNIXTIME(source_modified_time) as modified, " +
			"rank_01 + rank_02 + rank_03 + rank_04 + rank_05 + rank_06 + rank_07 + rank_08 + rank_09 + " +
			"rank_10 + rank_11 + rank_12 + rank_13 + rank_14 + rank_15 as rank " +
			"FROM (SELECT id, `name`, `schema`, `source`, `urn`, source_modified_time, " +
			"CASE WHEN `name` = '$keyword' THEN 3000 ELSE 0 END rank_01, " +
			"CASE WHEN `name` like '$keyword%' THEN 2000 ELSE 0 END rank_02, " +
			"CASE WHEN `name` like '%$keyword%' THEN 1000 ELSE 0 END rank_03, " +
			"CASE WHEN `urn` = '$keyword' THEN 300 ELSE 0 END rank_04, " +
			"CASE WHEN `urn` like '$keyword%' THEN 200 ELSE 0 END rank_05, " +
			"CASE WHEN `urn` like '%$keyword%' THEN 100 ELSE 0 END rank_06, " +
			"CASE WHEN `schema` = '$keyword' THEN 30 ELSE 0 END rank_07, " +
			"CASE WHEN `schema` like '$keyword%' THEN 20 ELSE 0 END rank_08, " +
			"CASE WHEN `schema` like '%$keyword%' THEN 10 ELSE 0 END rank_09, " +
			"CASE WHEN `dataset_type` = '$keyword' THEN 30 ELSE 0 END rank_10, " +
			"CASE WHEN `dataset_type` LIKE '$keyword%' THEN 20 ELSE 0 END rank_11, " +
			"CASE WHEN `dataset_type` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_12, " +
			"CASE WHEN `fields` = '$keyword' THEN 30 ELSE 0 END rank_13, " +
			"CASE WHEN `fields` LIKE '$keyword%' THEN 20 ELSE 0 END rank_14, " +
			"CASE WHEN `fields` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_15 " +
			"FROM dict_dataset WHERE MATCH(`name`, `schema`,  `properties`, `urn`, `dataset_type`, `fields`)" +
			" AGAINST ('*$keyword* *v_$keyword* \"$keyword\"' IN BOOLEAN MODE) and `storage_type` = '$type' ) t " +
			"ORDER BY rank DESC, `name`, `urn` LIMIT ?, ?;";

	public final static String SEARCH_DATASET_BY_SOURCE_WITH_PAGINATION = "SELECt SQL_CALC_FOUND_ROWS " +
			"id, `name`, `schema`, `source`, `urn`, FROM_UNIXTIME(source_modified_time) as modified, " +
			"rank_01 + rank_02 + rank_03 + rank_04 + rank_05 + rank_06 + rank_07 + rank_08 + rank_09 + " +
			"rank_10 + rank_11 + rank_12 + rank_13 + rank_14 + rank_15 as rank " +
			"FROM (SELECT id, `name`, `schema`, `source`, `urn`, source_modified_time, " +
			"CASE WHEN `name` = '$keyword' THEN 3000 ELSE 0 END rank_01, " +
			"CASE WHEN `name` like '$keyword%' THEN 2000 ELSE 0 END rank_02, " +
			"CASE WHEN `name` like '%$keyword%' THEN 1000 ELSE 0 END rank_03, " +
			"CASE WHEN `urn` = '$keyword' THEN 300 ELSE 0 END rank_04, " +
			"CASE WHEN `urn` like '$keyword%' THEN 200 ELSE 0 END rank_05, " +
			"CASE WHEN `urn` like '%$keyword%' THEN 100 ELSE 0 END rank_06, " +
			"CASE WHEN `schema` = '$keyword' THEN 30 ELSE 0 END rank_07, " +
			"CASE WHEN `schema` like '$keyword%' THEN 20 ELSE 0 END rank_08, " +
			"CASE WHEN `schema` like '%$keyword%' THEN 10 ELSE 0 END rank_09, " +
			"CASE WHEN `dataset_type` = '$keyword' THEN 30 ELSE 0 END rank_10, " +
			"CASE WHEN `dataset_type` LIKE '$keyword%' THEN 20 ELSE 0 END rank_11, " +
			"CASE WHEN `dataset_type` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_12, " +
			"CASE WHEN `fields` = '$keyword' THEN 30 ELSE 0 END rank_13, " +
			"CASE WHEN `fields` LIKE '$keyword%' THEN 20 ELSE 0 END rank_14, " +
			"CASE WHEN `fields` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_15 " +
			"FROM dict_dataset WHERE MATCH(`name`, `schema`,  `properties`, `urn`, `dataset_type`, `fields`)" +
			" AGAINST ('*$keyword* *v_$keyword* \"$keyword\"' IN BOOLEAN MODE) and source = ? ) t " +
			"ORDER BY rank desc, `name`, `urn` LIMIT ?, ?;";

	public final static String SEARCH_DATASET_BY_SOURCE_WITH_PAGINATION_AND_TYPE = "SELECt SQL_CALC_FOUND_ROWS " +
			"id, `name`, `schema`, `source`, `urn`, FROM_UNIXTIME(source_modified_time) as modified, " +
			"rank_01 + rank_02 + rank_03 + rank_04 + rank_05 + rank_06 + rank_07 + rank_08 + rank_09 + " +
			"rank_10 + rank_11 + rank_12 + rank_13 + rank_14 + rank_15 as rank " +
			"FROM (SELECT id, `name`, `schema`, `source`, `urn`, source_modified_time, " +
			"CASE WHEN `name` = '$keyword' THEN 3000 ELSE 0 END rank_01, " +
			"CASE WHEN `name` like '$keyword%' THEN 2000 ELSE 0 END rank_02, " +
			"CASE WHEN `name` like '%$keyword%' THEN 1000 ELSE 0 END rank_03, " +
			"CASE WHEN `urn` = '$keyword' THEN 300 ELSE 0 END rank_04, " +
			"CASE WHEN `urn` like '$keyword%' THEN 200 ELSE 0 END rank_05, " +
			"CASE WHEN `urn` like '%$keyword%' THEN 100 ELSE 0 END rank_06, " +
			"CASE WHEN `schema` = '$keyword' THEN 30 ELSE 0 END rank_07, " +
			"CASE WHEN `schema` like '$keyword%' THEN 20 ELSE 0 END rank_08, " +
			"CASE WHEN `schema` like '%$keyword%' THEN 10 ELSE 0 END rank_09, " +
			"CASE WHEN `dataset_type` = '$keyword' THEN 30 ELSE 0 END rank_10, " +
			"CASE WHEN `dataset_type` LIKE '$keyword%' THEN 20 ELSE 0 END rank_11, " +
			"CASE WHEN `dataset_type` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_12, " +
			"CASE WHEN `fields` = '$keyword' THEN 30 ELSE 0 END rank_13, " +
			"CASE WHEN `fields` LIKE '$keyword%' THEN 20 ELSE 0 END rank_14, " +
			"CASE WHEN `fields` LIKE '%$keyword%' THEN 10 ELSE 0 END rank_15 " +
			"FROM dict_dataset WHERE MATCH(`name`, `schema`,  `properties`, `urn`, `dataset_type`, `fields`)" +
			" AGAINST ('*$keyword* *v_$keyword* \"$keyword\"' IN BOOLEAN MODE) and source = ? and `storage_type` = '$type' ) t " +
			"ORDER BY rank desc, `name`, `urn` LIMIT ?, ?;";

	public final static String SEARCH_FLOW_WITH_PAGINATION = "SELECT SQL_CALC_FOUND_ROWS " +
			"a.app_code, f.app_id, f.flow_id, f.flow_name, f.flow_group, f.flow_path, f.flow_level, " +
			"rank_01 + rank_02 + rank_03 + rank_04 as rank " +
			"FROM (SELECT app_id, flow_id, flow_name, flow_group, flow_path, flow_level, " +
			"CASE WHEN flow_name = '$keyword' THEN 3000 ELSE 0 END rank_01, " +
			"CASE WHEN flow_name like '%$keyword' THEN 2000 ELSE 0 END rank_02, " +
			"CASE WHEN flow_name like '$keyword%' THEN 1000 ELSE 0 END rank_03, " +
			"CASE WHEN flow_name like '%$keyword%' THEN 100 ELSE 0 END rank_04 " +
			"FROM flow WHERE flow_name like '%$keyword%' ) f " +
			"JOIN cfg_application a on f.app_id = a.app_id ORDER BY " +
			"rank DESC, flow_name, app_id, flow_id, flow_group, flow_path LIMIT ?, ?";

	public final static String SEARCH_JOB_WITH_PAGINATION = "SELECT SQL_CALC_FOUND_ROWS " +
			"a.app_code, f.flow_name, f.flow_group, f.flow_path, f.flow_level, " +
			"j.app_id, j.flow_id, j.job_id, j.job_name, j.job_path, j.job_type, " +
			"rank_01+rank_02+rank_03+rank_04 as rank " +
			"FROM (SELECT app_id, flow_id, job_id, job_name, job_path, job_type, " +
			"CASE WHEN job_name = '$keyword' THEN 3000 ELSE 0 END rank_01, " +
			"CASE WHEN job_name like '%$keyword' THEN 2000 ELSE 0 END rank_02, " +
			"CASE WHEN job_name like '$keyword%' THEN 1000 ELSE 0 END rank_03, " +
			"CASE WHEN job_name like '%$keyword%' THEN 100 ELSE 0 END rank_04 " +
			"FROM flow_job WHERE job_name like '%$keyword%' ) j " +
			"JOIN cfg_application a on a.app_id = j.app_id " +
			"JOIN flow f on f.app_id = j.app_id AND f.flow_id = j.flow_id " +
			"ORDER BY rank DESC, j.job_name, j.app_id, j.flow_id, j.job_id, j.job_path LIMIT ?, ?";

	public final static String SEARCH_METRIC_WITH_PAGINATION = "SELECT SQL_CALC_FOUND_ROWS " +
			"metric_id, `metric_name`, `metric_description`, `dashboard_name`, `metric_ref_id_type`, " +
			"`metric_ref_id`, `metric_category`, `metric_group`, " +
			"rank_01 + rank_02 + rank_03 + rank_04 + rank_05 + rank_06 + rank_07 + rank_08 + rank_09 + rank_10 + " +
			"rank_11 + rank_12 + rank_13 + rank_14 + rank_15 + rank_16 + rank_17 + rank_18 + rank_19 + rank_20 as rank " +
			"FROM (SELECT metric_id, `metric_name`, `metric_description`, `dashboard_name`, " +
			"`metric_ref_id_type`, `metric_ref_id`, `metric_category`, `metric_group`, " +
			"CASE WHEN match(`metric_name`) against ('$keyword' IN BOOLEAN MODE) THEN 90000 ELSE 0 END rank_01, " +
			"CASE WHEN match(`metric_name`) against ('$keyword' IN BOOLEAN MODE) THEN 30000 ELSE 0 END rank_02, " +
			"CASE WHEN match(`metric_name`) against ('$keyword*' IN BOOLEAN MODE) THEN 20000 ELSE 0 END rank_03, " +
			"CASE WHEN match(`metric_name`) against ('*$keyword*' IN BOOLEAN MODE) THEN 10000 ELSE 0 END rank_04, " +
			"CASE WHEN match(`metric_description`) against ('$keyword' IN BOOLEAN MODE) THEN 9000 ELSE 0 END rank_05, " +
			"CASE WHEN match(`metric_description`) against ('$keyword' IN BOOLEAN MODE) THEN 3000 ELSE 0 END rank_06, " +
			"CASE WHEN match(`metric_description`) against ('$keyword*' IN BOOLEAN MODE) THEN 2000 ELSE 0 END rank_07, " +
			"CASE WHEN match(`metric_description`) against ('*$keyword*' IN BOOLEAN MODE) THEN 1000 ELSE 0 END rank_08, " +
			"CASE WHEN match(`metric_category`) against ('$keyword' IN BOOLEAN MODE) THEN 900 ELSE 0 END rank_09, " +
			"CASE WHEN match(`metric_category`) against ('$keyword' IN BOOLEAN MODE) THEN 300 ELSE 0 END rank_10, " +
			"CASE WHEN match(`metric_category`) against ('$keyword*' IN BOOLEAN MODE) THEN 200 ELSE 0 END rank_11, " +
			"CASE WHEN match(`metric_category`) against ('*$keyword*' IN BOOLEAN MODE) THEN 100 ELSE 0 END rank_12, " +
			"CASE WHEN match(`metric_group`) against ('$keyword' IN BOOLEAN MODE) THEN 90 ELSE 0 END rank_13, " +
			"CASE WHEN match(`metric_group`) against ('$keyword' IN BOOLEAN MODE) THEN 30 ELSE 0 END rank_14, " +
			"CASE WHEN match(`metric_group`) against ('$keyword*' IN BOOLEAN MODE) THEN 20 ELSE 0 END rank_15, " +
			"CASE WHEN match(`metric_group`) against ('*$keyword*' IN BOOLEAN MODE) THEN 10 ELSE 0 END rank_16, " +
			"CASE WHEN match(`dashboard_name`) against ('$keyword' IN BOOLEAN MODE) THEN 9 ELSE 0 END rank_17, " +
			"CASE WHEN match(`dashboard_name`) against ('$keyword' IN BOOLEAN MODE) THEN 3 ELSE 0 END rank_18, " +
			"CASE WHEN match(`dashboard_name`) against ('$keyword*' IN BOOLEAN MODE) THEN 2 ELSE 0 END rank_19, " +
			"CASE WHEN match(`dashboard_name`) against ('*$keyword*' IN BOOLEAN MODE) THEN 1 ELSE 0 END rank_20 " +
			"FROM dict_business_metric WHERE " +
			"MATCH(`metric_name`, `dashboard_name`,  `metric_group`, `metric_category`) " +
			"AGAINST ('*$keyword*' IN BOOLEAN MODE) ) m ORDER BY " +
			"rank DESC, `metric_name`, `metric_category`, `metric_group`, `dashboard_name` LIMIT ?, ?;";

	public final static String SEARCH_DATASET_BY_COMMENTS_WITH_PAGINATION = "SELECT SQL_CALC_FOUND_ROWS " +
			"id, name, source, urn, `schema` FROM dict_dataset WHERE id in " +
			"(SELECT dataset_id FROM comments WHERE MATCH(text) against ('*$keyword*' in BOOLEAN MODE) ) " +
			"UNION ALL SELECT id, name, source, urn, `schema` FROM dict_dataset WHERE id in " +
			"(SELECT fd.dataset_id FROM ( " +
			"SELECT id FROM field_comments fc WHERE " +
			"MATCH(comment) AGAINST ('*$keyword*' IN BOOLEAN MODE) ) c JOIN dict_field_detail fd " +
			"ON ( find_in_set(c.id, fd.comment_ids) or c.id = fd.default_comment_id )) " +
			"ORDER BY 2 LIMIT ?, ?;";

	public final static String SEARCH_AUTOCOMPLETE_LIST = "searchSource";
	public final static String SEARCH_AUTOCOMPLETE_LIST_DATASET = "searchSourceDataset";
	public final static String SEARCH_AUTOCOMPLETE_LIST_METRIC = "searchSourceMetric";
	public final static String SEARCH_AUTOCOMPLETE_LIST_FLOW = "searchSourceFlow";

	public final static String GET_DATASET_AUTO_COMPLETE_LIST = "SELECT DISTINCT name FROM dict_dataset";
	public final static String GET_METRIC_AUTO_COMPLETE_LIST = "SELECT DISTINCT metric_name FROM dict_business_metric";

	public final static String GET_FLOW_AUTO_COMPLETE_LIST = "SELECT DISTINCT flow_name FROM flow";
	public final static String GET_JOB_AUTO_COMPLETE_LIST = "SELECT DISTINCT job_name FROM flow_job";

	public static List<String> getAutoCompleteList()
	{
		//Logger.debug("=== Entering SearchDAO.java:getAutoCompleteList()");
		List<String> cachedAutoCompleteList = (List<String>)Cache.get(SEARCH_AUTOCOMPLETE_LIST);
			//Logger.debug("=== Run first query in SearchDAO.java:getAutoCompleteList()");
		if (cachedAutoCompleteList == null || cachedAutoCompleteList.size() == 0)
		{
			//List<String> metricList = getJdbcTemplate().queryForList(GET_METRIC_AUTO_COMPLETE_LIST, String.class);
			//List<String> flowList = getJdbcTemplate().queryForList(GET_FLOW_AUTO_COMPLETE_LIST, String.class);
			//Logger.debug("=== Run second query in SearchDAO.java:getAutoCompleteList()");
			//List<String> jobList = getJdbcTemplate().queryForList(GET_JOB_AUTO_COMPLETE_LIST, String.class);
			//Logger.debug("=== Run third query in SearchDAO.java:getAutoCompleteList()");
			List<String> datasetList = getJdbcTemplate().queryForList(GET_DATASET_AUTO_COMPLETE_LIST, String.class);
			//Logger.debug("=== Run fourth query in SearchDAO.java:getAutoCompleteList()");
			//Logger.debug("=== Run all queries in SearchDAO.java:getAutoCompleteList()");
			cachedAutoCompleteList = datasetList;
			Collections.sort(cachedAutoCompleteList);
			Cache.set(SEARCH_AUTOCOMPLETE_LIST, cachedAutoCompleteList, 60*60);
		}
		Logger.trace("=== Exiting SearchDAO.java:getAutoCompleteList()");

		return cachedAutoCompleteList;
	}

	public static List<String> getAutoCompleteListForDataset()
	{
		//Logger.debug("Entering SearchDAO.java:getAutoCompleteListForDataset()");
		List<String> cachedAutoCompleteListForDataset = (List<String>)Cache.get(SEARCH_AUTOCOMPLETE_LIST_DATASET);
		if (cachedAutoCompleteListForDataset == null || cachedAutoCompleteListForDataset.size() == 0)
		{
			List<String> datasetList = getJdbcTemplate().queryForList(GET_DATASET_AUTO_COMPLETE_LIST, String.class);
			cachedAutoCompleteListForDataset = datasetList.stream().collect(Collectors.toList());
			Collections.sort(cachedAutoCompleteListForDataset);
			Cache.set(SEARCH_AUTOCOMPLETE_LIST_DATASET, cachedAutoCompleteListForDataset, 60*60);
		}

		return cachedAutoCompleteListForDataset;
	}

	public static List<String> getSuggestionList(String category, String searchKeyword)
	{
		//Logger.debug("Entering SearchDAO.java:getSuggestionList()");
		List<String> SuggestionList = new ArrayList<String>();
		String elasticSearchType = "dataset";
		String elasticSearchTypeURLKey = "elasticsearch.dataset.url";
		String fieldName = "name";

		JsonNode responseNode = null;
		ObjectNode keywordNode = null;

		try
		{
			String lCategory = category.toLowerCase();
			Logger.info("lCategory is " + category);

			switch (lCategory) {
				case "dataset":
					elasticSearchType = "dataset";
					elasticSearchTypeURLKey = "elasticsearch.dataset.url";
					fieldName = "name";
					break;
				case "metric":
					elasticSearchType = "metric";
					elasticSearchTypeURLKey = "elasticsearch.metric.url";
					fieldName = "metric_name";
					break;
				case "flow":
					elasticSearchType = "flow";
					elasticSearchTypeURLKey = "elasticsearch.flow.url";
					fieldName = "flow_name";
					break;
				default:
					break;
			}

			keywordNode = utils.Search.generateElasticSearchPhraseSuggesterQuery(elasticSearchType, fieldName, searchKeyword);
		}
		catch(Exception e)
		{
			Logger.error("Elastic search phrase suggester error. Error message :" + e.getMessage());
		}

		Logger.info("The suggest query sent to Elastic Search is: " + keywordNode.toString());

		Promise<WSResponse> responsePromise = WS.url(Play.application().configuration().getString(
				elasticSearchTypeURLKey)).post(keywordNode);
		responseNode = responsePromise.get(1000).asJson();

		// Logger.info("responseNode for getSuggestionList is " + responseNode.toString());

		if (responseNode != null && responseNode.isContainerNode() && responseNode.has("hits"))
		{
			JsonNode suggestNode = responseNode.get("suggest");
			Logger.info("suggestNode is " + suggestNode.toString());
			if (suggestNode != null && suggestNode.has("simple_phrase"))
			{
				JsonNode simplePhraseNode = suggestNode.get("simple_phrase");

				if (simplePhraseNode != null && simplePhraseNode.isArray())
				{
					Iterator<JsonNode> arrayIterator = simplePhraseNode.elements();
					if (arrayIterator != null)
					{
						while (arrayIterator.hasNext())
						{
							JsonNode node = arrayIterator.next();
							if (node.isContainerNode() && node.has("options"))
							{
								JsonNode optionsNode = node.get("options");
								if (optionsNode != null && optionsNode.isArray())
								{
									Iterator<JsonNode> arrayIteratorOptions = optionsNode.elements();
									if (arrayIteratorOptions != null)
									{
										while (arrayIteratorOptions.hasNext())
										{
											JsonNode textNode = arrayIteratorOptions.next();
											if (textNode != null && textNode.has("text"))
											{
												String oneSuggestion = textNode.get("text").asText();
												Logger.info("oneSuggestion is " + oneSuggestion);
												SuggestionList.add(oneSuggestion);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return SuggestionList;
	}

	public static ObjectNode getPagedEntityByKeyword(String category, String keyword, String source, String storage_type, int page, int size)
	{
		//Logger.debug("Entering SearchDAO.java:getPagedEntityByKeyword()");
		List<Dataset> pagedDatasets = new ArrayList<Dataset>();
		final JdbcTemplate jdbcTemplate = getJdbcTemplate();
		javax.sql.DataSource ds = jdbcTemplate.getDataSource();
		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);

		TransactionTemplate txTemplate = new TransactionTemplate(tm);

		ObjectNode result;
		result = txTemplate.execute(new TransactionCallback<ObjectNode>()
		{
			public ObjectNode doInTransaction(TransactionStatus status)
			{
				List<Map<String, Object>> rows = null;
				if (StringUtils.isBlank(source) || source.toLowerCase().equalsIgnoreCase("all"))
				{
					if (storage_type == null || storage_type.equals("all")) {
						String query = SEARCH_DATASET_WITH_PAGINATION.replace("$keyword", keyword);
						rows = jdbcTemplate.queryForList(query, (page - 1) * size, size);
					} else {
						String query = SEARCH_DATASET_WITH_PAGINATION_AND_TYPE.replace("$keyword", keyword);
						query = query.replace("$type", storage_type);
						rows = jdbcTemplate.queryForList(query, (page - 1) * size, size);
					}
				}
				else
				{
					if (storage_type == null || storage_type.equals("all")) {
						String query = SEARCH_DATASET_BY_SOURCE_WITH_PAGINATION.replace("$keyword", keyword);
						rows = jdbcTemplate.queryForList(query, (page - 1) * size, size);
					} else {
						String query = SEARCH_DATASET_BY_SOURCE_WITH_PAGINATION_AND_TYPE.replace("$keyword", keyword);
						query = query.replace("$type", storage_type);
						rows = jdbcTemplate.queryForList(query, source, (page - 1) * size, size);
					}
				}

				for (Map row : rows) {

					Dataset ds = new Dataset();
					ds.id = (Long)row.get(DatasetRowMapper.DATASET_ID_COLUMN);
					ds.name = (String)row.get(DatasetRowMapper.DATASET_NAME_COLUMN);
					ds.source = (String)row.get(DatasetRowMapper.DATASET_SOURCE_COLUMN);
					ds.urn = (String)row.get(DatasetRowMapper.DATASET_URN_COLUMN);
					ds.schema = (String)row.get(DatasetRowMapper.DATASET_SCHEMA_COLUMN);
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
				resultNode.put("category", category);
				resultNode.put("source", source);
				resultNode.put("itemsPerPage", size);
				resultNode.put("totalPages", (int)Math.ceil(count/((double)size)));
				resultNode.set("data", Json.toJson(pagedDatasets));

				return resultNode;
			}
		});

		return result;
	}


	public static ObjectNode getPagedDatasetByKeyword(String category, String keyword, String source, int page, int size)
	{
		//Logger.debug("Entering SearchDAO.java:getPagedDatasetByKeyword()");
		return getPagedEntityByKeyword(category, keyword, source, "data", page, size);
		
	}

	// flow temporarily represents db
	public static ObjectNode getPagedDbByKeyword(String category, String keyword, int page, int size)
	{
		//Logger.debug("Entering SearchDAO.java:getPagedFlowByKeyword()");
		return getPagedEntityByKeyword(category, keyword, "all", "db", page, size);
		
	}

	public static ObjectNode getPagedJobByKeyword(String category, String keyword, int page, int size)
	{
		//Logger.debug("Entering SearchDAO.java:getPagedJobByKeyword()");
		return getPagedEntityByKeyword(category, keyword, "all", "app", page, size);
		
	}

	public static ObjectNode getPagedAllByKeyword(String category, String keyword, int page, int size) {
		return getPagedEntityByKeyword(category, keyword, "all", "all", page, size);
	}

	public static ObjectNode getPagedCommentsByKeyword(String category, String keyword, int page, int size)
	{
		//Logger.debug("Entering SearchDAO.java:getPagedCommentsByKeyword()");
		List<Dataset> pagedDatasets = new ArrayList<Dataset>();
		final JdbcTemplate jdbcTemplate = getJdbcTemplate();
		javax.sql.DataSource ds = jdbcTemplate.getDataSource();
		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);

		TransactionTemplate txTemplate = new TransactionTemplate(tm);

		ObjectNode result;
		result = txTemplate.execute(new TransactionCallback<ObjectNode>()
		{
			public ObjectNode doInTransaction(TransactionStatus status)
			{
				List<Map<String, Object>> rows = null;
				String query = SEARCH_DATASET_BY_COMMENTS_WITH_PAGINATION.replace("$keyword", keyword);
				rows = jdbcTemplate.queryForList(query, (page-1)*size, size);

				for (Map row : rows) {

					Dataset ds = new Dataset();
					ds.id = (long)row.get(DatasetRowMapper.DATASET_ID_COLUMN);
					ds.name = (String)row.get(DatasetRowMapper.DATASET_NAME_COLUMN);
					ds.source = (String)row.get(DatasetRowMapper.DATASET_SOURCE_COLUMN);
					ds.urn = (String)row.get(DatasetRowMapper.DATASET_URN_COLUMN);
					ds.schema = (String)row.get(DatasetRowMapper.DATASET_SCHEMA_COLUMN);
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
				resultNode.put("category", category);
				resultNode.put("itemsPerPage", size);
				resultNode.put("totalPages", (int)Math.ceil(count/((double)size)));
				resultNode.set("data", Json.toJson(pagedDatasets));

				return resultNode;
			}
		});

		return result;
	}

}
