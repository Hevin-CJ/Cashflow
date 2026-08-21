package com.hevincj.cashflow.config

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProguardRulesIntegrityTest {

    @Test
    fun testProguardRulesContainRequiredMlKitAndCrashlyticsKeeps() {
        val possiblePaths = listOf(
            File("proguard-rules.pro"),
            File("app/proguard-rules.pro"),
            File("../app/proguard-rules.pro")
        )
        val proguardFile = possiblePaths.firstOrNull { it.exists() }
        assertTrue("proguard-rules.pro should exist at a known location", proguardFile != null && proguardFile.exists())

        val content = proguardFile!!.readText()

        // 1. Crashlytics line numbers & exceptions
        assertTrue("Must keep SourceFile,LineNumberTable attributes", content.contains("-keepattributes SourceFile,LineNumberTable"))
        assertTrue("Must keep Exception classes", content.contains("-keep public class * extends java.lang.Exception"))
        assertTrue("Must keep Crashlytics members", content.contains("com.google.firebase.crashlytics.**"))

        // 2. Google MLKit & Play Services Vision (Prevents R8 ComponentRegistrar stripping)
        assertTrue("Must keep com.google.mlkit.**", content.contains("-keep class com.google.mlkit.** { *; }"))
        assertTrue("Must keep MLKit vision barcode internals", content.contains("-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }"))
        assertTrue("Must keep Play Services vision", content.contains("-keep class com.google.android.gms.vision.** { *; }"))
        assertTrue("Must keep Firebase components", content.contains("-keep class com.google.firebase.components.** { *; }"))

        // 3. Serialization DTOs and Room entities
        assertTrue("Must keep DTO models", content.contains("com.hevincj.cashflow.data.remote.models.**"))
        assertTrue("Must keep Room entities", content.contains("com.hevincj.cashflow.data.local.entity.**"))
    }
}
