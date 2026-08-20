package com.hevincj.cashflow.utils

import org.junit.Test
import java.io.IOException

class CrashLoggerCancelGuardTest {

    @Test
    fun crashLogger_w_handlesCanceledIOExceptionSafely() {
        val canceledEx = IOException("Canceled")
        // Should not throw or crash
        CrashLogger.w("TestTag", "Request cancelled", canceledEx)
    }

    @Test
    fun crashLogger_e_handlesCanceledIOExceptionSafely() {
        val canceledEx = IOException("Canceled")
        // Should not throw or crash
        CrashLogger.e("TestTag", "Fatal cancellation event", canceledEx)
    }

    @Test
    fun crashLogger_recordException_handlesCanceledIOExceptionSafely() {
        val canceledEx = IOException("Canceled")
        // Should silently ignore without crashing
        CrashLogger.recordException(canceledEx)
    }

    @Test
    fun crashLogger_handlesRealIoExceptionSafely() {
        val realEx = IOException("Failed to connect to backend")
        CrashLogger.w("TestTag", "Real network error", realEx)
        CrashLogger.e("TestTag", "Real network error", realEx)
        CrashLogger.recordException(realEx)
    }
}
