/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.modules.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.system.domain.Dept;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.RoleMapper;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.modules.system.domain.dto.DeptQueryCriteria;
import me.zhengjie.utils.*;
import me.zhengjie.modules.system.mapper.DeptMapper;
import me.zhengjie.modules.system.service.DeptService;
import me.zhengjie.utils.enums.DataScopeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author Zheng Jie
* @date 2019-03-25
*/
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;
    private final RedisUtils redisUtils;
    private final RoleMapper roleMapper;

    @Override
    public List<Dept> queryAll(DeptQueryCriteria criteria, Boolean isQuery) throws Exception {
        String dataScopeType = SecurityUtils.getDataScopeType();
        if (isQuery) {
            if(dataScopeType.equals(DataScopeEnum.ALL.getValue())){
                criteria.setPidIsNull(true);
            }
            List<Field> fields = StringUtils.getAllFields(criteria.getClass(), new ArrayList<>());
            List<String> fieldNames = new ArrayList<String>(){{ add("pidIsNull");add("enabled");}};
            for (Field field : fields) {
                // Set accessible permission on object to allow access to private properties
                field.setAccessible(true);
                Object val = field.get(criteria);
                if(fieldNames.contains(field.getName())){
                    continue;
                }
                if (ObjectUtil.isNotNull(val)) {
                    criteria.setPidIsNull(null);
                    break;
                }
            }
        }
        // Data scope
        criteria.setIds(SecurityUtils.getCurrentUserDataScope());
        List<Dept> list = deptMapper.findAll(criteria);
        // If empty, indicates custom or this-level data scope, deduplication required
        if(StringUtils.isBlank(dataScopeType)){
            return deduplication(list);
        }
        return list;
    }

    @Override
    public Dept findById(Long id) {
        String key = CacheKey.DEPT_ID + id;
        Dept dept = redisUtils.get(key, Dept.class);
        if(dept == null){
            dept = deptMapper.selectById(id);
            redisUtils.set(key, dept, 1, TimeUnit.DAYS);
        }
        return dept;
    }

    @Override
    public List<Dept> findByPid(long pid) {
        return deptMapper.findByPid(pid);
    }

    @Override
    public Set<Dept> findByRoleId(Long id) {
        return deptMapper.findByRoleId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Dept resources) {
        save(resources);
        // Clear cache
        updateSubCnt(resources.getPid());
        // Clear datascope cache for custom role permissions
        delCaches(resources.getPid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Dept resources) {
        // Old department
        Long oldPid = findById(resources.getId()).getPid();
        Long newPid = resources.getPid();
        if(resources.getPid() != null && resources.getId().equals(resources.getPid())) {
            throw new BadRequestException("The parent department cannot be itself");
        }
        Dept dept = getById(resources.getId());
        resources.setId(dept.getId());
        saveOrUpdate(resources);
        // Update sub node count in parent node
        updateSubCnt(oldPid);
        updateSubCnt(newPid);
        // Clear cache
        delCaches(resources.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Dept> depts) {
        for (Dept dept : depts) {
            // Clear cache
            delCaches(dept.getId());
            deptMapper.deleteById(dept.getId());
            updateSubCnt(dept.getPid());
        }
    }

    @Override
    public void download(List<Dept> depts, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Dept dept : depts) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("Department Name", dept.getName());
            map.put("Department Status", dept.getEnabled() ? "Enabled" : "Disabled");
            map.put("Create Date", dept.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public Set<Dept> getDeleteDepts(List<Dept> menuList, Set<Dept> deptSet) {
        for (Dept dept : menuList) {
            deptSet.add(dept);
            List<Dept> depts = deptMapper.findByPid(dept.getId());
            if(CollUtil.isNotEmpty(depts)){
                getDeleteDepts(depts, deptSet);
            }
        }
        return deptSet;
    }

    @Override
    public List<Long> getDeptChildren(List<Dept> deptList) {
        List<Long> list = new ArrayList<>();
        deptList.forEach(dept -> {
                    if (dept!=null && dept.getEnabled()) {
                        List<Dept> depts = deptMapper.findByPid(dept.getId());
                        if (CollUtil.isNotEmpty(depts)) {
                            list.addAll(getDeptChildren(depts));
                        }
                        list.add(dept.getId());
                    }
                }
        );
        return list;
    }

    @Override
    public List<Dept> getSuperior(Dept dept, List<Dept> depts) {
        if(dept.getPid() == null){
            depts.addAll(deptMapper.findByPidIsNull());
            return depts;
        }
        depts.addAll(deptMapper.findByPid(dept.getPid()));
        return getSuperior(findById(dept.getPid()), depts);
    }

    @Override
    public Object buildTree(List<Dept> deptList) {
        Set<Dept> trees = new LinkedHashSet<>();
        Set<Dept> depts= new LinkedHashSet<>();
        List<String> deptNames = deptList.stream().map(Dept::getName).collect(Collectors.toList());
        boolean isChild;
        for (Dept dept : deptList) {
            isChild = false;
            if (dept.getPid() == null) {
                trees.add(dept);
            }
            for (Dept it : deptList) {
                if (it.getPid() != null && dept.getId().equals(it.getPid())) {
                    isChild = true;
                    if (dept.getChildren() == null) {
                        dept.setChildren(new ArrayList<>());
                    }
                    dept.getChildren().add(it);
                }
            }
            if(isChild) {
                depts.add(dept);
            } else if(dept.getPid() != null &&  !deptNames.contains(findById(dept.getPid()).getName())) {
                depts.add(dept);
            }
        }

        if (CollectionUtil.isEmpty(trees)) {
            trees = depts;
        }
        Map<String,Object> map = new HashMap<>(2);
        map.put("totalElements",depts.size());
        map.put("content",CollectionUtil.isEmpty(trees)? depts :trees);
        return map;
    }

    @Override
    public void verification(Set<Dept> depts) {
        Set<Long> deptIds = depts.stream().map(Dept::getId).collect(Collectors.toSet());
        if(userMapper.countByDepts(deptIds) > 0){
            throw new BadRequestException("The selected department has associated users, please dissociate before proceeding!");
        }
        if(roleMapper.countByDepts(deptIds) > 0){
            throw new BadRequestException("The selected department has associated roles, please dissociate before proceeding!");
        }
    }

    private void updateSubCnt(Long deptId){
        if(deptId != null){
            int count = deptMapper.countByPid(deptId);
            deptMapper.updateSubCntById(count, deptId);
        }
    }

    private List<Dept> deduplication(List<Dept> list) {
        List<Dept> depts = new ArrayList<>();
        for (Dept dept : list) {
            boolean flag = true;
            for (Dept dept1 : list) {
                if (dept1.getId().equals(dept.getPid())) {
                    flag = false;
                    break;
                }
            }
            if (flag){
                depts.add(dept);
            }
        }
        return depts;
    }

    /**
     * Clear cache
     * @param id /
     */
    public void delCaches(Long id){
        List<User> users = userMapper.findByRoleDeptId(id);
        // Delete data scope
        redisUtils.delByKeys(CacheKey.DATA_USER, users.stream().map(User::getId).collect(Collectors.toSet()));
        redisUtils.del(CacheKey.DEPT_ID + id);
    }
}
