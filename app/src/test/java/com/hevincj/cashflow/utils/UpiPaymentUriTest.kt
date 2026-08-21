package com.hevincj.cashflow.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiPaymentUriTest {

    @Test
    fun testNormalisePhoneWithCountryCodeAndSpaces() {
        assertEquals("9876543210", UpiIntentBuilder.normalisePhone("+91 98765 43210"))
        assertEquals("9876543210", UpiIntentBuilder.normalisePhone("+91-98765-43210"))
        assertEquals("9876543210", UpiIntentBuilder.normalisePhone("919876543210"))
        assertEquals("9876543210", UpiIntentBuilder.normalisePhone("09876543210"))
        assertEquals("9876543210", UpiIntentBuilder.normalisePhone("9876543210"))
    }

    @Test
    fun testResolveVpaForPhonePe() {
        val vpa = UpiIntentBuilder.resolveVpaForApp("com.phonepe.app", "9876543210")
        assertEquals("9876543210@ybl", vpa)
    }

    @Test
    fun testResolveVpaForPaytm() {
        val vpa = UpiIntentBuilder.resolveVpaForApp("net.one97.paytm", "9876543210")
        assertEquals("9876543210@paytm", vpa)
    }

    @Test
    fun testResolveVpaForBhim() {
        val vpa = UpiIntentBuilder.resolveVpaForApp("in.org.npci.upiapp", "9876543210")
        assertEquals("9876543210@upi", vpa)
    }

    @Test
    fun testResolveVpaForAmazonPay() {
        val vpa = UpiIntentBuilder.resolveVpaForApp("in.amazon.mShop.android.shopping", "9876543210")
        assertEquals("9876543210@apl", vpa)
    }

    @Test
    fun testResolveVpaForGooglePay() {
        val vpa = UpiIntentBuilder.resolveVpaForApp("com.google.android.apps.nbu.paisa.user", "9876543210")
        assertEquals("9876543210@okaxis", vpa)
    }

    @Test
    fun testResolveVpaWithCustomHandleOverridesAppDefault() {
        val vpa = UpiIntentBuilder.resolveVpaForApp("com.phonepe.app", "9876543210", "@okhdfcbank")
        assertEquals("9876543210@okhdfcbank", vpa)

        val vpaWithoutAt = UpiIntentBuilder.resolveVpaForApp("net.one97.paytm", "9876543210", "ptsbi")
        assertEquals("9876543210@ptsbi", vpaWithoutAt)
    }

    @Test
    fun testBuildUpiUriContainsAllMandatoryFields() {
        val uri = UpiIntentBuilder.buildUpiUri(
            vpa = "9876543210@ybl",
            name = "John Doe",
            amount = "150.00",
            note = "Dinner Payment"
        )

        assertTrue("URI must start with upi://pay", uri.startsWith("upi://pay?"))
        assertTrue("URI must contain pa=9876543210@ybl", uri.contains("pa=9876543210%40ybl") || uri.contains("pa=9876543210@ybl"))
        assertTrue("URI must contain pn=John+Doe", uri.contains("pn=John") || uri.contains("pn=John+Doe") || uri.contains("pn=John%20Doe"))
        assertTrue("URI must contain cu=INR", uri.contains("cu=INR"))
        assertTrue("URI must contain am=150.00", uri.contains("am=150.00"))
        assertTrue("URI must contain tn=Dinner", uri.contains("tn=Dinner") || uri.contains("tn=Dinner+Payment"))
    }
}
