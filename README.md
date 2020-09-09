jitsi-stats
======

This is a wrapper around [callstats.io] to encapsulate common logic used in [jvb] and [jigasi].

Usage:
======

To use it AbstractStatsPeriodicRunnable needs to be extended passing to the constructor:
- o: [Conference] or [Call]
- period: the reporting interval
- statsService: the stats service created using the StatsServiceFactory
- conferenceJid: A jid of the conference name@conference.domain.com or name@conference.tenant.domain.com
- conferenceIDPrefix: The conference prefix, this is the domain of the deployment.
- initiatorID: The initiator for these stats, a string representing either the bridge or jigasi. And later this is seen in [callstats.io] dashboard.

When reporting to [callstats.io] we will report sideId as `tenant` in case of jid of type `name@conference.tenant.domain.com` or `/` in case of `name@conference.domain.com`
and all this if `conferenceIDPrefix` is `domain.com`. 

There is a conference identification (conferenceId), which is the string used to search in [callstats.io] dashboard.
- The conferenceId will be `domain.com/tenant/name` if we use jid: `name@conference.tenant.domain.com` and `conferenceIDPrefix` is `domain.com`.
- If there is no `conferenceIDPrefix` passed we will set conference identification to be just `name` and siteId will be `/`;
- If there is `conferenceIDPrefix` but the jid does not contain this configure prefix, `name@someother.d.com` then conference identification will be just `domain.com/name`.

[jvb]: https://github.com/jitsi/jitsi-videobridge
[jigasi]: https://github.com/jitsi/jigasi
[callstats.io]: https://www.callstats.io/
[Conference]: https://github.com/jitsi/jitsi-videobridge/blob/master/jvb/src/main/java/org/jitsi/videobridge/Conference.java#L61 
[Call]: https://github.com/jitsi/jitsi/blob/master/src/net/java/sip/communicator/service/protocol/Call.java
