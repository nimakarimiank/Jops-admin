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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.config.properties.FileProperties;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.security.service.OnlineUserService;
import me.zhengjie.modules.security.service.UserCacheManager;
import me.zhengjie.modules.system.domain.Job;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.exception.EntityExistException;
import me.zhengjie.modules.system.domain.dto.UserQueryCriteria;
import me.zhengjie.modules.system.mapper.UserJobMapper;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.modules.system.mapper.UserRoleMapper;
import me.zhengjie.modules.system.service.UserService;
import me.zhengjie.utils.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Zheng Jie
 * @date 2018-11-23
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final UserJobMapper userJobMapper;
    private final UserRoleMapper userRoleMapper;
    private final FileProperties properties;
    private final RedisUtils redisUtils;
    private final UserCacheManager userCacheManager;
    private final OnlineUserService onlineUserService;

    @Override
    public PageResult<User> queryAll(UserQueryCriteria criteria, Page<Object> page) {
        criteria.setOffset(page.offset());
        List<User> users = userMapper.findAll(criteria);
        Long total = userMapper.countAll(criteria);
        return PageUtil.toPage(users, total);
    }

    @Override
    public List<User> queryAll(UserQueryCriteria criteria) {
        return userMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User findById(long id) {
        String key = CacheKey.USER_ID + id;
        User user = redisUtils.get(key, User.class);
        if (user == null) {
            user = getById(id);
            redisUtils.set(key, user, 1, TimeUnit.DAYS);
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(User resources) {
        resources.setDeptId(resources.getDept().getId());
        if (userMapper.findByUsername(resources.getUsername()) != null) {
            throw new EntityExistException(User.class, "username", resources.getUsername());
        }
        if (userMapper.findByEmail(resources.getEmail()) != null) {
            throw new EntityExistException(User.class, "email", resources.getEmail());
        }
        if (userMapper.findByPhone(resources.getPhone()) != null) {
            throw new EntityExistException(User.class, "phone", resources.getPhone());
        }
        resources.setIsAdmin(false);
        save(resources);
        // Save user jobs
        userJobMapper.insertData(resources.getId(), resources.getJobs());
        // Save user roles
        userRoleMapper.insertData(resources.getId(), resources.getRoles());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(User resources) throws Exception {
        User user = getById(resources.getId());
        User user1 = userMapper.findByUsername(resources.getUsername());
        User user2 = userMapper.findByEmail(resources.getEmail());
        User user3 = userMapper.findByPhone(resources.getPhone());
        if (user1 != null && !user.getId().equals(user1.getId())) {
            throw new EntityExistException(User.class, "username", resources.getUsername());
        }
        if (user2 != null && !user.getId().equals(user2.getId())) {
            throw new EntityExistException(User.class, "email", resources.getEmail());
        }
        if (user3 != null && !user.getId().equals(user3.getId())) {
            throw new EntityExistException(User.class, "phone", resources.getPhone());
        }
        // If user roles changed
        if (!resources.getRoles().equals(user.getRoles())) {
            redisUtils.del(CacheKey.DATA_USER + resources.getId());
            redisUtils.del(CacheKey.MENU_USER + resources.getId());
            redisUtils.del(CacheKey.ROLE_AUTH + resources.getId());
            redisUtils.del(CacheKey.ROLE_USER + resources.getId());
        }
        // Modifying department affects data scope
        if (!Objects.equals(resources.getDept(),user.getDept())) {
            redisUtils.del(CacheKey.DATA_USER + resources.getId());
        }
        // If user is disabled, clear user login info
        if(!resources.getEnabled()){
            onlineUserService.kickOutForUsername(resources.getUsername());
        }
        user.setDeptId(resources.getDept().getId());
        user.setUsername(resources.getUsername());
        user.setEmail(resources.getEmail());
        user.setEnabled(resources.getEnabled());
        user.setRoles(resources.getRoles());
        user.setDept(resources.getDept());
        user.setJobs(resources.getJobs());
        user.setPhone(resources.getPhone());
        user.setNickName(resources.getNickName());
        user.setGender(resources.getGender());
        saveOrUpdate(user);
        // Clear cache
        delCaches(user.getId(), user.getUsername());
        // Update user jobs
        userJobMapper.deleteByUserId(resources.getId());
        userJobMapper.insertData(resources.getId(), resources.getJobs());
        // Update user roles
        userRoleMapper.deleteByUserId(resources.getId());
        userRoleMapper.insertData(resources.getId(), resources.getRoles());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCenter(User resources) {
        User user = getById(resources.getId());
        User user1 = userMapper.findByPhone(resources.getPhone());
        if (user1 != null && !user.getId().equals(user1.getId())) {
            throw new EntityExistException(User.class, "phone", resources.getPhone());
        }
        user.setNickName(resources.getNickName());
        user.setPhone(resources.getPhone());
        user.setGender(resources.getGender());
        saveOrUpdate(user);
        // Clear cache
        delCaches(user.getId(), user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        for (Long id : ids) {
            // Clear cache
            User user = getById(id);
            delCaches(user.getId(), user.getUsername());
        }
        userMapper.deleteBatchIds(ids);
        // Delete user jobs
        userJobMapper.deleteByUserIds(ids);
        // Delete user roles
        userRoleMapper.deleteByUserIds(ids);
    }

    @Override
    public User findByName(String userName) {
        return userMapper.findByUsername(userName);
    }

    @Override
    public User getLoginData(String userName) {
        return userMapper.findByUsername(userName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePass(String username, String pass) {
        userMapper.updatePass(username, pass, new Date());
        flushCache(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(Set<Long> ids, String pwd) {
        List<User> users = userMapper.selectBatchIds(ids);
        // Clear cache
        users.forEach(user -> {
            // Clear cache
            flushCache(user.getUsername());
            // Force logout
            onlineUserService.kickOutForUsername(user.getUsername());
        });
        // Reset password
        userMapper.resetPwd(ids, pwd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> updateAvatar(MultipartFile multipartFile) {
        // File size validation
        FileUtil.checkSize(properties.getAvatarMaxSize(), multipartFile.getSize());
        // Validate uploaded file format
        String image = "gif jpg png jpeg";
        String fileType = FileUtil.getExtensionName(multipartFile.getOriginalFilename());
        if(fileType != null && !image.contains(fileType)){
            throw new BadRequestException("File format error! Only " + image + " format is supported");
        }
        User user = userMapper.findByUsername(SecurityUtils.getCurrentUsername());
        String oldPath = user.getAvatarPath();
        File file = FileUtil.upload(multipartFile, properties.getPath().getAvatar());
        user.setAvatarPath(Objects.requireNonNull(file).getPath());
        user.setAvatarName(file.getName());
        saveOrUpdate(user);
        if (StringUtils.isNotBlank(oldPath)) {
            FileUtil.del(oldPath);
        }
        @NotBlank String username = user.getUsername();
        flushCache(username);
        return new HashMap<String, String>(1) {{
            put("avatar", file.getName());
        }};
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(String username, String email) {
        userMapper.updateEmail(username, email);
        flushCache(username);
    }

    @Override
    public void download(List<User> users, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (User user : users) {
            List<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Username", user.getUsername());
            map.put("Roles", roles);
            map.put("Department", user.getDept().getName());
            map.put("Jobs", user.getJobs().stream().map(Job::getName).collect(Collectors.toList()));
            map.put("Email", user.getEmail());
            map.put("Status", user.getEnabled() ? "Enabled" : "Disabled");
            map.put("Phone Number", user.getPhone());
            map.put("Password Reset Time", user.getPwdResetTime());
            map.put("Create Date", user.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    /**
     * Clear cache
     *
     * @param id /
     */
    public void delCaches(Long id, String username) {
        redisUtils.del(CacheKey.USER_ID + id);
        flushCache(username);
    }

    /**
     * Clear user cache during login
     *
     * @param username /
     */
    private void flushCache(String username) {
        userCacheManager.cleanUserCache(username);
    }
}
