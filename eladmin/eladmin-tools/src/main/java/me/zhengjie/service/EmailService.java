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
package me.zhengjie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.domain.dto.EmailDto;
import me.zhengjie.domain.EmailConfig;

/**
 * @author Zheng Jie
 * @date 2018-12-26
 */
public interface EmailService extends IService<EmailConfig> {

    /**
     * Update email configuration
     * @param emailConfig Email configuration
     * @param old /
     * @return /
     * @throws Exception /
     */
    EmailConfig config(EmailConfig emailConfig, EmailConfig old) throws Exception;

    /**
     * Query configuration
     * @return EmailConfig Email configuration
     */
    EmailConfig find();

    /**
     * Send email
     * @param emailDto Email content to send
     * @param emailConfig Email configuration
     */
    void send(EmailDto emailDto, EmailConfig emailConfig);
}
