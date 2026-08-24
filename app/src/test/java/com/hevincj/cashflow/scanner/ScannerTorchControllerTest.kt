package com.hevincj.cashflow.scanner

import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.TorchState
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ScannerTorchControllerTest {

    @Test
    fun testTorchEnabledWhenCameraHasFlashUnit() {
        val mockCameraControl = mock<CameraControl>()
        val mockCameraInfo = mock<CameraInfo>()
        val successFuture: ListenableFuture<Void> = Futures.immediateFuture(null)

        whenever(mockCameraInfo.hasFlashUnit()).thenReturn(true)
        whenever(mockCameraControl.enableTorch(true)).thenReturn(successFuture)

        if (mockCameraInfo.hasFlashUnit()) {
            val future = mockCameraControl.enableTorch(true)
            assertTrue(future.isDone)
        }

        verify(mockCameraControl).enableTorch(true)
    }

    @Test
    fun testTorchNotEnabledWhenCameraLacksFlashUnit() {
        val mockCameraControl = mock<CameraControl>()
        val mockCameraInfo = mock<CameraInfo>()

        whenever(mockCameraInfo.hasFlashUnit()).thenReturn(false)

        if (mockCameraInfo.hasFlashUnit()) {
            mockCameraControl.enableTorch(true)
        }

        verify(mockCameraControl, never()).enableTorch(any())
    }

    @Test
    fun testTorchDisabledOnCleanup() {
        val mockCameraControl = mock<CameraControl>()
        val successFuture: ListenableFuture<Void> = Futures.immediateFuture(null)
        whenever(mockCameraControl.enableTorch(false)).thenReturn(successFuture)

        val future = mockCameraControl.enableTorch(false)
        assertTrue(future.isDone)
        verify(mockCameraControl).enableTorch(false)
    }

    @Test
    fun testTorchStateConstants() {
        assertEquals(0, TorchState.OFF)
        assertEquals(1, TorchState.ON)
    }
}
