package phanes.replay.job.opensearch.theme;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import phanes.replay.job.opensearch.theme.domain.ThemeDoc;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchWriter implements ItemWriter<ThemeDoc> {

    private final OpenSearchClient client;
    private static final String THEME_INDEX = "replay-theme-write";

    @Override
    public void write(@NonNull Chunk<? extends ThemeDoc> chunk) throws IOException {
        if (chunk.isEmpty()) return;
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (ThemeDoc doc : chunk.getItems()) {
            bulkBuilder.operations(op -> op
                    .update(idx -> idx
                            .index(THEME_INDEX)
                            .id(String.valueOf(doc.getId()))
                            .document(doc)
                            .docAsUpsert(true)
                    )
            );
            log.info("theme doc={}", doc);
        }
        BulkResponse response = client.bulk(bulkBuilder.build());
        if (response.errors()) {
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    log.error("[OpenSearch BulkError] id={} type={} reason={} json={}", item.id(), item.error().type(), item.error().reason(), item.toJsonString());
                }
            }
            throw new RuntimeException("OpenSearch Bulk operation failed");
        }
        log.info("Indexed {} documents successfully to [{}]", chunk.size(), THEME_INDEX);
    }
}