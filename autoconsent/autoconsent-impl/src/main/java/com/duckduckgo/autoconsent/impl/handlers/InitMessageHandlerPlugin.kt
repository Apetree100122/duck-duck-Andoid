/*
 * Copyright (c) 2022 DuckDuckGo
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

package com.duckduckgo.autoconsent.impl.handlers

import android.webkit.WebView
import androidx.core.net.toUri
import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.app.global.DispatcherProvider
import com.duckduckgo.app.global.isHttp
import com.duckduckgo.app.global.isHttps
import com.duckduckgo.autoconsent.impl.JsReader
import com.duckduckgo.autoconsent.impl.MessageHandlerPlugin
import com.duckduckgo.autoconsent.impl.adapters.JSONObjectAdapter
import com.duckduckgo.di.scopes.AppScope
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.SingleInstanceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@ContributesMultibinding(AppScope::class)
@SingleInstanceIn(AppScope::class)
class InitMessageHandlerPlugin @Inject constructor(
    @AppCoroutineScope val appCoroutineScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider
) : MessageHandlerPlugin {

    private val moshi = Moshi.Builder().add(JSONObjectAdapter()).build()
    private lateinit var rules: String

    override fun process(messageType: String, jsonString: String, webView: WebView) {
        if (messageType == type) {
            appCoroutineScope.launch(dispatcherProvider.main()) {
                try {
                    Timber.d("MARCOS receiving init and responding")
                    val message: InitMessage = parseMessage(jsonString) ?: return@launch
                    val url = message.url
                    val uri = url.toUri()

                    if (!uri.isHttp && !uri.isHttps) {
                        Timber.d("MARCOS ignoring special URL scheme")
                        return@launch
                    }

                    val isAutoconsentEnabled = true

                    if (isAutoconsentEnabled == false) { // ToDo is autoconsent is not enabled
                        return@launch
                    }

                    if (true == false) { // ToDo is autoconsent disabled for this site
                        return@launch
                    }

                    val disabledCmps = emptyList<String>()
                    val autoAction = "optOut"
                    val enablePreHide = true
                    val detectRetries = 1

                    val config = Config(isAutoconsentEnabled, autoAction, disabledCmps, enablePreHide, detectRetries)
                    val initResp = InitResp(rules = getRules(), config = config)

                    val response = ReplyHandler.constructReply(getMessage(initResp))

                    Timber.d("MARCOS message is $response")
                    webView.evaluateJavascript("javascript:$response", null)
                } catch (e: Exception) {
                    Timber.d("MARCOS exception is ${e.localizedMessage}")
                }
            }
        }
    }

    override val type: String = "init"

    private fun parseMessage(jsonString: String): InitMessage? {
        val jsonAdapter: JsonAdapter<InitMessage> = moshi.adapter(InitMessage::class.java)
        return jsonAdapter.fromJson(jsonString)
    }

    private fun getMessage(initResp: InitResp): String {
        val jsonAdapter: JsonAdapter<InitResp> = moshi.adapter(InitResp::class.java)
        return jsonAdapter.toJson(initResp).toString()
    }

    private fun getRules(): JSONObject {
        if (!this::rules.isInitialized) {
            rules = JsReader.loadJs("rules.json")
        }
        return JSONObject(rules)
    }

    data class InitMessage(val type: String, val url: String)

    data class Config(
        val enabled: Boolean,
        val autoAction: String?,
        val disabledCmps: List<String>,
        val enablePrehide: Boolean,
        val detectRetries: Int
    )

    // rules can actually be null but we will always pass them through
    data class InitResp(val type: String = "initResp", val config: Config, val rules: JSONObject)
}