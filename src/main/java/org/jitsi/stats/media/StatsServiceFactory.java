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
import io.callstats.sdk.internal.*;
import io.callstats.sdk.listeners.*;
import org.jitsi.utils.logging2.*;
import org.jitsi.utils.version.*;

import java.util.*;

/**
 * The factory responsible for creating <tt>StatsService</tt> maintain their instances.
 *
 * @author Damian Minkov
 */
public class StatsServiceFactory
{
    /**
     * The {@code Logger} used by the {@code StatsServiceFactory} class to
     * print debug information.
     */
    private static final Logger logger = new LoggerImpl(StatsServiceFactory.class.getName());

    /**
     * The single instance of this factory.
     */
    private static StatsServiceFactory factoryInstance;

    /**
     * All StatsService instances created and started to initialize.
     */
    private Map<Integer, StatsService> callStatsInstances = new HashMap<>();

    /**
     * Returns the single instance of this <tt>StatsServiceFactory</tt>.
     * @return the factory instance.
     */
    public static StatsServiceFactory getInstance()
    {
        if (factoryInstance == null)
        {
            factoryInstance = new StatsServiceFactory();
        }

        return factoryInstance;
    }

    /**
     * Creates <tt>StatsService</tt> and when ready notify via <tt>callback</tt>.
     *
     * @param version the version to use.
     * @param id The callstats AppID.
     * @param appSecret Shared Secret for authentication on Callstats.io
     * @param keyId ID of the key that was used to generate token.
     * @param keyPath The path to private key file.
     * @param initiatorID The initiator id to report to callstats.io.
     * @param isClient The initiator will be reporting client connection (jigasi)
     * not server one (jvb).
     * @param callback  callback to be notified if callstats.io initialized orr failed to do so.
     * @return returns the created service.
     */
    public synchronized StatsService createStatsService(
        final Version version,
        final int id,
        String appSecret,
        String keyId,
        String keyPath,
        String initiatorID,
        boolean isClient,
        final InitCallback callback)
    {
        if (callStatsInstances.containsKey(id))
            return callStatsInstances.get(id);

         // prefer keyId/keyPath over appSecret
        if(keyId == null || keyPath == null)
        {
            logger.warn("KeyID/keyPath missing, will try using appSecret");

            if(appSecret == null)
            {
                callback.error("Missing parameres", "appSecret missing");

                logger.warn("appSecret missing. Skipping callstats init");
                return null;
            }
        }

        ServerInfo serverInfo = createServerInfo(version, isClient);

        final CallStats callStats = new CallStats();

        // The method CallStats.initialize() will (likely) return asynchronously
        // so it may be better to make the new CallStats instance available to
        // the rest of the statistics service before the method in question
        // returns even if it may fail.
        StatsService statsService = new StatsService(id, callStats);
        callStatsInstances.put(id, statsService);

        CallStatsInitListener callStatsInitListener =
            new CallStatsInitListener()
            {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onError(CallStatsErrors error, String errMsg)
                {
                    if (callback != null)
                    {
                        callback.error(error.getReason(), errMsg);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onInitialized(String msg)
                {
                    // callstats get re-initialized every few hours, which
                    // can leads to registering callstats internally many times,
                    // while the service instance is the same
                    // so we return if we have the same service registered
                    StatsService statsService = callStatsInstances.get(id);
                    if (statsService == null || statsService.isInitialized())
                    {
                        return;
                    }
                    statsService.setInitialized(true);

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("callstats.io Java library initialized successfully with message: " + msg);
                    }

                    callback.onInitialized(statsService, msg);
                }
            };

        if(keyId != null && keyPath != null)
        {
            callStats.initialize(
                id,
                new TokenGenerator(
                    String.valueOf(id), keyId, initiatorID, keyPath),
                initiatorID,
                serverInfo,
                callStatsInitListener);
        }
        else
        {
            callStats.initialize(
                id,
                appSecret,
                initiatorID,
                serverInfo,
                callStatsInitListener);
        }

        return statsService;
    }

    /**
     * Stops statistics service with <tt>id</tt>.
     * @param id the id of the StatsService to stop.
     */
    public void stopStatsService(int id)
    {
        callStatsInstances.remove(id);
    }

    /**
     * Initializes a new {@code ServerInfo} instance.
     *
     * @param version the {@code Version} to use.
     * @param isClient The initiator will be reporting client connection (jigasi)
     * not server one (jvb).
     * @return a new {@code ServerInfo} instance
     */
    private ServerInfo createServerInfo(
        Version version,
        boolean isClient)
    {
        ServerInfo serverInfo = new ServerInfo();

        // os
        serverInfo.setOs(System.getProperty("os.name"));

        if (version != null)
        {
            // name
            serverInfo.setName(version.getApplicationName());
            // ver
            serverInfo.setVer(version.toString());
        }

        // the default endpoint type is server (middlebox)
        String endpointType = CallStatsConst.END_POINT_TYPE;

        if (isClient)
        {
            endpointType = "browser";
        }

        serverInfo.setEndpointType(endpointType);

        return serverInfo;
    }

    /**
     * Init callback interface.
     */
    public interface InitCallback
    {
        /**
         * Callstats failed to initialize.
         * @param reason the reason string.
         * @param message the error messages.
         */
        void error(String reason, String message);

        void onInitialized(StatsService statsService, String message);
    }
}
