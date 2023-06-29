package com.vitorhilarioapps.mystock.utils

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UtilExtensionsKtTest {

    @MockK
    lateinit var voidTask: Task<Void>

    @MockK
    lateinit var authTask: Task<AuthResult>

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun runAuthTask_should_return_false() = runTest {
        var success = false

        launch { success = runVoidTask(null) }
        advanceUntilIdle()

        Assertions.assertEquals(false, success)
    }

    @Test
    fun moneyType_should_returns_correct_parser() {
        val value = 53.1543.moneyType()

        Assertions.assertEquals("R$53,15", value)
    }
}