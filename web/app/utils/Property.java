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
package utils;


import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import play.Logger;
import dao.DataSource;
import java.util.List;

public class Property {


    private static String MYSQL_DB_WHEREHOWS_OPENSOURCE = "wherehows_opensource_mysql";

    private final static String GET_PROPERTY = "SELECT property_value FROM wh_property WHERE property_name = ?";

    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(DataSource.getDataSource(MYSQL_DB_WHEREHOWS_OPENSOURCE));

    private static NamedParameterJdbcTemplate namedJdbcTemplate =
            new NamedParameterJdbcTemplate(DataSource.getDataSource(MYSQL_DB_WHEREHOWS_OPENSOURCE));

    protected static JdbcTemplate getJdbcTemplate()
    {
        return jdbcTemplate;
    }

    protected static NamedParameterJdbcTemplate getNamedParameterJdbcTemplate()
    {
        return namedJdbcTemplate;
    }


    public static String getPrefix(String urn) {
        return urn.substring(0, urn.indexOf("://")).toLowerCase();
    }

    public static String getPostfix(String urn) {
        return urn.substring(urn.indexOf("://") + 3);
    }

    public static String getProp(String propName) {
        List<String> props = getJdbcTemplate().queryForList(GET_PROPERTY, String.class, propName);
        if (props == null || props.size() == 0) {
            //Logger.info("Could not find property for property_name: " + propName);
            return "default";
        }
        return props.get(0);
    }
}