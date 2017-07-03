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
package wherehows.common.schemas;

import java.util.ArrayList;
import java.util.List;
import wherehows.common.utils.StringUtil;


/**
 * Created by zechen on 10/16/15.
 */
public class PropertyRecord extends AbstractRecord {

    String property_name;
    String property_value;
    String is_encrypted;

    public PropertyRecord(String name, String value, String enc) {
        property_name = name;
        property_value = value;
        is_encrypted = enc;
    }

    @Override
    public List<Object> fillAllFields() {
        List<Object> allFields = new ArrayList<>();
        allFields.add(property_name);
        allFields.add(property_value);
        allFields.add(is_encrypted);
        return allFields;
    }
}