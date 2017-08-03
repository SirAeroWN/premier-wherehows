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

-- create statement for dataset related tables :
-- dict_dataset, dict_dataset_sample, dict_field_detail, dict_dataset_schema_history


-- dataset table
CREATE TABLE `dict_dataset` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8 NOT NULL,
  `schema` mediumtext CHARACTER SET utf8,
  `schema_type` varchar(50) CHARACTER SET utf8 DEFAULT 'JSON' COMMENT 'JSON, Hive, DDL, XML, CSV',
  `properties` text CHARACTER SET utf8,
  `fields` mediumtext CHARACTER SET utf8,
  `urn` varchar(200) CHARACTER SET utf8 NOT NULL,
  `source` varchar(50) CHARACTER SET utf8 DEFAULT NULL COMMENT 'The original data source type (for dataset in data warehouse). Oracle, Kafka ...',
  `location_prefix` varchar(200) CHARACTER SET utf8 DEFAULT NULL,
  `parent_name` varchar(200) DEFAULT NULL COMMENT 'Schema Name for RDBMS, Group Name for Jobs/Projects/Tracking Datasets on HDFS ',
  `storage_type` enum('data','app','db') CHARACTER SET utf8 DEFAULT NULL,
  `ref_dataset_id` int(11) unsigned DEFAULT NULL COMMENT 'Refer to Master/Main dataset for Views/ExternalTables',
  `status_id` smallint(6) unsigned DEFAULT NULL COMMENT 'Reserve for dataset status',
  `dataset_type` varchar(30) CHARACTER SET utf8 DEFAULT NULL COMMENT 'hdfs, hive, kafka, teradata, mysql, sqlserver, file, nfs, pinot, salesforce, oracle, db2, netezza, cassandra, hbase, qfs, zfs',
  `hive_serdes_class` varchar(300) DEFAULT NULL,
  `is_partitioned` char(1) DEFAULT NULL,
  `partition_layout_pattern_id` smallint(6) DEFAULT NULL,
  `sample_partition_full_path` varchar(256) DEFAULT NULL COMMENT 'sample partition full path of the dataset',
  `source_created_time` int(10) unsigned DEFAULT NULL COMMENT 'source created time of the flow',
  `source_modified_time` int(10) unsigned DEFAULT NULL COMMENT 'latest source modified time of the flow',
  `created_time` int(10) unsigned DEFAULT NULL COMMENT 'wherehows created time',
  `modified_time` int(10) unsigned DEFAULT NULL COMMENT 'latest wherehows modified',
  `wh_etl_exec_id` bigint(20) DEFAULT NULL COMMENT 'wherehows etl execution id that modified this record',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_dataset_urn` (`urn`),
  FULLTEXT KEY `fti_datasets_all` (`name`,`schema`,`properties`,`urn`, `dataset_type`, `fields`)
)
  ENGINE=InnoDB
  AUTO_INCREMENT=16
  DEFAULT CHARSET=utf8;

-- fields to comments mapping
CREATE TABLE `dict_dataset_field_comment` (
  `field_id`   INT(11) UNSIGNED NOT NULL,
  `comment_id` BIGINT(20) NOT NULL,
  `dataset_id` INT(11) UNSIGNED NOT NULL,
  `is_default` TINYINT(1) NULL DEFAULT '0',
  PRIMARY KEY (field_id, comment_id),
  KEY (comment_id)
)
  ENGINE = InnoDB;

-- dataset comments
CREATE TABLE comments (
  `id`           INT(11) AUTO_INCREMENT                                                                       NOT NULL,
  `text`         TEXT CHARACTER SET utf8                                                                      NOT NULL,
  `user_id`      INT(11)                                                                                      NOT NULL,
  `dataset_id`   INT(11)                                                                                      NOT NULL,
  `created`      DATETIME                                                                                     NULL,
  `modified`     DATETIME                                                                                     NULL,
  `comment_type` ENUM('Description', 'Grain', 'Partition', 'ETL Schedule', 'DQ Issue', 'Question', 'Comment') NULL,
  PRIMARY KEY (id),
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `dataset_id` (`dataset_id`) USING BTREE,
  FULLTEXT KEY `fti_comment` (`text`)
)
  ENGINE = InnoDB
  CHARACTER SET utf8
  COLLATE utf8_general_ci
  AUTO_INCREMENT = 0;

-- field comments
CREATE TABLE `field_comments` (
  `id`                     INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`                INT(11)          NOT NULL DEFAULT '0',
  `comment`                VARCHAR(4000)    NOT NULL,
  `created`                TIMESTAMP        NOT NULL,
  `modified`               TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `comment_crc32_checksum` INT(11) UNSIGNED          NULL COMMENT '4-byte CRC32',
  PRIMARY KEY (`id`),
  KEY `comment_key` (`comment`(100)),
  FULLTEXT KEY `fti_comment` (`comment`)
)
  ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;
