/*
 * Copyright (C) 2019 HERE Europe B.V.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.ort.helper.common

import com.here.ort.model.config.PathExclude
import com.here.ort.model.config.RepositoryConfiguration
import com.here.ort.model.config.ScopeExclude
import com.here.ort.model.yamlMapper

import java.io.File

/**
 * Return a copy with sorting applied to all entry types which are to be sorted.
 */
internal fun RepositoryConfiguration.sortEntries(): RepositoryConfiguration =
    sortPathExcludes().sortScopeExcludes()

/**
 * Return a copy with the [PathExclude]s sorted.
 */
internal fun RepositoryConfiguration.sortPathExcludes(): RepositoryConfiguration =
    copy(
        excludes = excludes?.let {
            val paths = it.paths.sortedBy { pathExclude ->
                pathExclude.pattern.removePrefix("*").removePrefix("*")
            }
            it.copy(paths = paths)
        }
    )

/**
 * Return a copy with the [ScopeExclude]s sorted.
 */
internal fun RepositoryConfiguration.sortScopeExcludes(): RepositoryConfiguration =
    copy(
        excludes = excludes?.let {
            val scopes = it.scopes.sortedBy { scopeExclude ->
                scopeExclude.name.toString().removePrefix(".*")
            }
            it.copy(scopes = scopes)
        }
    )

/**
 * Serialize a [RepositoryConfiguration] as YAML to the given target [File].
 */
internal fun RepositoryConfiguration.writeAsYaml(targetFile: File) =
    yamlMapper.writeValue(targetFile, this)