package com.xg.mycomposeapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingMessageTest {

  @Test
  fun build_returnsCorrectMessage() {
    val name = "Android"
    val expected = "Hello greeing again Android!"
    assertEquals(expected, GreetingMessage.build(name))
  }

  @Test
  fun build_handlesEmptyName() {
    assertEquals("Hello greeing again !", GreetingMessage.build(""))
  }

  @Test
  fun build_handlesLongName() {
    val longName = "A".repeat(100)
    val expected = "Hello greeing again ${"A".repeat(100)}!"
    assertEquals(expected, GreetingMessage.build(longName))
  }

  // 🔥 唯一新增：覆盖 test() 方法
  @Test
  fun test_executesWithoutError() {
    GreetingMessage.test()
  }
}
