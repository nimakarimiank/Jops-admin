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
package me.zhengjie.modules.system.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

/**
* @author Zheng Jie
* @date 2019-03-25
*/
@Data
public class DeptQueryCriteria{

    @ApiModelProperty(value = "Department ID collection")
    private List<Long> ids;

    @ApiModelProperty(value = "Department name")
    private String name;

    @ApiModelProperty(value = "Is enabled")
    private Boolean enabled;

    @ApiModelProperty(value = "Parent department")
    private Long pid;

    @ApiModelProperty(value = "Query where PID is null")
    private Boolean pidIsNull;

    @ApiModelProperty(value = "Create time")
    private List<Timestamp> createTime;
}