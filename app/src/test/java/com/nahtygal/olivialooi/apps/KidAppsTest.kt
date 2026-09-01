package com.nahtygal.olivialooi.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KidAppsTest {
    @Test
    fun menuContainsOnlyInitialApprovedAppsInOrder() {
        assertEquals(listOf("youtube_kids", "disney_plus"), KidApps.all.map(KidApp::id))
    }

    @Test
    fun packageNamesMatchOfficialAndroidApps() {
        assertEquals(
            "com.google.android.apps.youtube.kids",
            KidApps.all.first().packageName,
        )
        assertEquals("com.disney.disneyplus", KidApps.all.last().packageName)
    }

    @Test
    fun packageLookupReturnsMatchingApp() {
        val disney = KidApps.findByPackageName("com.disney.disneyplus")

        assertEquals("disney_plus", disney?.id)
    }

    @Test
    fun packageLookupRejectsUnknownApp() {
        assertNull(KidApps.findByPackageName("example.not.approved"))
    }

    @Test
    fun appIdsAndPackageNamesAreUnique() {
        assertEquals(KidApps.all.size, KidApps.all.map(KidApp::id).toSet().size)
        assertEquals(KidApps.all.size, KidApps.all.map(KidApp::packageName).toSet().size)
        assertTrue(KidApps.all.all { it.id.isNotBlank() && it.packageName.isNotBlank() })
    }
}
