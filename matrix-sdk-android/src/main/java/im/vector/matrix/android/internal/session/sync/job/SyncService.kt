/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.matrix.android.internal.session.sync.job

import android.app.Service
import android.content.Intent
import android.os.IBinder
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.failure.isTokenError
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.session.sync.SyncTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Can execute periodic sync task.
 * An IntentService is used in conjunction with the AlarmManager and a Broadcast Receiver
 * in order to be able to perform a sync even if the app is not running.
 * The <receiver> and <service> must be declared in the Manifest or the app using the SDK
 */
abstract class SyncService : Service() {

    private var userId: String? = null
    private var mIsSelfDestroyed: Boolean = false

    private var isInitialSync: Boolean = false
    private lateinit var session: Session
    private lateinit var syncTask: SyncTask
    private lateinit var networkConnectivityChecker: NetworkConnectivityChecker
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var coroutineDispatchers: MatrixCoroutineDispatchers
    private lateinit var backgroundDetectionObserver: BackgroundDetectionObserver

    private val isRunning = AtomicBoolean(false)

    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand $intent")
        intent?.let {
            val matrix = Matrix.getInstance(applicationContext)
            val safeUserId = it.getStringExtra(EXTRA_USER_ID) ?: return@let
            val sessionComponent = matrix.sessionManager.getSessionComponent(safeUserId)
                    ?: return@let
            session = sessionComponent.session()
            userId = safeUserId
            syncTask = sessionComponent.syncTask()
            isInitialSync = !session.hasAlreadySynced()
            networkConnectivityChecker = sessionComponent.networkConnectivityChecker()
            taskExecutor = sessionComponent.taskExecutor()
            coroutineDispatchers = sessionComponent.coroutineDispatchers()
            backgroundDetectionObserver = matrix.backgroundDetectionObserver
            onStart(isInitialSync)
            if (isRunning.get()) {
                Timber.i("Received a start while was already syncing... ignore")
            } else {
                isRunning.set(true)
                serviceScope.launch(coroutineDispatchers.io) {
                    doSync()
                }
            }
        }
        // No intent just start the service, an alarm will should call with intent
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.i("## onDestroy() : $this")
        if (!mIsSelfDestroyed) {
            Timber.w("## Destroy by the system : $this")
        }
        serviceScope.coroutineContext.cancelChildren()
        isRunning.set(false)
        super.onDestroy()
    }

    private fun stopMe() {
        mIsSelfDestroyed = true
        stopSelf()
    }

    private suspend fun doSync() {
        if (!networkConnectivityChecker.hasInternetAccess()) {
            Timber.v("No network reschedule to avoid wasting resources")
            userId?.also {
                onRescheduleAsked(it, isInitialSync, delay = 10_000L)
            }
            stopMe()
            return
        }
        Timber.v("Execute sync request with timeout 0")
        val params = SyncTask.Params(TIME_OUT)
        try {
            syncTask.execute(params)
            // Start sync if we were doing an initial sync and the syncThread is not launched yet
            if (isInitialSync && session.syncState().value == SyncState.Idle) {
                val isForeground = !backgroundDetectionObserver.isInBackground
                session.startSync(isForeground)
            }
            stopMe()
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            if (throwable.isTokenError()) {
                stopMe()
            } else {
                Timber.v("Retry to sync in 5s")
                delay(DELAY_FAILURE)
                doSync()
            }
        }
    }

    abstract fun onStart(isInitialSync: Boolean)

    abstract fun onRescheduleAsked(userId: String, isInitialSync: Boolean, delay: Long)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        private const val TIME_OUT = 0L
        private const val DELAY_FAILURE = 5_000L
    }
}
