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
package me.zhengjie.modules.quartz.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.quartz.domain.QuartzJob;
import me.zhengjie.modules.quartz.domain.QuartzLog;
import me.zhengjie.modules.quartz.mapper.QuartzJobMapper;
import me.zhengjie.modules.quartz.mapper.QuartzLogMapper;
import me.zhengjie.modules.quartz.service.QuartzJobService;
import me.zhengjie.modules.quartz.domain.dto.QuartzJobQueryCriteria;
import me.zhengjie.modules.quartz.utils.QuartzManage;
import me.zhengjie.utils.*;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author Zheng Jie
 * @date 2019-01-07
 */
@RequiredArgsConstructor
@Service(value = "quartzJobService")
public class QuartzJobServiceImpl extends ServiceImpl<QuartzJobMapper, QuartzJob> implements QuartzJobService {

    private final QuartzJobMapper quartzJobMapper;
    private final QuartzLogMapper quartzLogMapper;
    private final QuartzManage quartzManage;
    private final RedisUtils redisUtils;

    @Override
    public PageResult<QuartzJob> queryAll(QuartzJobQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(quartzJobMapper.findAll(criteria, page));
    }

    @Override
    public PageResult<QuartzLog> queryAllLog(QuartzJobQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(quartzLogMapper.findAll(criteria, page));
    }

    @Override
    public List<QuartzJob> queryAll(QuartzJobQueryCriteria criteria) {
        return quartzJobMapper.findAll(criteria);
    }

    @Override
    public List<QuartzLog> queryAllLog(QuartzJobQueryCriteria criteria) {
        return quartzLogMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(QuartzJob resources) {
        if (!CronExpression.isValidExpression(resources.getCronExpression())){
            throw new BadRequestException("Invalid cron expression format");
        }
        save(resources);
        quartzManage.addJob(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(QuartzJob resources) {
        if (!CronExpression.isValidExpression(resources.getCronExpression())){
            throw new BadRequestException("Invalid cron expression format");
        }
        if(StringUtils.isNotBlank(resources.getSubTask())){
            List<String> tasks = Arrays.asList(resources.getSubTask().split(","));
            if (tasks.contains(resources.getId().toString())) {
                throw new BadRequestException("Cannot add current task ID to subtasks");
            }
        }
        saveOrUpdate(resources);
        quartzManage.updateJobCron(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIsPause(QuartzJob quartzJob) {
        // Toggle pause status
        if (quartzJob.getIsPause()) {
            quartzManage.resumeJob(quartzJob);
            quartzJob.setIsPause(false);
        } else {
            quartzManage.pauseJob(quartzJob);
            quartzJob.setIsPause(true);
        }
        saveOrUpdate(quartzJob);
    }

    @Override
    public void execution(QuartzJob quartzJob) {
        quartzManage.runJobNow(quartzJob);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        for (Long id : ids) {
            QuartzJob quartzJob = getById(id);
            quartzManage.deleteJob(quartzJob);
            removeById(quartzJob);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executionSubJob(String[] tasks) throws InterruptedException {
        for (String id : tasks) {
            if (StrUtil.isBlank(id)) {
                // If subtask ID is cleared manually, empty string issue may occur
                continue;
            }
            QuartzJob quartzJob = getById(Long.parseLong(id));
            // Execute task
            String uuid = IdUtil.simpleUUID();
            quartzJob.setUuid(uuid);
            // Execute task
            execution(quartzJob);
            // Get execution status; if execution fails, stop executing subsequent subtasks
            Boolean result = redisUtils.get(uuid, Boolean.class);
            while (result == null) {
                // Sleep for 5 seconds, then check subtask execution status again
                Thread.sleep(5000);
                result = redisUtils.get(uuid, Boolean.class);
            }
            if(!result){
                redisUtils.del(uuid);
                break;
            }
        }
    }

    @Override
    public void download(List<QuartzJob> quartzJobs, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (QuartzJob quartzJob : quartzJobs) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("Job Name", quartzJob.getJobName());
            map.put("Bean Name", quartzJob.getBeanName());
            map.put("Execution Method", quartzJob.getMethodName());
            map.put("Parameters", quartzJob.getParams());
            map.put("Expression", quartzJob.getCronExpression());
            map.put("Status", quartzJob.getIsPause() ? "Paused" : "Running");
            map.put("Description", quartzJob.getDescription());
            map.put("Creation Date", quartzJob.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public void downloadLog(List<QuartzLog> queryAllLog, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (QuartzLog quartzLog : queryAllLog) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("Job Name", quartzLog.getJobName());
            map.put("Bean Name", quartzLog.getBeanName());
            map.put("Execution Method", quartzLog.getMethodName());
            map.put("Parameters", quartzLog.getParams());
            map.put("Expression", quartzLog.getCronExpression());
            map.put("Exception Details", quartzLog.getExceptionDetail());
            map.put("Time Elapsed (ms)", quartzLog.getTime());
            map.put("Status", quartzLog.getIsSuccess() ? "Success" : "Failure");
            map.put("Creation Date", quartzLog.getCreateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}
