package com.songyuankun.wechat.blog.controller;

import com.songyuankun.wechat.blog.event.NumberEventProducer;
import com.songyuankun.wechat.blog.application.ArticleApplicationService;
import com.songyuankun.wechat.cache.ArticleCache;
import com.songyuankun.wechat.common.Response;
import com.songyuankun.wechat.common.ResponseUtils;
import com.songyuankun.wechat.entity.ArticlePO;
import com.songyuankun.wechat.response.ArticlePOInfoResponse;
import com.songyuankun.wechat.service.TagServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

/**
 * @author songyuankun
 */

@RestController
@RequestMapping("blog/article")
@Slf4j
public class ArticleBlogController {
    private final ArticleApplicationService articleService;
    private final TagServiceImpl tagService;
    private final NumberEventProducer numberEventProducer;
    private final ArticleCache articleCache;

    public ArticleBlogController(ArticleApplicationService articleService, TagServiceImpl tagService, NumberEventProducer numberEventProducer, ArticleCache articleCache) {
        this.articleService = articleService;
        this.tagService = tagService;
        this.numberEventProducer = numberEventProducer;
        this.articleCache = articleCache;
    }


    @GetMapping("info/{id}")
    public Response<ArticlePOInfoResponse> info(@PathVariable Integer id) {
        ArticlePO one = articleService.getOne(id);
        if (one != null) {
            //读写分离处理
            numberEventProducer.onData("READ", id);
            ArticlePOInfoResponse articleInfoResponse = new ArticlePOInfoResponse();
            BeanUtils.copyProperties(one, articleInfoResponse);
            articleInfoResponse.setTagList(tagService.getTagsByArticleId(id));
            articleCache.incrementReadCount(id);
            return ResponseUtils.success(articleInfoResponse);
        } else {
            return ResponseUtils.notFound();
        }
    }


    @GetMapping("page")
    public Response<Page<ArticlePO>> publicPage(
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Boolean recommend,
            @RequestParam(required = false) Boolean latest,
            @RequestParam(required = false) Boolean favorite
    ) {
        ArticlePO articlePO = new ArticlePO();
        articlePO.setPublish(true);
        if (recommend != null && recommend) {
            articlePO.setRecommend(true);
        }
        Sort sort = Sort.by(Sort.Order.desc("top"));
        if (latest != null && latest) {
            Sort.Order createTime = Sort.Order.desc("createTime");
            sort = sort.and(Sort.by(createTime));
        }
        if (favorite != null && favorite) {
            Sort.Order likeNum = Sort.Order.desc("likeNum");
            sort = sort.and(Sort.by(likeNum));
        }
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);
        Page<ArticlePO> all = articleService.findAll(Example.of(articlePO), pageable);
        return ResponseUtils.success(all);
    }


    @GetMapping("hotReads")
    public Response<Page<ArticlePO>> hotReads() {
        return ResponseUtils.success(articleService.hotReads());
    }


    @PutMapping("like/{id}")
    public Response<Object> likeArticle(@PathVariable Integer id) {
        //读写分离处理
        numberEventProducer.onData("LIKE", id);
        return ResponseUtils.success();
    }

}
