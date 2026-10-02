package com.reaxkey.model
import org.junit.Assert.*
import org.junit.Test
class MediaRulesTest {
    @Test fun acceptsOnlyDeclaredSupportedTypes() { assertTrue(MediaRules.accepts("image/png",listOf("image/*"))); assertFalse(MediaRules.accepts("image/gif",listOf("image/png"))); assertFalse(MediaRules.accepts("text/html",listOf("*/*"))) }
    @Test fun noDeclarationMeansNoDirectSend() { assertFalse(MediaRules.accepts("image/png",emptyList())) }
    @Test fun distinguishesAnimationAndHeuristicStickers() { assertEquals("ANIMATED_IMAGE",MediaRules.classify("image/webp",true,512,512)); assertEquals("STICKER",MediaRules.classify("image/png",false,128,128)); assertEquals("IMAGE",MediaRules.classify("image/jpeg",false,128,128)) }
    @Test fun interAppUrisMustBeContent() { assertFalse(MediaRules.contentUri("file:///private/a.png")); assertTrue(MediaRules.contentUri("content://media/images/1")) }
    @Test fun stickerCandidatesRejectPhotosAndOversizedImages() {
        assertTrue(StickerRules.candidate("image/webp",512,512))
        assertTrue(StickerRules.candidate("image/gif",320,320))
        assertFalse(StickerRules.candidate("image/jpeg",512,512))
        assertFalse(StickerRules.candidate("image/png",4032,3024))
        assertFalse(StickerRules.candidate("image/webp",0,512))
    }
}
