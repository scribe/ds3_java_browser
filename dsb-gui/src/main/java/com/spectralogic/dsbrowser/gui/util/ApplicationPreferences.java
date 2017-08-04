/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import java.util.prefs.Preferences;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public final class ApplicationPreferences {

    private static final double DEFAULT_X = 10;
    private static final double DEFAULT_Y = 10;
    private static final Preferences pref = Preferences.userRoot().node(NODE_NAME);
    private static final ApplicationPreferences prefInstance = new ApplicationPreferences();

    private ApplicationPreferences() {
    }

    public static ApplicationPreferences getInstance() {
        return prefInstance;
    }

    public static double getX() {
        return pref.getDouble(WINDOW_POSITION_X, DEFAULT_X);
    }

    public static double getY() {
        return pref.getDouble(WINDOW_POSITION_Y, DEFAULT_Y);
    }

    public static double getWidth() {
        return pref.getDouble(WINDOW_WIDTH, Constants.MIN_WIDTH);
    }

    public static double getHeight() {
        return pref.getDouble(WINDOW_HEIGHT, Constants.MIN_HEIGHT);
    }

    public static boolean isWindowMaximized() {
        return pref.getBoolean(WINDOW_MAXIMIZED, false);
    }

}
