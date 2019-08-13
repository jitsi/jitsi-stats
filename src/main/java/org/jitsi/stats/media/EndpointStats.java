/*
 * Copyright @ 2019 - present, 8x8 Inc
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

import java.util.*;

/**
 * Contains basic stats about the streams of a single endpoint.
 */
public class EndpointStats
{
    /**
     * The list of stats for streams received from this endpoint.
     */
    private final List<SsrcStats> receiveStats = new LinkedList<>();

    /**
     * The list of stats for streams sent by this endpoint.
     */
    private final List<SsrcStats> sendStats = new LinkedList<>();

    /**
     * The ID of the endpoint.
     */
    private final String endpointId;

    /**
     * Initializes a new {@link EndpointStats} instance.
     */
    public EndpointStats(String endpointId)
    {
        this.endpointId = endpointId;
    }

    /**
     * Adds stats for a receive stream/ssrc.
     */
    public void addReceiveStats(SsrcStats ssrcStats)
    {
        receiveStats.add(ssrcStats);
    }

    /**
     * Adds stats for a send stream/ssrc.
     */
    public void addSendStats(SsrcStats ssrcStats)
    {
        sendStats.add(ssrcStats);
    }

    /**
     * Gets the receive stream stats.
     */
    public List<SsrcStats> getReceiveStats()
    {
        return receiveStats;
    }

    /**
     * Gets the send stream stats.
     */
    public List<SsrcStats> getSendStats()
    {
        return sendStats;
    }

    /**
     * Gets the ID of the endpoint.
     */
    public String getEndpointId()
    {
        return endpointId;
    }

}
