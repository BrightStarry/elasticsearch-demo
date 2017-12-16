package com.zx.elasticsearch;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@SpringBootApplication
public class ElasticsearchDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchDemoApplication.class, args);
    }


    //es连接
    @Autowired
    private TransportClient client;

    /**
     * 查询es
     */
    @GetMapping("/get/book/novel")
    public ResponseEntity get(String id) {
        if (StringUtils.isEmpty(id)) {
            return ResponseEntity.notFound().build();
        }
        GetResponse result = client.prepareGet("book", "man", id).get();

        //没找到,返回404
        if (!result.isExists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(result.getSource());
    }

    /**
     * 新增
     */
    @PostMapping("/add/book/novel")
    public ResponseEntity add(String title, String author,
                              Integer wordCount, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate) {
        try {
            //构造json
            XContentBuilder content = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title", title)
                    .field("author", author)
                    .field("word_count", wordCount)
                    .field("publish_date", publishDate.getTime())
                    .endObject();
            //增加并接收返回结果
            IndexResponse result = client.prepareIndex("book", "man")
                    .setSource(content)
                    .get();
            return ResponseEntity.ok().body(result.getId());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/book/novel")
    public String delete(String id) {
        DeleteResponse result = client.prepareDelete("book", "man", id).get();
        return result.getResult().toString();
    }

    /**
     * 更新
     */
    @PutMapping("/update/book/novel")
    public String update(
            String id,
            @RequestParam(required = false) String title
    ) throws IOException, ExecutionException, InterruptedException {


        //构造json
        XContentBuilder content = XContentFactory.jsonBuilder()
                .startObject()
                .field("title", title)
                .endObject();
        //更新请求
        UpdateRequest updateRequest = new UpdateRequest("book", "man", id);
        updateRequest.doc(content);


        UpdateResponse result = client.update(updateRequest).get();
        return result.getResult().toString();
    }

    /**
     * 复合查询
     */
    @PostMapping("/query/book/novel")
    public List<Map<String, Object>> query(String title,String author,Integer gtWordCount,Integer ltWordCount) {
        //构造布尔查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchQuery("author", author));
        boolQuery.must(QueryBuilders.matchQuery("title", title));

        //构造范围查询
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("word_count")
                .from(gtWordCount).to(ltWordCount);
        //将两个查询用filter结合
        boolQuery.filter(rangeQuery);

        //请求体
        SearchRequestBuilder builder = client.prepareSearch("book")
                .setTypes("man")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(boolQuery)
                .setFrom(0)
                .setSize(10);
        //输出请求体
        System.out.println(builder);

        //请求并获取响应
        SearchResponse response = builder.get();

        List<Map<String, Object>> result = new ArrayList<>();
        for (SearchHit item : response.getHits()) {
            result.add(item.getSourceAsMap());
        }

        return result;
    }
}
