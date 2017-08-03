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

-- configuration tables
CREATE TABLE `wh_property` (
  `property_name`  VARCHAR(127) NOT NULL
  COMMENT 'property name',
  `property_value` TEXT COMMENT 'property value',
  `is_encrypted`   CHAR(1)      DEFAULT 'N'
  COMMENT 'whether the value is encrypted',
  `group_name`     VARCHAR(127) DEFAULT NULL
  COMMENT 'group name for the property',
  PRIMARY KEY (`property_name`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COMMENT = 'WhereHows properties table';