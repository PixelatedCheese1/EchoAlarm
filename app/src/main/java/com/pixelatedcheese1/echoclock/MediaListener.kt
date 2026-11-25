package com.pixelatedcheese1.echoclock

import android.service.notification.NotificationListenerService

/** Intentionally empty: having an enabled listener is what authorises MediaSessionManager.getActiveSessions(). */
class MediaListener : NotificationListenerService()
