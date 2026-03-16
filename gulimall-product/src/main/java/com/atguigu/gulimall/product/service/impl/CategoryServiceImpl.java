package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //组装成树形结构
        //1.找到所有的一级分类.
        List<CategoryEntity> level1Menus = entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((menu) -> {
                    menu.setChildren(getChildrens(menu,entities));
                    return menu;
                })
                .sorted((menu1,menu2) -> { return (menu1.getSort() == null ? 0 : menu1.getSort())
                        - (menu2.getSort() == null ? 0:menu2.getSort());})
                .collect(Collectors.toList());

        return level1Menus;




    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单，是否被其他地方引用
        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        CategoryEntity byId = this.getById(catelogId);
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);

    }
    /**
     * 级联更新
     * @param category
     */


    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }
    /**
     * 查询所有一级分类
     * @return
     */
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid",0));
    }

    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        //1.查出所有分类
        //2.查出所有一级分类
        List<CategoryEntity> level1Categorys = getLevel1Categorys();
        //3.封装数据
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(
                key -> key.getCatId().toString(),
                value -> {
                    //2.封装二级分类
                    List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", value.getCatId()));
                    List<Catalog2Vo> catalog2Vos = null;
                    if (categoryEntities != null && categoryEntities.size() > 0) {
                        catalog2Vos = categoryEntities.stream().map(level2 -> {
                            Catalog2Vo catalog2Vo = new Catalog2Vo(value.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                            List<CategoryEntity> level3Catelog = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", level2.getCatId()));
                            if (level3Catelog != null && level3Catelog.size() > 0){
                                List<Catalog2Vo.Catalog3Vo> catalog3Vos = level3Catelog.stream().map(level3 -> {
                                    Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                                    return catalog3Vo;
                                }).collect(Collectors.toList());
                                catalog2Vo.setCatalog3List(catalog3Vos);

                            }
                            return catalog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catalog2Vos;
                }
        ));
        return parent_cid;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths){
        //收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0){
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    //递归查找子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all){
        List<CategoryEntity> collect = all.stream().filter(categoryEntity -> categoryEntity.getParentCid() == root.getCatId())
                .map(categoryEntity -> {
                     categoryEntity.setChildren(getChildrens(categoryEntity, all));
                     return categoryEntity;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort())
                            - (menu2.getSort() == null ? 0:menu2.getSort());
                })
                .collect(Collectors.toList());

        return collect;
    }

}