package com.nahtygal.olivialooi.speech

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class StoryOfflineVoiceTest {
    private val cloud = LooLooVoiceCandidate("cloud",Locale.US,500,100,true)
    private val local = LooLooVoiceCandidate("local",Locale.UK,200,100,false)
    @Test fun `strict local ignores preferred cloud voice`() { assertEquals(local,LooLooVoiceSelector.select(listOf(cloud,local),"cloud",offlineOnly=true)) }
    @Test fun `strict local has no network fallback`() { assertNull(LooLooVoiceSelector.select(listOf(cloud),offlineOnly=true)) }
    @Test fun `existing callers retain prior preference behavior`() { assertEquals(cloud,LooLooVoiceSelector.select(listOf(cloud,local),"cloud")) }
}
