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


CREATE TABLE `dataset_security` (
  `dataset_id`                INT(10) UNSIGNED NOT NULL,
  `dataset_urn`               VARCHAR(200)     NOT NULL,
  `classification`            VARCHAR(500)     DEFAULT NULL
  COMMENT 'JSON: confidential fields',
  `record_owner_type`         VARCHAR(50)      DEFAULT NULL
  COMMENT 'MEMBER,CUSTOMER,INTERNAL,COMPANY,GROUP',
  `retention_policy`          VARCHAR(200)     DEFAULT NULL
  COMMENT 'JSON: specification of retention',
  `geographic_affinity`       VARCHAR(200)     DEFAULT NULL
  COMMENT 'JSON: must be stored in the geo region',
  `modified_time`             INT UNSIGNED DEFAULT NULL
  COMMENT 'the modified time in epoch',
  PRIMARY KEY (`dataset_id`),
  UNIQUE KEY `dataset_urn` (`dataset_urn`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;