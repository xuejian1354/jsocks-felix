/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ymbl.smartgateway.jsocks.log;

import org.osgi.service.log.LogService;

public final class SystemLogger
{
    private final static LogService NOP = new NopLogger();
    private static LogService LOGGER;

    public static void setLogService(LogService service)
    {
        LOGGER = service;
    }

    private static LogService getLogger()
    {
        return LOGGER != null ? LOGGER : NOP;
    }

    public static void debug(String message)
    {
        getLogger().log(LogService.LOG_DEBUG, message);
    }

    public static void info(String message)
    {
        getLogger().log(LogService.LOG_INFO, message);
    }

    public static void warning(String message, Throwable cause)
    {
        getLogger().log(LogService.LOG_WARNING, message, cause);
    }

    public static void error(String message, Throwable cause)
    {
        getLogger().log(LogService.LOG_ERROR, message, cause);
    }
}
