package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    /**
     * 检索的所有参数
     * @param param
     * @return
     */
    SearchResult search(SearchParam param);
}
