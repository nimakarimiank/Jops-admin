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
package me.zhengjie.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

/**
 * Log query criteria
 * @author Zheng Jie
 * @date 2019-6-4 09:23:07
 */
@Data
public class SysLogQueryCriteria {

    @ApiModelProperty(value = "Fuzzy query")
    private String blurry;

    @ApiModelProperty(value = "Username")
    private String username;

    @ApiModelProperty(value = "Log type")
    private String logType;

    @ApiModelProperty(value = "Creation time")
    private List<Timestamp> createTime;

    @ApiModelProperty(value = "Page number", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "Page size", example = "10")
    private Integer size = 10;
}
