package com.nahtygal.olivialooi.profile

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KidProfileRepositoryTest {
    @Test
    fun `Olivia has stable identity and display name`() {
        assertEquals("olivia", KidProfile.OLIVIA.stableId)
        assertEquals("Olivia", KidProfile.OLIVIA.displayName)
    }

    @Test
    fun `Eliana has stable identity and display name`() {
        assertEquals("eliana", KidProfile.ELIANA.stableId)
        assertEquals("Eliana", KidProfile.ELIANA.displayName)
    }

    @Test
    fun `legacy default is Olivia`() {
        assertEquals(KidProfile.OLIVIA, KidProfile.LEGACY_DEFAULT)
        assertEquals(KidProfile.OLIVIA, repository().selectedProfile)
    }

    @Test
    fun `selecting Olivia persists its stable ID`() {
        val store = InMemoryKidProfileIdStore()

        repository(store).selectProfile(KidProfile.OLIVIA)

        assertEquals("olivia", store.profileId)
        assertEquals(KidProfile.OLIVIA, repository(store).selectedProfile)
    }

    @Test
    fun `selecting Eliana persists its stable ID`() {
        val store = InMemoryKidProfileIdStore()

        repository(store).selectProfile(KidProfile.ELIANA)

        assertEquals("eliana", store.profileId)
        assertEquals(KidProfile.ELIANA, repository(store).selectedProfile)
    }

    @Test
    fun `selection survives repository recreation`() {
        val store = InMemoryKidProfileIdStore()
        repository(store).selectProfile(KidProfile.ELIANA)

        val recreatedRepository = repository(store)

        assertEquals(KidProfile.ELIANA, recreatedRepository.selectedProfile)
    }

    @Test
    fun `unknown persisted ID safely falls back to legacy default`() {
        val store = InMemoryKidProfileIdStore(profileId = "someone-else")

        assertEquals(KidProfile.OLIVIA, repository(store).selectedProfile)
    }

    @Test
    fun `profile deserialization is exact and cannot cross identities`() {
        assertEquals(KidProfile.OLIVIA, KidProfile.fromStableId("olivia"))
        assertEquals(KidProfile.ELIANA, KidProfile.fromStableId("eliana"))
        assertNotEquals(KidProfile.OLIVIA, KidProfile.fromStableId("eliana"))
        assertNotEquals(KidProfile.ELIANA, KidProfile.fromStableId("olivia"))
        assertNull(KidProfile.fromStableId("Olivia"))
    }

    @Test
    fun `domain model has no Android or UI dependencies`() {
        val source = projectFile(
            "src/main/java/com/nahtygal/olivialooi/profile/KidProfile.kt",
        ).readText()

        assertFalse(source.contains("import android."))
        assertFalse(source.contains("import androidx."))
        assertFalse(source.contains("compose", ignoreCase = true))
    }

    private fun repository(
        store: InMemoryKidProfileIdStore = InMemoryKidProfileIdStore(),
    ): KidProfileRepository = PersistentKidProfileRepository(store)

    private fun projectFile(relativePath: String): File = File(relativePath).let { direct ->
        if (direct.exists()) direct else File("app", relativePath)
    }

    private class InMemoryKidProfileIdStore(
        var profileId: String? = null,
    ) : KidProfileIdStore {
        override fun readProfileId(): String? = profileId

        override fun writeProfileId(stableId: String) {
            profileId = stableId
        }
    }
}
