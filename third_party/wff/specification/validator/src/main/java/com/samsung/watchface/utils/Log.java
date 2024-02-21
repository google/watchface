/*
 * Copyright 2023 Samsung Electronics Co., Ltd All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsung.watchface.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {
    private static class LogFormatter extends Formatter {
        @Override
        public java.lang.String format(LogRecord logRecord) {
            return logRecord.getLevel() + ": " + formatMessage(logRecord) + System.lineSeparator();
        }
    }

    private final static Logger LOG = Logger.getLogger("");
    static {
        for (Handler hnd : LOG.getHandlers()) {
            LOG.removeHandler(hnd);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        LOG.addHandler(handler);
    }

    public static void e(String msg) {
        logSafely(Level.SEVERE, msg);
    }

    public static void i(String msg) {
        logSafely(Level.INFO, msg);
    }

    private static void logSafely(Level level, String msg) {
        if (LOG.isLoggable(level)) {
            LOG.log(level, msg);
        }
    }
}
