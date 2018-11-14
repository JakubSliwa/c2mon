package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClientTransport;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Transport-based (check
 * <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/transport-client.html>
 * Elasticsearch Documentation</a> for more details) supported index-related operations manager.
 *
 * @author Justin Lewis Salmon
 * @author Serhiy Boychenko
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="false")
public class IndexManagerTransport implements IndexManager {

    private final List<String> indexCache = new CopyOnWriteArrayList<>();

    private ElasticsearchProperties properties;
    private ElasticsearchClientTransport client;

    /**
     * @param client {@link ElasticsearchClientTransport} client instance.
     */
    @Autowired
    public IndexManagerTransport(ElasticsearchClientTransport client) {
        this.client = client;
        this.properties = client.getProperties();
    }

    @Override
    public boolean create(String indexName, String mapping) {
        synchronized (IndexManagerTransport.class) {
            if (exists(indexName)) {
                return true;
            }

            CreateIndexRequestBuilder builder = client.getClient().admin().indices().prepareCreate(indexName);
            builder.setSettings(Settings.builder()
                    .put("number_of_shards", properties.getShardsPerIndex())
                    .put("number_of_replicas", properties.getReplicasPerShard())
                    .build());

            if (mapping != null) {
                builder.addMapping(TYPE, mapping, XContentType.JSON);
            }

            log.debug("Creating new index with name {}", indexName);
            boolean created;

            try {
                CreateIndexResponse response = builder.get();
                created = response.isAcknowledged();
            } catch (ResourceAlreadyExistsException e) {
                log.debug("Index already exists.", e);
                created = true;
            }

            client.waitForYellowStatus();

            if (created) {
                indexCache.add(indexName);
            }

            return created;
        }
    }

    @Override
    public boolean index(String indexName, String source, String routing) {
        return index(indexName, source, "", routing);
    }

    @Override
    public boolean index(String indexName, String source, String id, String routing) {
        synchronized (IndexManagerTransport.class) {
            IndexRequest indexRequest = new IndexRequest(indexName, TYPE);
            if (id != null && !id.isEmpty()) {
                indexRequest.id(id);
            }
            indexRequest.source(source, XContentType.JSON);
            indexRequest.routing(routing);

            boolean indexed = false;
            try {
                IndexResponse indexResponse = client.getClient().index(indexRequest).get();
                indexed = indexResponse.status().equals(RestStatus.CREATED) || indexResponse.status().equals(RestStatus.OK);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error indexing '#{}' to '{}'.", id, indexName, e);
            }

            client.waitForYellowStatus();

            return indexed;
        }
    }

    @Override
    public boolean exists(String indexName) {
        return exists(indexName, "");
    }

    @Override
    public boolean exists(String indexName, String routing) {
        synchronized (IndexManagerTransport.class) {
            if (indexCache.contains(indexName)) {
                return true;
            }

            client.waitForYellowStatus();

            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.types(TYPE);
            searchRequest.routing(routing);

            boolean exists = false;
            try {
                SearchResponse response = client.getClient().search(searchRequest).get();
                exists = response.status().equals(RestStatus.OK);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error searching index '{}'.", indexName, e);
            }

            if (exists) {
                indexCache.add(indexName);
            }

            return exists;
        }
    }

    @Override
    public boolean update(String indexName, String source, String id) {
        synchronized (IndexManagerTransport.class) {
            UpdateRequest updateRequest = new UpdateRequest(indexName, TYPE, id);
            updateRequest.doc(source, XContentType.JSON);
            updateRequest.routing(id);

            IndexRequest indexRequest = new IndexRequest(indexName, TYPE, id);
            indexRequest.source(source, XContentType.JSON);
            indexRequest.routing(id);

            updateRequest.upsert(indexRequest);

            boolean updated = false;
            try {
                UpdateResponse updateResponse = client.getClient().update(updateRequest).get();
                updated = updateResponse.status().equals(RestStatus.OK);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error updating index '{}'.", indexName, e);
            }

            client.waitForYellowStatus();

            return updated;
        }
    }

    @Override
    public boolean delete(String indexName, String id, String routing) {
        synchronized (IndexManagerTransport.class) {
            DeleteRequest deleteRequest = new DeleteRequest(indexName, TYPE, id);
            deleteRequest.routing(routing);

            boolean deleted = false;
            try {
                DeleteResponse deleteResponse = client.getClient().delete(deleteRequest).get();
                deleted = deleteResponse.status().equals(RestStatus.OK);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error while deleting index", e);
            }

            if (deleted) {
                indexCache.remove(indexName);
            }

            client.waitForYellowStatus();

            return deleted;
        }
    }

    @Override
    public void purgeIndexCache() {
        synchronized (IndexManagerTransport.class) {
            indexCache.clear();
        }
    }
}