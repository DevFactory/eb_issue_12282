package com.mike;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import com.ibm.json.java.JSONObject;

public class IndexManagerTest extends TestRunner {
	
	@Test
	public void testAddMappings() throws IOException, InterruptedException, ExecutionException {
		Client localClient = ElasticsearchIntegrationTest.client();
		
		BulkRequestBuilder bulkRequest = localClient.prepareBulk();
		
		IndexRequestBuilder indexRequest = localClient.prepareIndex(DEFAULT_INDEX, "logs")
				.setSource("{mykey1:\"value\"}").setOpType(OpType.CREATE);
			
		bulkRequest.add(indexRequest).execute().get();
		
		flushAndRefresh(DEFAULT_INDEX);
		
		IndexRequestBuilder indexRequest2 = localClient.prepareIndex(DEFAULT_INDEX, "logs")
				.setSource("{mykey2:\"value\"}").setOpType(OpType.CREATE);
			
		bulkRequest.add(indexRequest2).execute().get();
		
		flushAndRefresh(DEFAULT_INDEX);
		
		// Get the dynamically created mappings for the custom charts
		GetMappingsRequest getMappingsRequest = new GetMappingsRequest();
        getMappingsRequest.indices(DEFAULT_INDEX).types("logs");
        GetMappingsResponse getMappingsResponse = localClient.admin().indices().getMappings(getMappingsRequest).actionGet();
        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappingsByIndex = getMappingsResponse.getMappings();
        
        // it's a dynamic mapping, so we should find a property named 'mykey1' and 'mykey2'
        int foundCount = 0;
		
        // Loop is necessary due to elasticsearch API, but we're always only searching one index
        for (ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> indexEntry : mappingsByIndex) {
           
        	if (indexEntry.value.isEmpty()) {
                continue;
            }
            
        	// Again, the loop is necessary because of the API, but we're only looping through the custom data mapping
            for (ObjectObjectCursor<String, MappingMetaData> typeEntry : indexEntry.value) {
                JSONObject allMappings = JSONObject.parse(typeEntry.value.source().toString());
                JSONObject currentMapping = (JSONObject)allMappings.get("logs");
                JSONObject mappingProperties = (JSONObject)currentMapping.get("properties");
                
                for (Object mappingKey : mappingProperties.keySet()) {
                	String mappingKeyString = (String)mappingKey;
                	if (mappingKeyString.equals("mykey1") || mappingKeyString.equals("mykey2")) {
                		foundCount++;
                	}
                }
            }
        }
        
        assertEquals("property keys were not found by looking at the mappings even though I called flushAndRefresh()", 2, foundCount);
        
	}
		
}
