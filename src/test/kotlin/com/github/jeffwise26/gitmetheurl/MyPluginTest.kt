package com.github.jeffwise26.gitmetheurl

import com.github.jeffwise26.gitmetheurl.action.GitMeTheUrlAction
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun test() {
      val action = GitMeTheUrlAction()
    }
}
