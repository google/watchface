/*
 * Copyright 2023 Google LLC
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

package com.google.wear.watchface.dfx.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A UserConfigSet is a pairing of {@link UserConfigKey UserConfigiKeys} to {@link UserConfigValue
 * UserConifigurationValues}.
 */
class UserConfigSet {
    final Map<UserConfigKey, UserConfigValue> config;

    UserConfigSet(Map<UserConfigKey, UserConfigValue> config) {
        this.config = config;
    }

    UserConfigValue get(UserConfigKey key) {
        return config.get(key);
    }

    public boolean containsKey(UserConfigKey key) {
        return config.containsKey(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserConfigSet)) return false;
        UserConfigSet that = (UserConfigSet) o;
        return Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config);
    }

    @Override
    public String toString() {
        return "UserConfigSet{" + "config=" + config + '}';
    }

    /**
     * Create a copy of this UserConfigSet plus the given key-value pair. Does not mutate the
     * original value.
     */
    UserConfigSet plus(UserConfigKey key, UserConfigValue value) {
        HashMap<UserConfigKey, UserConfigValue> copyMap = new HashMap<>(config);
        copyMap.put(key, value);
        return new UserConfigSet(copyMap);
    }

    static UserConfigSet singleton(UserConfigKey key, UserConfigValue value) {
        HashMap<UserConfigKey, UserConfigValue> map = new HashMap<>();
        map.put(key, value);
        return new UserConfigSet(map);
    }
}
