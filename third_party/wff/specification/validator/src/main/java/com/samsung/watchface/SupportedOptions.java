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

package com.samsung.watchface;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum SupportedOptions {
    HELP("--help", "Print help with supported options"),
    STOP_ON_FAIL("--stop-on-fail", "Stop immediately if there is a failure while validation");

    public static final Set<String> optionStrings = unmodifiableOptionSet();

    public final String arg;
    public final String description;
    SupportedOptions(String optionString, String desc) {
        this.arg = optionString;
        this.description = desc;
    }

    private static Set<String> unmodifiableOptionSet() {
        Set<String> options = new HashSet<>();
        Arrays.stream(SupportedOptions.values()).forEach(supportedOptions -> options.add(supportedOptions.arg));
        return Collections.unmodifiableSet(options);
    }
}
