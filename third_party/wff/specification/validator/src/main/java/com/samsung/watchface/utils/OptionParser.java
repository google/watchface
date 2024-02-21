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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OptionParser {
    public final Set<String> supportedOptions;
    public Set<String> options;

    public OptionParser(Set<String> supportedOptions) {
        this.supportedOptions = Collections.unmodifiableSet(supportedOptions);
    }

    /**
     * Parse options from args and return a list of remainder of the args which is not supported.
     * Supported options are loaded after this call.
     *
     * @param args arguments to be parsed.
     */
    public List<String> parse(String[] args) {
        options = Arrays.stream(args).filter(this::isSupportedOption).collect(Collectors.toSet());
        return Arrays.stream(args).filter(arg -> !isSupportedOption(arg))
                .collect(Collectors.toList());
    }

    /**
     * Return whether the option is exist or not.
     *
     * @param option target option
     * @return true if the option is exist, or return false.
     */
    public boolean hasOption(String option) {
        return options.contains(option);
    }

    /**
     * Return whether the options is supported or not.
     *
     * @param arg string to check whether it is supported option or not.
     * @return true if the option is supported, or return false.
     */
    public boolean isSupportedOption(String arg) {
        return supportedOptions.contains(arg);
    }
}
