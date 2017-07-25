-- noinspection SqlDialectInspectionForFile

-- noinspection SqlNoDataSourceInspectionForFile

--
-- Copyright 2015 LinkedIn Corp. All rights reserved.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--

-- inserts a bunch of default values, most of which are exclusive to premier

INSERT INTO `wh_property` (`property_name`, `property_value`, `is_encrypted`, `group_name`)
VALUES
	('node.color.datamgt', 'wheat', 'N', NULL),
	('node.color.domain-lucene', 'seagreen', 'N', NULL),
	('node.color.domain-parquet', 'tan', 'N', NULL),
	('node.color.druid', 'lightblue', 'N', NULL),
	('node.color.lucene', 'seagreen', 'N', NULL),
	('node.color.lucene-search', 'seagreen', 'N', NULL),
	('node.color.match-parquet', 'tan', 'N', NULL),
	('node.color.moveit', 'peachpuff', 'N', NULL),
	('node.color.moveit-extract', 'peachpuff', 'N', NULL),
	('node.color.moveit-index-pairing', 'peachpuff', 'N', NULL),
	('node.color.moveit-index-search', 'peachpuff', 'N', NULL),
	('node.color.moveit-transform', 'peachpuff', 'N', NULL),
	('node.color.moveit-transform-patient', 'peachpuff', 'N', NULL),
	('node.color.natezza', 'mistyrose', 'N', NULL),
	('node.color.opportunity-parquet', 'tan', 'N', NULL),
	('node.color.parquet', 'tan', 'N', NULL),
	('node.color.pim', 'orange', 'N', NULL),
	('node.color.pivoted-lucene-search', 'seagreen', 'N', NULL),
	('node.color.prospector', 'lightcoral', 'N', NULL),
	('node.color.pulse', 'sandybrown', 'N', NULL),
	('node.color.purchase-druid', 'lightblue', 'N', NULL),
	('node.color.qa', 'navajowhite', 'N', NULL),
	('node.color.raw-parquet', 'tan', 'N', NULL),
	('node.color.sa', 'palegreen', 'N', NULL),
	('node.color.spend', 'palegreen', 'N', NULL),
	('node.color.supply-druid', 'lightblue', 'N', NULL),
	('node.color.surgical-supply-file', 'teal', 'N', NULL),
	('node.color.work-parquet', 'tan', 'N', NULL),
	('node.color.pig', 'lightblue', 'N', NULL),
	('node.color.hdfs', 'thistle', 'N', NULL),
	('node.color.nas', 'tan', 'N', NULL),
	('node.color.teradata', 'mistyrose', 'N', NULL),
	('node.color.shell', 'sandybrown', 'N', NULL),
	('node.color.mload', 'peachpuff', 'N', NULL),
	('node.color.sql', 'navajowhite', 'N', NULL),
	('node.color.lassen', 'palegreen', 'N', NULL),
	('node.color.cmd', 'orange', 'N', NULL),
	('node.color.tpt', 'wheat', 'N', NULL),
	('node.color.informatica', 'seagreen', 'N', NULL),
	('node.color.java', 'lightcoral', 'N', NULL),
	('edge.label.from.prospector', 'created by analysis', 'N', NULL),
	('prop.data', 'source,dataset_node.type,prop/description', 'N', NULL),
	('prop.sortlist.data', 'abstracted_path,dataset_node.type', 'N', NULL),
	('node.type.datamgt', 'db', 'N', NULL),
	('node.type.domain-lucene', 'data', 'N', NULL),
	('node.type.domain-parquet', 'data', 'N', NULL),
	('node.type.druid', 'data', 'N', NULL),
	('node.type.lucene', 'data', 'N', NULL),
	('node.type.lucene-search', 'data', 'N', NULL),
	('node.type.match-parquet', 'data', 'N', NULL),
	('node.type.moveit', 'app', 'N', NULL),
	('node.type.moveit-extract', 'app', 'N', NULL),
	('node.type.moveit-index-pairing', 'app', 'N', NULL),
	('node.type.moveit-index-search', 'app', 'N', NULL),
	('node.type.moveit-transform', 'app', 'N', NULL),
	('node.type.moveit-transform-patient', 'app', 'N', NULL),
	('node.type.natezza', 'db', 'N', NULL),
	('node.type.opportunity-parquet', 'data', 'N', NULL),
	('node.type.parquet', 'data', 'N', NULL),
	('node.type.pim', 'db', 'N', NULL),
	('node.type.pivoted-lucene-search', 'data', 'N', NULL),
	('node.type.prospector', 'app', 'N', NULL),
	('node.type.pulse', 'db', 'N', NULL),
	('node.type.purchase-druid', 'data', 'N', NULL),
	('node.type.qa', 'db', 'N', NULL),
	('node.type.raw-parquet', 'data', 'N', NULL),
	('node.type.sa', 'db', 'N', NULL),
	('node.type.sla-supply', 'app', 'N', NULL),
	('node.type.spend', 'db', 'N', NULL),
	('node.type.supply-druid', 'data', 'N', NULL),
	('node.type.surgical-supply-file', 'data', 'N', NULL),
	('node.type.work-parquet', 'data', 'N', NULL),
	('node.type.druid-indexer', 'app', 'N', NULL);

