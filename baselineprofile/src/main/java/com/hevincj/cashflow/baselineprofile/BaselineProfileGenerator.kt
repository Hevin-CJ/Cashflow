package com.hevincj.cashflow.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile for the CashFlow app.
 *
 * Run with:
 *   ./gradlew :baselineprofile:generateBaselineProfile
 *
 * The generated profile will be written to:
 *   app/src/main/baseline-prof.txt
 *
 * For guidance on how to write these tests, refer to:
 *   https://d.android.com/baseline-profiles
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.hevincj.cashflow",
            profileBlock = {
                // Start from a clean slate by pressing the home button on the device
                pressHome()
                // Find the app on the home screen and launch it
                startActivityAndWait()

                // Wait for the home screen to load (main content area)
                device.waitForIdle()

                // Scroll the transaction list to capture list scrolling code paths
                device.findObject(
                    androidx.test.uiautomator.By.scrollable(true)
                )?.let { scrollable ->
                    scrollable.scroll(androidx.test.uiautomator.Direction.DOWN, 1.0f)
                    scrollable.scroll(androidx.test.uiautomator.Direction.UP, 1.0f)
                }
            }
        )
    }
}
