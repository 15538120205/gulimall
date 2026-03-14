package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Test
    public void contextLoads(){
        System.out.println(client);
    }

    /**
     * 存储数据
     * @throws Exception
     */
    @Test
    public void indexData() throws Exception {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
//        indexRequest.source("userName","zhangsan","age",18,"sex","男");
        User user = User.builder().age(18).sex("男").userName("张三").build();

        String s = JSON.toJSONString(user);
        indexRequest.source(s, XContentType.JSON);
        //同步
        //执行操作
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //提取响应数据
        System.out.println(index);
        //异步
    }
    @Data
    @Builder
    private static class User{
        private String userName;
        private Integer age;
        private String sex;
    }
    @Data
    @ToString
    public class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;


    }

    @Test
    void searchData() throws Exception {
        //创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bank");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构造检索条件
        searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
//        searchSourceBuilder.from(0);
//        searchSourceBuilder.size(10);
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(ageAgg);
        searchSourceBuilder.aggregation(balanceAvg);
        searchRequest.source(searchSourceBuilder);
        System.out.println("检索条件"+searchSourceBuilder.toString());

        //执行检索
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //分析响应数据
        System.out.println("88888888888888888888888888888888888888888");

        System.out.println(searchRequest.toString());
        Map map = JSON.parseObject(searchResponse.toString(), Map.class);
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println("account"+account);
        }
        //获取分析数据
        Aggregations aggregations = searchResponse.getAggregations();
        Terms ageAgg1 = aggregations.get("ageAgg");
        ageAgg1.getBuckets().forEach(bucket -> {
            System.out.println("年龄"+bucket.getKeyAsString()+"==>"+bucket.getDocCount());
        });
        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("平均薪资"+balanceAvg1.getValueAsString());


    }

}
