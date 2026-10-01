package com.reaxkey.model
import org.junit.Assert.*
import org.junit.Test
class MediaRulesTest {
    @Test fun acceptsOnlyDeclaredSupportedTypes() { assertTrue(MediaRules.accepts("image/png",listOf("image/*"))); assertFalse(MediaRules.accepts("image/gif",listOf("image/png"))); assertFalse(MediaRules.accepts("text/html",listOf("*/*"))) }
    @Test fun noDeclarationMeansNoDirectSend() { assertFalse(MediaRules.accepts("image/png",emptyList())) }
    @Test fun distinguishesAnimationAndHeuristicStickers() { assertEquals("ANIMATED_IMAGE",MediaRules.classify("image/webp",true,512,512)); assertEquals("STICKER",MediaRules.classify("image/png",false,128,128)); assertEquals("IMAGE",MediaRules.classify("image/jpeg",false,128,128)) }
    @Test fun interAppUrisMustBeContent() { assertFalse(MediaRules.contentUri("file:///private/a.png")); assertTrue(MediaRules.contentUri("content://media/images/1")) }
}
