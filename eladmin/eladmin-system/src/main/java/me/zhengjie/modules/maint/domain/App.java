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
package me.zhengjie.modules.maint.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import me.zhengjie.base.BaseEntity;
import java.io.Serializable;

/**
* @author zhanghouying
* @date 2019-08-24
*/
@Getter
@Setter
@TableName("mnt_app")
public class App extends BaseEntity implements Serializable {

    
	@TableId(value = "app_id", type = IdType.AUTO)
	@ApiModelProperty(value = "ID", hidden = true)
    private Long id;

	@ApiModelProperty(value = "Name")
    private String name;

	@ApiModelProperty(value = "Port")
	private int port;

	@ApiModelProperty(value = "Upload path")
	private String uploadPath;

	@ApiModelProperty(value = "Deploy path")
	private String deployPath;

	@ApiModelProperty(value = "Backup path")
	private String backupPath;

	@ApiModelProperty(value = "Start script")
	private String startScript;

	@ApiModelProperty(value = "Deploy script")
	private String deployScript;

    public void copy(App source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
