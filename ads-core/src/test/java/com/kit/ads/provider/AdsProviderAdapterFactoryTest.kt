package com.kit.ads.provider

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [24])
class AdsProviderAdapterFactoryTest {

    @Test
    fun spi_admobAndApplovin_areAvailableOnTestClasspath() {
        assertTrue(AdsProviderAdapterFactory.isAvailable(AdsProviderType.ADMOB))
        assertTrue(AdsProviderAdapterFactory.isAvailable(AdsProviderType.APPLOVIN))
    }

    @Test
    fun spi_create_returnsProviderAdapters() {
        val admob = AdsProviderAdapterFactory.create(AdsProviderType.ADMOB)
        val applovin = AdsProviderAdapterFactory.create(AdsProviderType.APPLOVIN)

        assertNotNull(admob)
        assertNotNull(applovin)
        assertTrue(admob.javaClass.name.endsWith("AdmobProviderAdapter"))
        assertTrue(applovin.javaClass.name.endsWith("ApplovinProviderAdapter"))
    }
}
