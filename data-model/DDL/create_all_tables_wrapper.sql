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

/* wrapper sql to call all individual DDL */
-- use wherehows;

-- family_setup.sql sets up the family table

-- default_properties.sql inserts some default properties into `wh_property` table

source ETL_DDL/dataset_metadata.sql;
source ETL_DDL/etl_configure_tables.sql;
source ETL_DDL/owner_metadata.sql;
source ETL_DDL/dataset_info_metadata.sql;

source ETL_DDL/family_setup.sql;

source WEB_DDL/track.sql;
source WEB_DDL/users.sql;

source ETL_DDL/default_properties.sql;

show tables;
