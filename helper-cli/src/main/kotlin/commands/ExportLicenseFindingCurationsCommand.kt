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

package org.ossreviewtoolkit.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

import java.io.File

import org.ossreviewtoolkit.helper.common.RepositoryLicenseFindingCurations
import org.ossreviewtoolkit.helper.common.getRepositoryLicenseFindingCurations
import org.ossreviewtoolkit.helper.common.mergeLicenseFindingCurations
import org.ossreviewtoolkit.helper.common.replaceConfig
import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.readValue
import org.ossreviewtoolkit.model.yamlMapper
import org.ossreviewtoolkit.utils.expandTilde
import org.ossreviewtoolkit.utils.safeMkdirs

internal class ExportLicenseFindingCurationsCommand : CliktCommand(
    help = "Export the license finding curations to a file which maps repository URLs to the license finding " +
            "curations for the respective repository."
) {
    private val licenseFindingCurationsFile by option(
        "--license-finding-curations-file",
        help = "The output license finding curations file."
    ).convert { it.expandTilde() }
        .file(mustExist = false, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = false)
        .required()

    private val ortResultFile by option(
        "--ort-result-file",
        help = "The input ORT file from which the license finding curations are to be read."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .required()

    private val repositoryConfigurationFile by option(
        "--repository-configuration-file",
        help = "Override the repository configuration contained in the given input ORT file."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)

    private val updateOnlyExisting by option(
        "--update-only-existing",
        help = "If enabled, only entries are imported for which an entry already exists which differs only in terms " +
                "of its concluded license, comment or reason."
    ).flag()

    override fun run() {
        val localLicenseFindingCurations = ortResultFile
            .readValue<OrtResult>()
            .replaceConfig(repositoryConfigurationFile)
            .getRepositoryLicenseFindingCurations()

        val globalLicenseFindingCurations = if (licenseFindingCurationsFile.isFile) {
            licenseFindingCurationsFile.readValue<RepositoryLicenseFindingCurations>()
        } else {
            mapOf()
        }

        globalLicenseFindingCurations
            .mergeLicenseFindingCurations(localLicenseFindingCurations, updateOnlyExisting = updateOnlyExisting)
            .writeAsYaml(licenseFindingCurationsFile)
    }
}

/**
 * Serialize this [RepositoryLicenseFindingCurations] to the given [targetFile] as YAML.
 */
private fun RepositoryLicenseFindingCurations.writeAsYaml(targetFile: File) {
    targetFile.parentFile.apply { safeMkdirs() }

    yamlMapper.writeValue(
        targetFile,
        mapValues { (_, curations) ->
            curations.sortedBy { it.path.removePrefix("*").removePrefix("*") }
        }.toSortedMap()
    )
}
