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
package models.daos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.dao.DataAccessException;
import play.Logger;
import play.Play;
import utils.Urn;
import utils.JdbcUtil;
import wherehows.common.DatasetPath;
import wherehows.common.schemas.ApplicationRecord;
import wherehows.common.schemas.LineageDatasetRecord;
import wherehows.common.schemas.LineageRecordLite;
import wherehows.common.utils.PreparedStatementUtil;
import wherehows.common.writers.DatabaseWriter;
import wherehows.common.exceptions.IncompleteJsonException;


/**
 * Essentially only impliments insertLineage(), everything else returns empty objects and sticks an error in the log
 * created by norv on 07/06/16
 */
public class LineageDaoLite {

    // insert relationships between give nodes into family table
    // all parents in parents array are parents of every child in the children array
    public static void insertLineage(JsonNode lineage) throws Exception, IOException, SQLException, IncompleteJsonException {
        List<LineageRecordLite> records = new ArrayList<LineageRecordLite>();

        // check that both parent_urn and child_urn are present, if not, then error out
        if (lineage.has("parent_urn") && lineage.has("child_urn")) {
            JsonNode parents = lineage.findPath("parent_urn");
            JsonNode children = lineage.findPath("child_urn");

            if (parents.isArray() && children.isArray()) {
                for (JsonNode parent : parents) {
                    for (JsonNode child : children) {
                        LineageRecordLite record = new LineageRecordLite(parent.textValue(), child.textValue());
                        records.add(record);
                    }
                }
            }

            DatabaseWriter dw = new DatabaseWriter(JdbcUtil.wherehowsJdbcTemplate, "family");
            for (LineageRecordLite record : records) {
                dw.append(record);
            }
            dw.insert("parent_urn, child_urn"); // overload to use slightly better version of insert function
            dw.close();
        } else if (lineage.has("parent_urn") && !lineage.has("child_urn")) {
            throw new IncompleteJsonException("Missing `child_urn` field");
        } else if (!lineage.has("parent_urn") && lineage.has("child_urn")) {
            throw new IncompleteJsonException("Missing `parent_urn` field");
        } else if (!lineage.has("parent_urn") && !lineage.has("child_urn")) {
            throw new IncompleteJsonException("Missing `parent_urn` and `child_urn` fields");
        }
    }
}
