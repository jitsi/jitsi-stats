/*
 * Copyright @ 2015 - present, 8x8 Inc
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
package org.jitsi.stats.media;

import org.jitsi.utils.*;

import java.io.*;
import java.util.*;

/**
 * Utility methods for statistics.
 *
 * @author Damian Minkov
 */
public class Utils
{
    /**
     * Sets the default {@code System} properties on which the
     * callstats-java-sdk library depends.
     *
     * @param defaults the {@code Map} in which the default {@code System}
     * properties on which the callstats-java-sdk library depends are to be
     * defined
     */
    public static void getCallStatsJavaSDKSystemPropertyDefaults(
        Map<String, String> defaults)
    {
        getCallStatsJavaSDKSystemPropertyDefaults(
            "log4j2.xml",
            defaults,
            "log4j.configurationFile");
        getCallStatsJavaSDKSystemPropertyDefaults(
            "callstats-java-sdk.properties",
            defaults,
            "callstats.configurationFile");
    }

    /**
     * Sets the default {@code System} properties on which the
     * callstats-java-sdk library depends.
     *
     * @param fileName the file name to use.
     * @param defaults the {@code Map} in which the default {@code System}
     * properties on which the callstats-java-sdk library depends are to be
     * defined
     * @param propertyName the property name to set.
     */
    private static void getCallStatsJavaSDKSystemPropertyDefaults(
        String fileName,
        Map<String, String> defaults,
        String propertyName)
    {
        // There are multiple locations in which we may have put the log4j2.xml
        // file. The callstats-java-sdk library defaults to config/log4j2.xml in
        // the current directory. And that is where we keep the file in our
        // source tree so that works when running from source. Unfortunately,
        // such a location may not work for us when we run from the .deb
        // package.

        List<File> files = new ArrayList<>();

        // Look for log4j2.xml in known locations under the current working
        // directory.
        files.add(new File("config", fileName));
        files.add(new File(fileName));

        // Additionally, look for log4j2.xml in the same known locations under
        // SC_HOME_DIR_LOCATION/SC_HOME_DIR_NAME because that is a directory
        // known to Jitsi-derived projects.
        String scHomeDirName
            = System.getProperty("net.java.sip.communicator.SC_HOME_DIR_NAME");

        if (!StringUtils.isNullOrEmpty(scHomeDirName))
        {
            String scHomeDirLocation
                = System.getProperty(
                        "net.java.sip.communicator.SC_HOME_DIR_LOCATION");

            if (!StringUtils.isNullOrEmpty(scHomeDirLocation))
            {
                File dir = new File(scHomeDirLocation, scHomeDirName);

                if (dir.isDirectory())
                {
                    for (int i = 0, end = files.size(); i < end; ++i)
                        files.add(new File(dir, files.get(i).getPath()));
                }
            }
        }

        // Pick the first existing log4j2.xml from the candidates defined above.
        for (File file : files)
        {
            if (file.exists())
            {
                defaults.put(propertyName, file.getAbsolutePath());
                break;
            }
        }
    }

}
