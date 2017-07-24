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
package wherehows.common.writers;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import wherehows.common.schemas.AbstractRecord;
import wherehows.common.schemas.Record;
import wherehows.common.utils.PreparedStatementUtil;
//import play.Logger;


/**
 * Created by zsun on 8/20/15.
 */
public class DatabaseWriter extends Writer {
  JdbcTemplate jdbcTemplate;
  String tableName;
  private static final Logger logger = LoggerFactory.getLogger(DatabaseWriter.class);

  public DatabaseWriter(JdbcTemplate jdbcTemplate, String tableName) {
    this.jdbcTemplate = jdbcTemplate;
    this.tableName = tableName;
  }

  public DatabaseWriter(DataSource dataSource, String tableName) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.tableName = tableName;
  }

  public DatabaseWriter(String connectionUrl, String tableName)
      throws SQLException {
    DriverManagerDataSource dataSource = new DriverManagerDataSource(connectionUrl);
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.tableName = tableName;
  }

  public synchronized void update(String setValues, String urn) {

    StringBuilder sb = new StringBuilder();
    sb.append("UPDATE " + this.tableName + " SET " + setValues + " WHERE urn = '" + urn + "'");
    this.jdbcTemplate.execute(sb.toString());
  }

  // a generalized version of update
  public synchronized void generalUpdate(String setValues, String selCol, String selVal) {

    StringBuilder sb = new StringBuilder();
    sb.append("UPDATE " + this.tableName + " SET " + setValues + " WHERE " + selCol + " = '" + selVal + "'");
    //System.out.println(sb.toString());
    this.jdbcTemplate.execute(sb.toString());
  }

  // if parameter is a string, needs to be passed as 'val' including the quotes
  public synchronized void remove(Map<String, String> params) {
    if (params != null && params.size() > 0) {
      StringBuilder sb = new StringBuilder();
      sb.append("DELETE FROM " + this.tableName + " WHERE ");
      for (Map.Entry<String, String> param : params.entrySet()) {
        sb.append(" " + param.getKey() + " = " + param.getValue() + " AND");
      }
      System.out.println("DELETE statement is: " + sb.substring(0, sb.length() - 4));
      this.jdbcTemplate.execute(sb.substring(0, sb.length() - 4));
    }
  }

  //TODO: this insert sql is too ambitious, need add column names
  @Override
  public synchronized boolean flush()
      throws SQLException {
    if (records.size() == 0) {
      return false;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO " + this.tableName + " VALUES ");
    for (Record r : this.records) {
      sb.append("(" + r.toDatabaseValue() + "),");
    }
    sb.deleteCharAt(sb.length() - 1);

    logger.debug("In databaseWriter : " + sb.toString());

    this.jdbcTemplate.execute(sb.toString());
    this.records.clear();
    return false;
  }

  public synchronized boolean insert(String commaDelimitedNames) throws SQLException, DataAccessException {
    if (records.size() == 0) {
      return false;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO " + this.tableName + " (" + commaDelimitedNames + ") VALUES ");
    for (Record r : this.records) {
      sb.append("(" + r.toDatabaseValue() + "),");
    }
    sb.deleteCharAt(sb.length() - 1);

    String statement = sb.toString();
    logger.debug("In databaseWriter : " + statement);

    this.jdbcTemplate.execute(statement);
    this.records.clear();
    return true;
  }

  @Override
  public void close()
      throws SQLException {
    logger.debug("closing...");
    this.flush();
    logger.debug("flushed");
  }

  /**
   * use JDBC template PreparedStatement to insert records
   * require record to have getAllValues() and optional getDbColumnNames() method
   * @return boolean if the insert is successful
   * @throws SQLException
   */
  public boolean insert() throws SQLException, IllegalAccessException {
    if (records.size() == 0 || !(records.get(0) instanceof AbstractRecord)) {
      logger.debug("DatabaseWriter no record to insert or unknown record Class.");
      System.out.println("DatabaseWriter no record or unknown record Class: " + records.size() + " | " + records.get(0) + " | " + (records.get(0) instanceof AbstractRecord));
      return false;
    }

    final AbstractRecord record0 = (AbstractRecord) records.get(0);
    final String[] columnNames = record0.getDbColumnNames();
    final String sql =
        (columnNames != null) ? PreparedStatementUtil.prepareInsertTemplateWithColumn(tableName, columnNames)
            : PreparedStatementUtil.prepareInsertTemplateWithoutColumn(tableName, record0.getAllFields().length);
    logger.info("sql string: " + sql);
    logger.error("columnNames: " + columnNames);
    System.out.println("sql: " + sql + " | " + columnNames);
    //logger.debug("DatabaseWriter template for " + record0.getClass() + " : " + sql);

    for (final Record record : records) {
      jdbcTemplate.update(sql, ((AbstractRecord) record).getAllValuesToString());
    }
    records.clear();
    return true;
  }

  /**
   * use JDBC template PreparedStatement to update row in database by setting columns with conditions
   * @param columns String[] column names to update
   * @param columnValues Object[] new values
   * @param conditions String[] conditions
   * @param conditionValues Object[] condition values
   */
  public void update(String[] columns, Object[] columnValues, String[] conditions, Object[] conditionValues)
      throws DataAccessException {
    if (columns.length != columnValues.length || conditions.length != conditionValues.length) {
      logger.error("DatabaseWriter update columns and values mismatch");
      return;
    }
    Object[] values = Arrays.copyOf(columnValues, columnValues.length + conditionValues.length);
    System.arraycopy(conditionValues, 0, values, columnValues.length, conditionValues.length);

    final String sql = PreparedStatementUtil.prepareUpdateTemplateWithColumn(tableName, columns, conditions);
    //logger.debug("DatabaseWriter template for " + record0.getClass() + " : " + sql);

    execute(sql, values);
  }

  /**
   * use JDBC template PreparedStatement to execute an SQL command with values
   * @param sql String, command with placeholders
   * @param values Object[]
   */
  public synchronized void execute(String sql, Object[] values)
      throws DataAccessException {
    // logger.debug("SQL: " + sql + ", values: " + Arrays.toString(values));
    jdbcTemplate.update(sql, values);
  }
}
