package com.example.padavancontrol.monitoring

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.DefaultPadavanRepository
import com.example.padavancontrol.data.SettingsDataStore
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.`when` as whenever

class RouterMonitorWorkerTest {

    @Test
    fun testDoWorkMonitoringDisabled() = runTest {
        val context = mock(Context::class.java)
        whenever(context.applicationContext).thenReturn(context)
        val params = mock(WorkerParameters::class.java)

        mockConstruction(SettingsDataStore::class.java) { mockStore, _ ->
            whenever(mockStore.isMonitoringEnabled()).thenReturn(flowOf(false))
        }.use {
            val worker = RouterMonitorWorker(context, params)
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun testDoWorkMonitoringEnabledTempAlert() = runTest {
        val context = mock(Context::class.java)
        whenever(context.applicationContext).thenReturn(context)
        val params = mock(WorkerParameters::class.java)

        mockConstruction(SettingsDataStore::class.java) { mockStore, _ ->
            whenever(mockStore.isMonitoringEnabled()).thenReturn(flowOf(true))
            whenever(mockStore.isNotifyHighTemp()).thenReturn(flowOf(true))
            whenever(mockStore.getHighTempThreshold()).thenReturn(flowOf(75))
            whenever(mockStore.isNotifyWanDisconnect()).thenReturn(flowOf(false))
            whenever(mockStore.isNotifyNewDevice()).thenReturn(flowOf(false))
            whenever(mockStore.getLastKnownWanIp()).thenReturn(flowOf(null))
            whenever(mockStore.getLastKnownDeviceMacs()).thenReturn(flowOf(emptySet()))
            whenever(mockStore.getLastKnownOnline()).thenReturn(flowOf(null))
        }.use {
            val systemStatus = SystemStatus(
                cpuUsage = 10,
                ramTotal = 512000000L,
                ramUsed = 256000000L,
                uptime = 86400L,
                cpuTemp = 80.0f,
                wifiTemp = 45.0f
            )

            mockConstruction(CredentialStore::class.java).use { mockCreds ->
                mockConstruction(DefaultPadavanRepository::class.java) { mockInst, _ ->
                    whenever(mockInst.getSystemStatus()).thenReturn(flowOf(Result.success(systemStatus)))
                    whenever(mockInst.getWanStatus()).thenReturn(flowOf(Result.success(WanStatus(isConnected = true, wanIp = "120.24.53.111", gateway = "120.24.53.1", dns1 = "8.8.8.8", dns2 = "8.8.4.4", connectionType = "PPPoE", uptime = 3600L))))
                    whenever(mockInst.getLanClients()).thenReturn(flowOf(Result.success(emptyList())))
                }.use {
                    mockConstruction(NotificationHelper::class.java).use { mockNotif ->
                        val worker = RouterMonitorWorker(context, params)
                        val result = worker.doWork()
                        assertEquals(ListenableWorker.Result.success(), result)
                    }
                }
            }
        }
    }
}
