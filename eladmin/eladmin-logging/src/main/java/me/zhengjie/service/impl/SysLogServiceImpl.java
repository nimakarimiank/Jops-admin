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
package me.zhengjie.service.impl;

import cn.hutool.core.lang.Dict;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.domain.SysLog;
import me.zhengjie.mapper.SysLogMapper;
import me.zhengjie.service.SysLogService;
import me.zhengjie.domain.dto.SysLogQueryCriteria;
import me.zhengjie.utils.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * @author Zheng Jie
 * @date 2018-11-24
 */
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    private final SysLogMapper sysLogMapper;
    // Define sensitive field constant array
    private static final String[] SENSITIVE_KEYS = {"password"};

    @Override
    public PageResult<SysLog> queryAll(SysLogQueryCriteria criteria, Page<SysLog> page) {
        return PageUtil.toPage(sysLogMapper.queryAll(criteria, page));
    }

    @Override
    public List<SysLog> queryAll(SysLogQueryCriteria criteria) {
        return sysLogMapper.queryAll(criteria);
    }

    @Override
    public PageResult<SysLog> queryAllByUser(SysLogQueryCriteria criteria, Page<SysLog> page) {
        return PageUtil.toPage(sysLogMapper.queryAllByUser(criteria, page));
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String username, String browser, String ip, ProceedingJoinPoint joinPoint, SysLog sysLog) {
        if (sysLog == null) {
            throw new IllegalArgumentException("Log cannot be null!");
        }

        // Get method signature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        me.zhengjie.annotation.Log aopLog = method.getAnnotation(me.zhengjie.annotation.Log.class);

        // Method path
        String methodName = joinPoint.getTarget().getClass().getName() + "." + signature.getName() + "()";

        // Get parameters
        JSONObject params = getParameter(method, joinPoint.getArgs());

        // Populate basic information
        sysLog.setRequestIp(ip);
        sysLog.setAddress(StringUtils.getCityInfo(sysLog.getRequestIp()));
        sysLog.setMethod(methodName);
        sysLog.setUsername(username);
        sysLog.setParams(JSON.toJSONString(params));
        sysLog.setBrowser(browser);
        sysLog.setDescription(aopLog.value());

        // If username was not retrieved, attempt to get it from parameters
        if(StringUtils.isBlank(sysLog.getUsername())){
            sysLog.setUsername(params.getString("username"));
        }

        // Save
        save(sysLog);
    }

    /**
     * Get request parameters based on method and passed arguments
     */
    private JSONObject getParameter(Method method, Object[] args) {
        JSONObject params = new JSONObject();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            // Filter out MultipartFile
            if (args[i] instanceof MultipartFile) {
                continue;
            }
            // Filter out HttpServletResponse
            if (args[i] instanceof HttpServletResponse) {
                continue;
            }
            // Filter out HttpServletRequest
            if (args[i] instanceof HttpServletRequest) {
                continue;
            }
            // Use parameter annotated with @RequestBody as request parameter
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (requestBody != null) {
                Object json = JSON.toJSON(args[i]);
                if (json instanceof JSONArray) {
                    params.put("reqBodyList", json);
                } else {
                    params.putAll((JSONObject) json);
                }
            } else {
                String key = parameters[i].getName();
                params.put(key, args[i]);
            }
        }
        // Iterate through sensitive field array and mask values
        Set<String> keys = params.keySet();
        for (String key : SENSITIVE_KEYS) {
            if (keys.contains(key)) {
                params.put(key, "******");
            }
        }
        // Return parameters
        return params;
    }

    @Override
    public Object findByErrDetail(Long id) {
        String details = sysLogMapper.getExceptionDetails(id);
        return Dict.create().set("exception", details);
    }

    @Override
    public void download(List<SysLog> sysLogs, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysLog sysLog : sysLogs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Username", sysLog.getUsername());
            map.put("IP", sysLog.getRequestIp());
            map.put("IP Source", sysLog.getAddress());
            map.put("Description", sysLog.getDescription());
            map.put("Browser", sysLog.getBrowser());
            map.put("Response Time (ms)", sysLog.getTime());
            map.put("Exception Details", sysLog.getExceptionDetail());
            map.put("Creation Date", sysLog.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delAllByError() {
        // Delete ERROR level logs
        sysLogMapper.deleteByLevel("ERROR");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delAllByInfo() {
        // Delete INFO level logs
        sysLogMapper.deleteByLevel("INFO");
    }
}
