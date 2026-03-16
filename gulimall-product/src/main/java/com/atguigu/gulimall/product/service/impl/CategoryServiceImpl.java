package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

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
//    @CacheEvict(value = {"category"},key ="'getLevel1Categorys'")//删除指定key值的缓存
    @CacheEvict(value = "category",allEntries = true)
//    @Caching(evict = {
//            @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
//            @CacheEvict(value = "category",key = "'getCatalogJson'")
//    })
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }
    /**
     * 查询所有一级分类
     * @return
     */
    @Cacheable(value = "category",key = "#root.method.name",sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid",0));
    }

    @Cacheable(value = "category",key = "#root.method.name")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        //优化,将多次查询改为一次查询
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查出所有分类
        //2.查出所有一级分类
        //List<CategoryEntity> level1Categorys = getLevel1Categorys();
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //3.封装数据
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(
                key -> key.getCatId().toString(),
                value -> {
                    //2.封装二级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, value.getCatId());
                    List<Catalog2Vo> catalog2Vos = null;
                    if (categoryEntities != null && categoryEntities.size() > 0) {
                        catalog2Vos = categoryEntities.stream().map(level2 -> {
                            Catalog2Vo catalog2Vo = new Catalog2Vo(value.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList, level2.getCatId());
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

    /**
     * redis查出所有分类
     * @return
     */
    //TODO 产生堆外内存溢出(lettuce的bug)

    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        // 1. 加入缓存逻辑
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        Map<String, List<Catalog2Vo>> result = null;

        if (StringUtils.isEmpty(catelogJSON)) {
            // 2. 缓存中没有，查询数据库
            result = getCatalogJsonFromDb(); // 直接赋值给result
//            // 3. 查到的数据再放入缓存，将对象转为json放在缓存中
//            String s = JSON.toJSONString(result);
//            redisTemplate.opsForValue().set("catelogJSON", s, 1, TimeUnit.DAYS);
        } else {
            // 缓存命中，解析JSON为对象
            result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {});
        }

        return result;
    }
    /**
     * 从数据库查出所有分类
     * @return
     */

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDb() {
        //分布式锁
        RLock lock = redissonClient.getLock("catelogJson-lock");
        lock.lock(); // 加锁
        // 加锁成功
        Map<String, List<Catalog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;

        /*//分布式锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);

        if (lock) {
            // 加锁成功
            Map<String, List<Catalog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            //业务成功解锁
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if (lockValue != null && lockValue.equals(uuid)) {
//                redisTemplate.delete("lock");
//            }
            //lua脚本实现原子性删除
            return dataFromDb;
        }else {
            //加锁失败，重试
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return getCatalogJsonFromDb(); // 重试
        }*/

        /*//加锁解决缓存击穿
        //在分布式情况下需要考虑分布式锁
        synchronized (this) {
            //再去缓存查询一次
            String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
            if (StringUtils.isEmpty(catelogJSON)){
                Map<String, List<Catalog2Vo>> result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
                });
                return result;
            }
            //优化,将多次查询改为一次查询
            List<CategoryEntity> selectList = baseMapper.selectList(null);

            //1.查出所有分类
            //2.查出所有一级分类
            //List<CategoryEntity> level1Categorys = getLevel1Categorys();
            List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
            //3.封装数据
            Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(
                    key -> key.getCatId().toString(),
                    value -> {
                        //2.封装二级分类
                        List<CategoryEntity> categoryEntities = getParent_cid(selectList, value.getCatId());
                        List<Catalog2Vo> catalog2Vos = null;
                        if (categoryEntities != null && categoryEntities.size() > 0) {
                            catalog2Vos = categoryEntities.stream().map(level2 -> {
                                Catalog2Vo catalog2Vo = new Catalog2Vo(value.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                                List<CategoryEntity> level3Catelog = getParent_cid(selectList, level2.getCatId());
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
            String s = JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catelogJSON",s,1, TimeUnit.DAYS);
            return parent_cid;
        }*/
    }

    private Map<String, List<Catalog2Vo>> getDataFromDb() {
        //再去缓存查询一次
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        if (!StringUtils.isEmpty(catelogJSON)){
            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
            return result;
        }
        //优化,将多次查询改为一次查询
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查出所有分类
        //2.查出所有一级分类
        //List<CategoryEntity> level1Categorys = getLevel1Categorys();
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //3.封装数据
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(
                key -> key.getCatId().toString(),
                value -> {
                    //2.封装二级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, value.getCatId());
                    List<Catalog2Vo> catalog2Vos = null;
                    if (categoryEntities != null && categoryEntities.size() > 0) {
                        catalog2Vos = categoryEntities.stream().map(level2 -> {
                            Catalog2Vo catalog2Vo = new Catalog2Vo(value.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList, level2.getCatId());
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
        //3.查到的数据再放入缓存,将对象转为json放在缓存中
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catelogJSON",s,1, TimeUnit.DAYS);
        return parent_cid;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parent_cid) {
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", value.getCatId()));
        return selectList.stream().filter(item -> item.getParentCid().equals(parent_cid)).collect(Collectors.toList());
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