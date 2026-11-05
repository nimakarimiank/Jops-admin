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
package me.zhengjie.modules.quartz.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import me.zhengjie.base.BaseEntity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author Zheng Jie
 * @date 2019-01-07
 */
@Getter
@Setter
@TableName("sys_quartz_job")
public class QuartzJob extends BaseEntity implements Serializable {

    public static final String JOB_KEY = "JOB_KEY";

    @TableId(value = "job_id", type = IdType.AUTO)
    @NotNull(groups = {Update.class})
    private Long id;

    @TableField(exist = false)
    @ApiModelProperty(value = "Used as unique identifier for subtasks", hidden = true)
    private String uuid;

    @ApiModelProperty(value = "Job name")
    private String jobName;

    @NotBlank
    @ApiModelProperty(value = "Bean name")
    private String beanName;

    @NotBlank
    @ApiModelProperty(value = "Method name")
    private String methodName;

    @ApiModelProperty(value = "Parameters")
    private String params;

    @NotBlank
    @ApiModelProperty(value = "Cron expression")
    private String cronExpression;

    @ApiModelProperty(value = "Status, pause or run")
    private Boolean isPause = false;

    @ApiModelProperty(value = "Person in charge")
    private String personInCharge;

    @ApiModelProperty(value = "Alarm email")
    private String email;

    @ApiModelProperty(value = "Subtasks")
    private String subTask;

    @ApiModelProperty(value = "Pause after failure")
    private Boolean pauseAfterFailure;

    @NotBlank
    @ApiModelProperty(value = "Remarks")
    private String description;
}