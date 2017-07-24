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

import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contr class used for common controller actions
 * Created by norv on 07/24/2017.
 */
public class ContrUtil {


    // generalized function for call failure
    public static void failure(ObjectNode resultJson, String error_message) {
        // ObjectNode is mutable so we don't have to return, maybe?
        response(resultJson, 400, "error_message", error_message);
    }

    // generalized function for call failure, allows for passed return_code
    public static void failure(ObjectNode resultJson, int return_code, String error_message) {
        // ObjectNode is mutable so we don't have to return, maybe?
        response(resultJson, return_code, "error_message", error_message);
    }

    private static void response(ObjectNode resultJson, int return_code, String message_name, String message) {
        resultJson.put("return_code", return_code);
        resultJson.put(message_name, message);
    }
}