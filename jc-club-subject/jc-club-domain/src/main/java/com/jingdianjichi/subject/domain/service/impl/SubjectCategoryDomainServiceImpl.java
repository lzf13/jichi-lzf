package com.jingdianjichi.subject.domain.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.jingdianjichi.subject.common.enums.IsDeletedFlagEnum;
import com.jingdianjichi.subject.domain.convert.SubjectCategoryConverter;
import com.jingdianjichi.subject.domain.entity.SubjectCategoryBO;
import com.jingdianjichi.subject.domain.entity.SubjectLabelBO;
import com.jingdianjichi.subject.domain.service.SubjectCategoryDomainService;
import com.jingdianjichi.subject.domain.util.CacheUtil;
import com.jingdianjichi.subject.infra.basic.entity.SubjectCategory;
import com.jingdianjichi.subject.infra.basic.entity.SubjectLabel;
import com.jingdianjichi.subject.infra.basic.entity.SubjectMapping;
import com.jingdianjichi.subject.infra.basic.service.SubjectCategoryService;
import com.jingdianjichi.subject.infra.basic.service.SubjectLabelService;
import com.jingdianjichi.subject.infra.basic.service.SubjectMappingService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class SubjectCategoryDomainServiceImpl implements SubjectCategoryDomainService {

    @Resource
    private SubjectCategoryService subjectCategoryService;

    @Resource
    private SubjectMappingService subjectMappingService;

    @Resource
    private SubjectLabelService subjectLabelService;

    @Resource
    private ThreadPoolExecutor labelThreadPool;

    @Resource
    private CacheUtil cacheUtil;

    @Override
    public void add(SubjectCategoryBO subjectCategoryBO) {
        if (log.isInfoEnabled()) {
            log.info("SubjectCategoryController.add.bo:{}", JSON.toJSONString(subjectCategoryBO));
        }
        SubjectCategory subjectCategory = SubjectCategoryConverter.INSTANCE
                .convertBoToCategory(subjectCategoryBO);
        subjectCategory.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        subjectCategoryService.insert(subjectCategory);
    }

    @Override
    public List<SubjectCategoryBO> queryCategory(SubjectCategoryBO subjectCategoryBO) {
        SubjectCategory subjectCategory = SubjectCategoryConverter.INSTANCE
                .convertBoToCategory(subjectCategoryBO);
        subjectCategory.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        List<SubjectCategory> subjectCategoryList = subjectCategoryService.queryCategory(subjectCategory);
        List<SubjectCategoryBO> boList = SubjectCategoryConverter.INSTANCE
                .convertBoToCategory(subjectCategoryList);
        if (log.isInfoEnabled()) {
            log.info("SubjectCategoryController.queryPrimaryCategory.boList:{}",
                    JSON.toJSONString(boList));
        }
        boList.forEach(bo -> {
            Integer subjectCount = subjectCategoryService.querySubjectCount(bo.getId());
            bo.setCount(subjectCount);
        });
        return boList;
    }

    @Override
    public Boolean update(SubjectCategoryBO subjectCategoryBO) {
        SubjectCategory subjectCategory = SubjectCategoryConverter.INSTANCE
                .convertBoToCategory(subjectCategoryBO);
        int count = subjectCategoryService.update(subjectCategory);
        return count > 0;
    }

    @Override
    public Boolean delete(SubjectCategoryBO subjectCategoryBO) {
        SubjectCategory subjectCategory = SubjectCategoryConverter.INSTANCE
                .convertBoToCategory(subjectCategoryBO);
        subjectCategory.setIsDeleted(IsDeletedFlagEnum.DELETED.getCode());
        int count = subjectCategoryService.update(subjectCategory);
        return count > 0;
    }

    @SneakyThrows
    @Override
    public List<SubjectCategoryBO> queryCategoryAndLabel(SubjectCategoryBO subjectCategoryBO) {
        Long id = subjectCategoryBO.getId();
        String cacheKey = "categoryAndLabel." + subjectCategoryBO.getId();
        List<SubjectCategoryBO> subjectCategoryBOS = cacheUtil.getResult(cacheKey,
                SubjectCategoryBO.class, (key) -> getSubjectCategoryBOS(id));
        return subjectCategoryBOS;
    }

    private List<SubjectCategoryBO> getSubjectCategoryBOS(Long categoryId) {
        SubjectCategory subjectCategory = new SubjectCategory();
        subjectCategory.setParentId(categoryId);
        subjectCategory.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        List<SubjectCategory> subjectCategoryList = subjectCategoryService.queryCategory(subjectCategory);
        if (log.isInfoEnabled()) {
            log.info("SubjectCategoryController.queryCategoryAndLabel.subjectCategoryList:{}",
                    JSON.toJSONString(subjectCategoryList));
        }
        // 将subjectCategoryList转换为SubjectCategoryBO列表
        List<SubjectCategoryBO> categoryBOList = SubjectCategoryConverter.INSTANCE.convertBoToCategory(subjectCategoryList);
        // 创建一个HashMap，用于存储分类ID与对应的标签列表的映射关系
        Map<Long, List<SubjectLabelBO>> map = new HashMap<>();
        // 使用CompletableFuture实现并行处理，为每个分类获取标签列表
        List<CompletableFuture<Map<Long, List<SubjectLabelBO>>>> completableFutureList = categoryBOList.stream().map(category ->
                // 使用supplyAsync异步执行getLabelBOList方法，并指定线程池为labelThreadPool
                CompletableFuture.supplyAsync(() -> getLabelBOList(category), labelThreadPool)
        ).collect(Collectors.toList());
        // 遍历所有异步任务的结果，并将结果合并到map中
        completableFutureList.forEach(future -> {
            try {
                // 获取异步任务的结果
                Map<Long, List<SubjectLabelBO>> resultMap = future.get();
                // 如果结果不为空，则将结果合并到map中
                if (!MapUtils.isEmpty(resultMap)) {
                    map.putAll(resultMap);
                }
            } catch (Exception e) {
                // 异常处理，打印堆栈信息
                e.printStackTrace();
            }
        });
        // 为每个分类设置对应的标签列表
        categoryBOList.forEach(categoryBO -> {
            // 检查该分类是否有对应的标签列表
            if (!CollectionUtils.isEmpty(map.get(categoryBO.getId()))) {
                // 设置标签列表
                categoryBO.setLabelBOList(map.get(categoryBO.getId()));
            }
        });
        // 返回处理后的分类列表
        return categoryBOList;
    }

    private Map<Long, List<SubjectLabelBO>> getLabelBOList(SubjectCategoryBO category) {
        if (log.isInfoEnabled()) {
            log.info("getLabelBOList:{}", JSON.toJSONString(category));
        }
        Map<Long, List<SubjectLabelBO>> labelMap = new HashMap<>();
        SubjectMapping subjectMapping = new SubjectMapping();
        subjectMapping.setCategoryId(category.getId());
        List<SubjectMapping> mappingList = subjectMappingService.queryLabelId(subjectMapping);
        if (CollectionUtils.isEmpty(mappingList)) {
            return null;
        }
        List<Long> labelIdList = mappingList.stream().map(SubjectMapping::getLabelId).collect(Collectors.toList());
        List<SubjectLabel> labelList = subjectLabelService.batchQueryById(labelIdList);
        List<SubjectLabelBO> labelBOList = new LinkedList<>();
        labelList.forEach(label -> {
            SubjectLabelBO subjectLabelBO = new SubjectLabelBO();
            subjectLabelBO.setId(label.getId());
            subjectLabelBO.setLabelName(label.getLabelName());
            subjectLabelBO.setCategoryId(label.getCategoryId());
            subjectLabelBO.setSortNum(label.getSortNum());
            labelBOList.add(subjectLabelBO);
        });
        labelMap.put(category.getId(), labelBOList);
        return labelMap;
    }

}
