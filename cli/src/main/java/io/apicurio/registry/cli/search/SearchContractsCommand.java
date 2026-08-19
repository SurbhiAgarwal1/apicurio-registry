/*
 * Copyright 2026 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.cli.search;

import io.apicurio.registry.cli.common.AbstractCommand;
import io.apicurio.registry.cli.common.ColumnsMixin;
import io.apicurio.registry.cli.common.OutputTypeMixin;
import io.apicurio.registry.cli.common.PaginationMixin;
import io.apicurio.registry.cli.utils.OutputBuffer;
import io.apicurio.registry.rest.client.search.contracts.ContractsRequestBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import static io.apicurio.registry.cli.utils.Conversions.convert;

@Command(
        name = "contract",
        aliases = {"contracts"},
        description = "Search for data contracts"
)
public class SearchContractsCommand extends AbstractCommand {

    @Option(
            names = {"--status"},
            description = "Filter by contract status (e.g. DRAFT, ACTIVE, DEPRECATED, RETIRED)."
    )
    private String status;

    @Option(
            names = {"--owner-team"},
            description = "Filter by owner team (exact match)."
    )
    private String ownerTeam;

    @Option(
            names = {"--compatibility-group"},
            description = "Filter by compatibility group (exact match)."
    )
    private String compatibilityGroup;

    @Mixin
    private PaginationMixin pagination;

    @Mixin
    private OutputTypeMixin outputType;

    @Mixin
    private ColumnsMixin columns;

    @Override
    public void run(final OutputBuffer output) throws Exception {
        //noinspection ConstantConditions
        final var results = convert(client.getRegistryClient().search().contracts().get(r -> {
            //noinspection ConstantConditions
            applyFilters(r.queryParameters);
        }));
        SearchUtil.printArtifactResults(output, results, outputType, pagination, columns);
    }

    private void applyFilters(final ContractsRequestBuilder.GetQueryParameters params) {
        params.offset = (pagination.getPage() - 1) * pagination.getSize();
        params.limit = pagination.getSize();
        if (status != null) {
            params.status = status;
        }
        if (ownerTeam != null) {
            params.ownerTeam = ownerTeam;
        }
        if (compatibilityGroup != null) {
            params.compatibilityGroup = compatibilityGroup;
        }
    }
}
