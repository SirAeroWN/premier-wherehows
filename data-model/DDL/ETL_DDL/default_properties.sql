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

INSERT INTO `wh_property` (`property_name`, `property_value`, `is_encrypted`, `group_name`)
VALUES
	('color.datamgt', 'wheat', 'N', NULL),
	('color.parquet', 'tan', 'N', NULL),
	('color.raw-parquet', 'tan', 'N', NULL),
	('color.match-parquet', 'tan', 'N', NULL),
	('color.domain-parquet', 'tan', 'N', NULL),
	('color.opportunity-parquet', 'tan', 'N', NULL),
	('color.lucene', 'seagreen', 'N', NULL),
	('color.domain-lucene', 'seagreen', 'N', NULL),
	('color.pulse', 'sandybrown', 'N', NULL),
	('color.moveit', 'peachpuff', 'N', NULL),
	('color.moveit-extract', 'peachpuff', 'N', NULL),
	('color.moveit-transform', 'peachpuff', 'N', NULL),
	('color.sa', 'palegreen', 'N', NULL),
	('color.spend', 'palegreen', 'N', NULL),
	('color.pim', 'orange', 'N', NULL),
	('color.qa', 'navajowhite', 'N', NULL),
	('color.natezza', 'mistyrose', 'N', NULL),
	('color.prospector', 'lightcoral', 'N', NULL),
	('color.druid', 'lightblue', 'N', NULL),
	('color.supply-druid', 'lightblue', 'N', NULL),
	('color.purchase-druid', 'lightblue', 'N', NULL),
	('color.moveit-transform-patient', 'peachpuff', 'N', NULL),
	('color.work-parquet', 'tan', 'N', NULL)
	('color.surgical-supply-file', 'teal', 'N', NULL);
	('type.qa', 'db', 'N', NULL),
	('type.sa', 'db', 'N', NULL),
	('type.pim', 'db', 'N', NULL),
	('type.pulse', 'db', 'N', NULL),
	('type.spend', 'db', 'N', NULL),
	('type.datamgt', 'db', 'N', NULL),
	('type.natezza', 'db', 'N', NULL),
	('type.druid', 'data', 'N', NULL),
	('type.lucene', 'data', 'N', NULL),
	('type.parquet', 'data', 'N', NULL),
	('type.raw-parquet', 'data', 'N', NULL),
	('type.supply-druid', 'data', 'N', NULL),
	('type.domain-lucene', 'data', 'N', NULL),
	('type.match-parquet', 'data', 'N', NULL),
	('type.domain-parquet', 'data', 'N', NULL),
	('type.purchase-druid', 'data', 'N', NULL),
	('type.opportunity-parquet', 'data', 'N', NULL),
	('type.moveit', 'app', 'N', NULL),
	('type.prospector', 'app', 'N', NULL),
	('type.moveit-extract', 'app', 'N', NULL),
	('type.moveit-transform', 'app', 'N', NULL)
	('type.moveit-transform-patient', 'app', 'N', NULL),
	('type.work-parquet', 'data', 'N', NULL)
	('type.surgical-supply-file', 'data', 'N', NULL);
