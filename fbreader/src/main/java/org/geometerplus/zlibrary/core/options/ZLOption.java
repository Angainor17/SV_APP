/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.core.options;

public abstract class ZLOption {
    public final StringPair myId;
    public ConfigInstance Config = new ConfigInstance();
    protected String myDefaultStringValue;
    protected String mySpecialName;

    protected ZLOption(String group, String optionName, String defaultStringValue) {
        myId = new StringPair(group, optionName);
        myDefaultStringValue = defaultStringValue != null ? defaultStringValue : "";
    }

    public final void setSpecialName(String specialName) {
        mySpecialName = specialName;
    }

    public void saveSpecialValue() {
    }

    protected final String getConfigValue() {
        final Config config = Config.Instance();
        return config != null ? config.getValue(myId, myDefaultStringValue) : myDefaultStringValue;
    }

    protected final void setConfigValue(String value) {
        final Config config = Config.Instance();
        if (config != null) {
            if (!myDefaultStringValue.equals(value)) {
                config.setValue(myId, value);
            } else {
                config.unsetValue(myId);
            }
        }
    }

    public static class ConfigInstance {
        public Config Instance() {
            return org.geometerplus.zlibrary.core.options.Config.Instance();
        }
    }
}
