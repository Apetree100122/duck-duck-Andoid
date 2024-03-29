/*
 * Copyright (c) 2018 DuckDuckGo
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

package com.duckduckgo.app.statistics.api

import com.duckduckgo.anvil.annotations.ContributesNonCachingServiceApi
import com.duckduckgo.common.utils.AppUrl
import com.duckduckgo.common.utils.BuildConfig
import com.duckduckgo.di.scopes.AppScope
import io.reactivex.Completable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

@ContributesNonCachingServiceApi(AppScope::class)
interface PixelService {

    @GET("${AppUrl.Url.PIXEL}/t/{pixelName}_android_{formFactor}")
    fun fire(
        @Path("pixelName") pixelName: String,
        @Path("formFactor") formFactor: String,
        @Query(AppUrl.ParamKey.ATB) atb: String,
        @QueryMap additionalQueryParams: Map<String, String> = emptyMap(),
        @QueryMap(encoded = true) encodedQueryParams: Map<String, String> = emptyMap(),
        @Query(AppUrl.ParamKey.DEV_MODE) devMode: Int? = if (BuildConfig.DEBUG) 1 else null,
    ): Completable
}
