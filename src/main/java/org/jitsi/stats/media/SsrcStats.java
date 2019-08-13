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

/**
 * Basic statistics for a single SSRC.
 */
public class SsrcStats
{
    /**
     * Number of bytes sent or received.
     */
    public long bytes = 0;

    /**
     * Number of packets sent or received.
     */
    public long packets = 0;

    /**
     * Number of packets lost.
     */
    public long packetsLost = 0;

    /**
     * The fraction of lost packets.
     */
    public double fractionalPacketLoss = 0d;

    /**
     * The SSRC for which the stats apply.
     */
    public long ssrc = -1;

    /**
     * The jitter in milliseconds.
     */
    public Double jitter_ms = null;

    /**
     * The RTT in milliseconds.
     */
    public int rtt_ms = -1;

    @Override
    public String toString()
    {
        return new StringBuilder("SsrcStats")
                .append("ssrc=").append(ssrc)
                .append("bytes=").append(bytes)
                .append(", packets=").append(packets)
                .append(", packetLost=").append(packetsLost)
                .append(", fractionalPacketLoss=").append(fractionalPacketLoss)
                .append(", jitter_ms=").append(jitter_ms)
                .append(", rtt_ms=").append(rtt_ms)
                .toString();
    }
}
