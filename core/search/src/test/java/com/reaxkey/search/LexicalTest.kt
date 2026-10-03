package com.reaxkey.search
import org.junit.Assert.*
import org.junit.Test
class LexicalTest {
    @Test fun foldsAccentsAndPunctuation() { assertEquals("reaction cafe",Lexical.normalize("Réaction__CAFÉ!!")) }
    @Test fun queryCannotInjectFtsOperators() { assertEquals("\"cat\"* AND \"or\"* AND \"dog\"*",Lexical.match("cat\" OR dog*")) }
    @Test fun emptyQueryHasNoMatch() { assertEquals("",Lexical.match(" ** ")) }
}
