/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.sync.impl.ui.setup

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.duckduckgo.app.CoroutineTestRule
import com.duckduckgo.sync.TestSyncFixtures
import com.duckduckgo.sync.TestSyncFixtures.accountCreatedFailInvalid
import com.duckduckgo.sync.TestSyncFixtures.jsonRecoveryKeyEncoded
import com.duckduckgo.sync.TestSyncFixtures.pdfFile
import com.duckduckgo.sync.impl.Clipboard
import com.duckduckgo.sync.impl.QREncoder
import com.duckduckgo.sync.impl.RecoveryCodePDF
import com.duckduckgo.sync.impl.Result
import com.duckduckgo.sync.impl.SyncRepository
import com.duckduckgo.sync.impl.ui.setup.SaveRecoveryCodeViewModel.Command
import com.duckduckgo.sync.impl.ui.setup.SaveRecoveryCodeViewModel.Command.Finish
import com.duckduckgo.sync.impl.ui.setup.SaveRecoveryCodeViewModel.Command.RecoveryCodePDFSuccess
import com.duckduckgo.sync.impl.ui.setup.SaveRecoveryCodeViewModel.ViewMode.CreatingAccount
import com.duckduckgo.sync.impl.ui.setup.SaveRecoveryCodeViewModel.ViewMode.SignedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveRecoveryCodeViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val qrEncoder: QREncoder = mock()
    private val recoveryPDF: RecoveryCodePDF = mock()
    private val syncRepository: SyncRepository = mock()
    private val clipboard: Clipboard = mock()

    private val testee = SaveRecoveryCodeViewModel(
        qrEncoder,
        recoveryPDF,
        syncRepository,
        clipboard,
        coroutineTestRule.testDispatcherProvider,
    )

    @Test
    fun whenUserIsNotSignedInThenAccountCreatedAndViewStateUpdated() = runTest {
        whenever(syncRepository.isSignedIn()).thenReturn(false)
        whenever(syncRepository.createAccount()).thenReturn(Result.Success(true))
        whenever(syncRepository.getRecoveryCode()).thenReturn(jsonRecoveryKeyEncoded)
        val bitmap = TestSyncFixtures.qrBitmap()
        whenever(qrEncoder.encodeAsBitmap(eq(jsonRecoveryKeyEncoded), any(), any())).thenReturn(bitmap)

        testee.viewState().test {
            val viewState = awaitItem()
            assertTrue(viewState.viewMode is SignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenUserSignedInThenShowViewState() = runTest {
        whenever(syncRepository.isSignedIn()).thenReturn(true)
        whenever(syncRepository.getRecoveryCode()).thenReturn(jsonRecoveryKeyEncoded)
        val bitmap = TestSyncFixtures.qrBitmap()
        whenever(qrEncoder.encodeAsBitmap(eq(jsonRecoveryKeyEncoded), any(), any())).thenReturn(bitmap)

        testee.viewState().test {
            val viewState = awaitItem()
            assertTrue(viewState.viewMode is SignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenCreateAccountFailsThenEmitError() = runTest {
        whenever(syncRepository.isSignedIn()).thenReturn(false)
        whenever(syncRepository.createAccount()).thenReturn(accountCreatedFailInvalid)

        testee.viewState().test {
            val viewState = awaitItem()
            assertTrue(viewState.viewMode is CreatingAccount)
            cancelAndIgnoreRemainingEvents()
        }

        testee.commands().test {
            val command = awaitItem()
            assertTrue(command is Command.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenUserClicksNextThenFinishFlow() = runTest {
        testee.commands().test {
            testee.onNextClicked()
            val command = awaitItem()
            assertTrue(command is Finish)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenUserClicksOnSaveRecoveryCodeThenEmitCheckIfUserHasPermissionCommand() = runTest {
        whenever(syncRepository.getRecoveryCode()).thenReturn(jsonRecoveryKeyEncoded)
        testee.commands().test {
            testee.onSaveRecoveryCodeClicked()
            val command = awaitItem()
            assertTrue(command is Command.CheckIfUserHasStoragePermission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenGenerateRecoveryCodeThenGenerateFileAndEmitSuccessCommand() = runTest {
        whenever(syncRepository.getRecoveryCode()).thenReturn(jsonRecoveryKeyEncoded)
        whenever(recoveryPDF.generateAndStoreRecoveryCodePDF(any(), eq(jsonRecoveryKeyEncoded))).thenReturn(pdfFile())

        testee.commands().test {
            testee.generateRecoveryCode(mock())
            val command = awaitItem()
            assertTrue(command is RecoveryCodePDFSuccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenUserClicksCopyThenCopyToClipboard() = runTest {
        whenever(syncRepository.getRecoveryCode()).thenReturn(jsonRecoveryKeyEncoded)
        testee.commands().test {
            testee.onCopyCodeClicked()
            val command = awaitItem()
            verify(clipboard).copyToClipboard(eq(jsonRecoveryKeyEncoded))
            assertTrue(command is Command.ShowMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}