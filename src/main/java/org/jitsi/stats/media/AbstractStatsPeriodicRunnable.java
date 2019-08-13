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

import io.callstats.sdk.data.*;
import io.callstats.sdk.listeners.*;
import org.jitsi.utils.concurrent.*;
import org.jitsi.utils.logging.*;

import java.lang.ref.*;
import java.util.*;

/**
 * Implements a {@link RecurringRunnable} which periodically generates a
 * statistics. The statistics is obtained from the two abstract methods
 * which are implemented in classes using this
 * <tt>AbstractStatsPeriodicRunnable</tt>. One method for received statistics
 * and one method for send statistics, all grouped by endpoint ID.
 *
 * @author Damian Minkov
 */
public abstract class AbstractStatsPeriodicRunnable<T>
    extends PeriodicRunnableWithObject<T>
{
    /**
     * The logger.
     */
    private final static Logger logger
        = Logger.getLogger(AbstractStatsPeriodicRunnable.class);

    /**
     * The user info object used to identify the reports to callstats. Holds
     * the conference, the initiatorID and user callstats ID.
     */
    private UserInfo userInfo = null;

    /**
     * The id which identifies the current initiator.
     */
    private String initiatorID;

    /**
     * The conference ID to use when reporting stats.
     */
    private final String conferenceID;

    /**
     * The stats service to use for periodic reports.
     */
    private StatsService statsService;

    /**
     * Constructs <tt>AbstractStatsPeriodicRunnable</tt>.
     *
     * @param o the conference/call used to report statistics.
     * @param period the reporting interval.
     * @param statsService the stats service that will be serving this reporting
     * @param conferenceName the conference name.
     * @param conferenceIDPrefix the conference prefix.
     * @param initiatorID the initiator.
     */
    public AbstractStatsPeriodicRunnable(
        T o,
        long period,
        StatsService statsService,
        String conferenceName,
        String conferenceIDPrefix,
        String initiatorID)
    {
        super(o, period);
        this.statsService = statsService;
        this.initiatorID = initiatorID;

        if (conferenceIDPrefix != null
            && !conferenceIDPrefix.endsWith("/"))
        {
            conferenceIDPrefix += "/";
        }

        this.conferenceID =
            (conferenceIDPrefix != null ? conferenceIDPrefix : "")
                + conferenceName;
    }

    /**
     * Retrieves stats for all endpoints.
     */
    protected abstract List<EndpointStats> getEndpointStats();

    @Override
    protected void doRun()
    {
        CallStats callStats = this.statsService.getCallStats();

        if (userInfo == null
            || callStats == null
            || !callStats.isInitialized())
        {
            return;
        }

        for (EndpointStats endpointStats : getEndpointStats())
        {
            String endpointId = endpointStats.getEndpointId();
            callStats.startStatsReportingForUser(
                endpointId,
                this.conferenceID);

            for (SsrcStats receiveStat : endpointStats.getReceiveStats())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(new StringBuilder()
                        .append("Receive stats (")
                        .append(this.initiatorID)
                        .append(" <- ").append(endpointId).append(") :")
                        .append(receiveStat.toString()));
                }

                ConferenceStatsBuilder conferenceStats
                    = new ConferenceStatsBuilder()
                        .bytesReceived(receiveStat.bytes)
                        .packetsReceived(receiveStat.packets)
                        .packetsLost(receiveStat.packetsLost)
                        .fractionalPacketLost(receiveStat.fractionalPacketLoss)
                        .ssrc(String.valueOf(receiveStat.ssrc))
                        .confID(this.conferenceID)
                        .localUserID(this.initiatorID)
                        .remoteUserID(endpointId)
                        .statsType(CallStatsStreamType.INBOUND)
                        .ucID(userInfo.getUcID());

                if (receiveStat.jitter_ms != null)
                {
                    conferenceStats
                            = conferenceStats.jitter(receiveStat.jitter_ms);
                }

                if (receiveStat.rtt_ms > 0)
                {
                    conferenceStats = conferenceStats.rtt(receiveStat.rtt_ms);
                }

                callStats.reportConferenceStats(
                    endpointId, conferenceStats.build());
            }

            for (SsrcStats sendStat : endpointStats.getSendStats())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(new StringBuilder()
                        .append("Send stats (")
                        .append(this.initiatorID)
                        .append(" -> ").append(endpointId).append(") :")
                        .append(sendStat.toString()));
                }

                ConferenceStatsBuilder conferenceStats
                    = new ConferenceStatsBuilder()
                        .bytesSent(sendStat.bytes)
                        .packetsSent(sendStat.packets)
                        .fractionalPacketLost(sendStat.fractionalPacketLoss)
                        .ssrc(String.valueOf(sendStat.ssrc))
                        .confID(this.conferenceID)
                        .localUserID(this.initiatorID)
                        .remoteUserID(endpointId)
                        .statsType(CallStatsStreamType.OUTBOUND)
                        .ucID(userInfo.getUcID());

                if (sendStat.jitter_ms != null)
                {
                    conferenceStats = conferenceStats.jitter(sendStat.jitter_ms);
                }

                if (sendStat.rtt_ms > 0)
                {
                    conferenceStats = conferenceStats.rtt(sendStat.rtt_ms);
                }

                callStats.reportConferenceStats(
                    endpointId, conferenceStats.build());
            }

            callStats.stopStatsReportingForUser(endpointId, this.conferenceID);
        }
    }

    /**
     * Called when conference/call is created. Sends a setup event to callstats
     * and creates the userInfo object that identifies the statistics for
     * this conference/call.
     */
    public void start()
    {
        ConferenceInfo conferenceInfo
            = new ConferenceInfo(this.conferenceID, this.initiatorID);

        // Send setup event to callstats and on successful response create
        // the userInfo object.
        this.statsService.getCallStats().sendCallStatsConferenceEvent(
            CallStatsConferenceEvents.CONFERENCE_SETUP,
            conferenceInfo,
            new CSStartConferenceListener(new WeakReference<>(this)));
    }

    /**
     * The conference has expired, send terminate event to callstats.
     */
    public void stop()
    {
        if (userInfo != null)
        {
            this.statsService.getCallStats().sendCallStatsConferenceEvent(
                CallStatsConferenceEvents.CONFERENCE_TERMINATED,
                userInfo);
        }
    }

    /**
     * Callstats has finished setting up the conference and we can start
     * sending stats.
     * @param ucid the id used to identify the conference inside callstats.
     */
    private void conferenceSetupResponse(String ucid)
    {
        userInfo = new UserInfo(conferenceID, this.initiatorID, ucid);
    }

    /**
     * Listener that get notified when conference had been processed
     * by callstats and we have the identifier for it and we can start sending
     * stats for it.
     */
    private class CSStartConferenceListener
        implements CallStatsStartConferenceListener
    {
        /**
         * Weak reference for the AbstractStatsPeriodicRunnable, to make sure
         * if this listener got leaked somewhere in callstats we will not keep
         * reference to conferences and such.
         */
        private final WeakReference<AbstractStatsPeriodicRunnable<T>> processible;

        /**
         * Creates listener.
         * @param processible the processible interested in ucid value on
         * successful setup of conference in callstats.
         */
        CSStartConferenceListener(
            WeakReference<AbstractStatsPeriodicRunnable<T>> processible)
        {
            this.processible = processible;
        }

        /**
         * Notified for received user ID.
         * @param ucid the received value for the userID.
         */
        @Override
        public void onResponse(String ucid)
        {
            AbstractStatsPeriodicRunnable p = processible.get();

            // maybe null cause it was garbage collected
            if (p != null)
                p.conferenceSetupResponse(ucid);
        }

        /**
         * Error starting callstats.
         * @param callStatsErrors the error
         * @param message the error message
         */
        @Override
        public void onError(CallStatsErrors callStatsErrors, String message)
        {
            logger.error("Failed to start a callstats conference (???): "
                + message + ", " + callStatsErrors);
        }
    }
}
