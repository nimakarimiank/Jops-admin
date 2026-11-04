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

import cn.hutool.extra.mail.Mail;
import cn.hutool.extra.mail.MailAccount;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.domain.EmailConfig;
import me.zhengjie.domain.dto.EmailDto;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.mapper.EmailConfigMapper;
import me.zhengjie.service.EmailService;
import me.zhengjie.utils.EncryptUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zheng Jie
 * @date 2018-12-26
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "email")
public class EmailServiceImpl extends ServiceImpl<EmailConfigMapper, EmailConfig> implements EmailService {

    @Override
    @CachePut(key = "'config'")
    @Transactional(rollbackFor = Exception.class)
    public EmailConfig config(EmailConfig emailConfig, EmailConfig old) throws Exception {
        emailConfig.setId(1L);
        if(!emailConfig.getPass().equals(old.getPass())){
            // Symmetric encryption
            emailConfig.setPass(EncryptUtils.desEncrypt(emailConfig.getPass()));
        }
        saveOrUpdate(emailConfig);
        return emailConfig;
    }

    @Override
    @Cacheable(key = "'config'")
    public EmailConfig find() {
        EmailConfig emailConfig = getById(1L);
        return emailConfig == null ? new EmailConfig() : emailConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(EmailDto emailDto, EmailConfig emailConfig){
        if(emailConfig.getId() == null){
            throw new BadRequestException("Please configure before proceeding");
        }
        // Encapsulate
        MailAccount account = new MailAccount();
        // Set user
        String user = emailConfig.getFromUser().split("@")[0];
        account.setUser(user);
        account.setHost(emailConfig.getHost());
        account.setPort(Integer.parseInt(emailConfig.getPort()));
        account.setAuth(true);
        try {
            // Symmetric decryption
            account.setPass(EncryptUtils.desDecrypt(emailConfig.getPass()));
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        account.setFrom(emailConfig.getUser()+"<"+emailConfig.getFromUser()+">");
        // Send via SSL
        account.setSslEnable(true);
        // Use STARTTLS secure connection
        account.setStarttlsEnable(true);
        // Fix issue where JDK disables certain TLS protocols by default after JDK 8, causing email sending failures
        account.setSslProtocols("TLSv1 TLSv1.1 TLSv1.2");
        String content = emailDto.getContent();
        // Send
        try {
            int size = emailDto.getTos().size();
            Mail.create(account)
                    .setTos(emailDto.getTos().toArray(new String[size]))
                    .setTitle(emailDto.getSubject())
                    .setContent(content)
                    .setHtml(true)
                    // Close session
                    .setUseGlobalSession(false)
                    .send();
        }catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }
}
