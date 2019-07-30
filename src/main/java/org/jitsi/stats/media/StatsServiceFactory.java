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
import org.jitsi.utils.version.*;
import org.jitsi.utils.version.Version;
import org.jitsi.utils.logging.Logger;
import org.osgi.framework.*;

import java.util.*;

/**
 * The factory responsible for creating <tt>StatsService</tt> maintain their
 * instances and register them to OSGi.
 *
 * @author Damian Minkov
 */
public class StatsServiceFactory
{
    /**
     * The {@code Logger} used by the {@code StatsServiceFactory} class to
     * print debug information.
     */
    private static final Logger logger
        = Logger.getLogger(StatsServiceFactory.class);

    /**
     * The single instance of this factory.
     */
    private static StatsServiceFactory factoryInstance;

    /**
     * All callstats instances created and started to initialize.
     */
    private Map<Integer, CallStats> callStatsInstances = new HashMap<>();

    /**
     * The OSGi service registrations. Kept so we can have all instances and
     * to be able to unregister them from OSGi.
     */
    private Map<Integer, ServiceRegistration<StatsService>>
        statsServiceInstanceRegistrations = new HashMap<>();

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
     * Returns <tt>StatsService</tt> instance with <tt>id</tt>, retrieved from
     * <tt>BundleContext</tt>.
     *
     * @param id the id of the <tt>StatsService</tt>.
     * @param context the OSGi bundle context.
     * @return <tt>StatsService</tt> instance or null if not found.
     */
    public StatsService getStatsService(int id, BundleContext context)
    {
        ServiceRegistration<StatsService> reg
            = statsServiceInstanceRegistrations.get(id);

        if (reg == null)
            return null;

        return context.getService(reg.getReference());
    }

    /**
     * Creates <tt>StatsService</tt> and when ready register it to OSGi.
     *
     * @param context the OSGi bundle context to use.
     * @param id The callstats AppID.
     * @param appSecret Shared Secret for authentication on Callstats.io
     * @param keyId ID of the key that was used to generate token.
     * @param keyPath The path to private key file.
     * @param initiatorID The initiator id to report to callstats.io.
     * @param isClient The initiator will be reporting client connection (jigasi)
     * not server one (jvb).
     */
    public synchronized void createStatsService(
        BundleContext context,
        int id,
        String appSecret,
        String keyId,
        String keyPath,
        String initiatorID,
        boolean isClient)
    {
        createStatsService(
            context,
            id,
            appSecret,
            keyId,
            keyPath,
            initiatorID,
            isClient,
            (reason, errorMsg)
                -> logger.error("callstats.io Java library failed to "
                    + "initialize with error: " + reason
                    + " and error message: " + errorMsg)
        );
    }

    /**
     * Creates <tt>StatsService</tt> and when ready register it to OSGi.
     *
     * @param context the OSGi bundle context to use.
     * @param id The callstats AppID.
     * @param appSecret Shared Secret for authentication on Callstats.io
     * @param keyId ID of the key that was used to generate token.
     * @param keyPath The path to private key file.
     * @param initiatorID The initiator id to report to callstats.io.
     * @param isClient The initiator will be reporting client connection (jigasi)
     * not server one (jvb).
     * @param errorCallback error callback to be notified if callstats.io failed
     * to initialize.
     */
    public synchronized void createStatsService(
        final BundleContext context,
        final int id,
        String appSecret,
        String keyId,
        String keyPath,
        String initiatorID,
        boolean isClient,
        final InitErrorCallback errorCallback)
    {
        if (callStatsInstances.containsKey(id))
            return;

         // prefer keyId/keyPath over appSecret
        if(keyId == null || keyPath == null)
        {
            logger.warn("KeyID/keyPath missing, will try using appSecret");

            if(appSecret == null)
            {
                errorCallback.errorCallback(
                    "Missing parameres", "appSecret missing");

                logger.warn("appSecret missing. Skipping callstats init");
                return;
            }
        }

        ServerInfo serverInfo = createServerInfo(context, isClient);

        final CallStats callStats = new CallStats();

        // The method CallStats.initialize() will (likely) return asynchronously
        // so it may be better to make the new CallStats instance available to
        // the rest of the statistics service before the method in question
        // returns even if it may fail.
        callStatsInstances.put(id, callStats);

        CallStatsInitListener callStatsInitListener =
            new CallStatsInitListener()
            {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onError(CallStatsErrors error, String errMsg)
                {
                    if (errorCallback != null)
                    {
                        errorCallback.errorCallback(error.getReason(), errMsg);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onInitialized(String msg)
                {
                    callStatsOnInitialized(id, context, callStats, msg);
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
    }

    /**
     * Stops statistics service with <tt>id</tt>.
     * @param context the OSGi bundle context.
     * @param id the id of the StatsService to stop.
     */
    public void stopStatsService(BundleContext context, int id)
    {
        ServiceRegistration serviceRegistration
            = statsServiceInstanceRegistrations.remove(id);

        callStatsInstances.remove(id);

        if (serviceRegistration == null)
            return;

        serviceRegistration.unregister();
    }

    /**
     * Initializes a new {@code ServerInfo} instance.
     *
     * @param bundleContext the {@code BundleContext} in which the method is
     * invoked
     * @param isClient The initiator will be reporting client connection (jigasi)
     * not server one (jvb).
     * @return a new {@code ServerInfo} instance
     */
    private ServerInfo createServerInfo(
        BundleContext bundleContext,
        boolean isClient)
    {
        ServerInfo serverInfo = new ServerInfo();

        // os
        serverInfo.setOs(System.getProperty("os.name"));

        // name & ver
        ServiceReference<VersionService> serviceReference
            = bundleContext == null
                ? null : bundleContext.getServiceReference(VersionService.class);

        VersionService versionService
            = (serviceReference == null)
                ? null : bundleContext.getService(serviceReference);

        if (versionService != null)
        {
            Version version = versionService.getCurrentVersion();

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
     * Notifies this {@code StatsServiceFactory} that a specific
     * {@code CallStats} failed to initialize.
     *
     * @param callStats the {@code CallStats} which failed to initialize
     * @param error the error
     * @param errMsg the error message
     */
    private void callStatsOnError(
        CallStats callStats,
        CallStatsErrors error,
        String errMsg)
    {
        logger.error(
            "callstats.io Java library failed to initialize with error: "
                + error + " and error message: " + errMsg);
    }

    /**
     * Notifies this {@code StatsServiceFactory} that a specific
     * {@code CallStats} initialized.
     *
     * @param callStats the {@code CallStats} which initialized
     * @param msg the message sent by {@code callStats} upon the successful
     * initialization
     */
    private void callStatsOnInitialized(
        int id,
        BundleContext context,
        CallStats callStats,
        String msg)
    {
        // callstats get re-initialized every few hours, which
        // can leads to registering callstats in osgi many times,
        // while the service instance is the same
        // so we return if we have the same service registered
        if (statsServiceInstanceRegistrations.containsKey(id))
            return;

        if (logger.isDebugEnabled())
        {
            logger.debug(
                "callstats.io Java library initialized successfully"
                    + " with message: " + msg);
        }


        ServiceRegistration<StatsService> serviceRegistration
            = context.registerService(
                StatsService.class,
                new StatsService(id, callStats),
                null);
        statsServiceInstanceRegistrations.put(id, serviceRegistration);
    }

    /**
     * Error callback interface.
     */
    public interface InitErrorCallback
    {
        /**
         * Callstats failed to initilize.
         * @param reason the reason string.
         * @param errorMessage the error messages.
         */
        void errorCallback(String reason, String errorMessage);
    }
}
