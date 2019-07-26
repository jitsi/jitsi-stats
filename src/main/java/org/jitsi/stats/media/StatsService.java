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

import io.callstats.sdk.*;

/**
 * Statistics service that is registered as a service inside OSGi.
 *
 * @author Damian Minkov
 */
public class StatsService
{
    /**
     * The id of the service, corresponds to callstats app_id.
     */
    private final int id;

    /**
     * Callstats instance initialized for this service.
     */
    private final CallStats callStats;

    /**
     * Constructs new <tt>StatsService</tt>.
     * @param id the id.
     * @param callStats the callstats instance.
     */
    StatsService(int id, CallStats callStats)
    {
        this.id = id;
        this.callStats = callStats;
    }

    /**
     * Returns this service id.
     * @return the service id.
     */
    public int getId()
    {
        return id;
    }

    /**
     * Returns the callstats instance for this service.
     * @return the callstats instance.
     */
    CallStats getCallStats()
    {
        return callStats;
    }

    /**
     * Send bridge statistics to callstats.
     * @param stats the bridge statistics.
     */
    public void sendBridgeStatusUpdate(BridgeStatistics stats)
    {
        // Queuing is not implemented by CallStats at the time of this writing.
        if (callStats != null && callStats.isInitialized())
        {
            callStats.sendCallStatsBridgeStatusUpdate(stats.build());
        }
    }
}
