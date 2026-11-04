<h1 style="text-align: center">ELADMIN Management System</h1>

#### Project Overview
An administrative management system with separated frontend and backend architectures, built on Spring Boot 2.7.18, MyBatis-Plus, JWT, Spring Security, Redis, and Vue.

**Documentation:**  [https://eladmin.vip](https://eladmin.vip)

**Live Demo:**  [https://eladmin.vip/demo](https://eladmin.vip/demo)

**Credentials:** `admin / 123456`

#### Source Code

| github                                 |   gitee  |
|--------------------------------------| --- |
| https://github.com/elunez/eladmin-mp |  https://gitee.com/elunez/eladmin-mp   |

#### Recommended VPS
<a href="https://bwh81.net/aff.php?aff=70876" target="_blank">
<img src="https://eladmin.vip/images/banner/side.jpeg" style="width: 435px;border-radius: 2px;" alt="BandwagonHost">
</a>

Use coupon code: `BWHNCXNVXV` for a 6.81% discount, [View details](https://eladmin.vip/pages/040101/)
#### Key Features
- Built with a modern technology stack supported by rich community resources.
- High-efficiency development with a one-click code generator for both frontend and backend code.
- Data dictionary support for convenient status and enumerated value management.
- API rate limiting support to protect service layers against malicious traffic and spikes.
- Endpoint-level functional and data permission controls with customizable operations.
- Custom permission annotations and anonymous endpoint annotations for rapid access control and whitelisting.
- Encapsulated common frontend components: tabular data fetching, data dictionary selectors, and more.
- Unified exception handling across frontend and backend for standardized error responses without cumbersome checks.
- Online user management, server performance monitoring, and concurrent/single-session login restrictions.
- DevOps management for streamlined application deployment and management on remote servers.

####  System Features
- User Management: Configure user accounts; new accounts default to password 123456.
- Role Management: Assign permissions and menus, with department-based role data scopes.
- Menu Management: Dynamic routing with backend-driven configuration, supporting multi-level nested menus.
- Department Management: Configure organizational hierarchies with tree table visualization.
- Job Management: Configure positions and titles across departments.
- Dictionary Management: Maintain standard enumerated values such as status, gender, etc.
- System Logs: Audit user operational and exception logs to facilitate debugging and troubleshooting.
- SQL Monitoring: Monitor database query performance via Druid; default credentials: admin / 123456.
- Scheduled Tasks: Quartz integration for cron jobs with detailed execution logs and status monitoring.
- Code Generator: Highly configurable CRUD generator for frontend and backend code, eliminating repetitive boilerplate.
- Email Utility: Rich text email dispatch supporting HTML formatting.
- Amazon S3 Cloud Storage: Multi-cloud object storage integration compatible with the Amazon S3 protocol (Qiniu Cloud, Alibaba Cloud OSS, etc.).
- Alipay Integration: Integrated Alipay payment processing with sandbox/test account support.
- Server Monitoring: Real-time monitoring of server CPU, memory, JVM, and disk utilization.
- DevOps Management: One-click remote application deployment and management.

#### Project Structure
The project adopts a modular architecture divided by functional domain as follows:

- `eladmin-common`: Common shared module containing generic utilities, base configurations, and entities.

- `eladmin-system`: Core system module and application entry point, packaged as the final deployable artifact.

- `eladmin-logging`: Audit logging module; imported by other modules requiring AOP operation logging.

- `eladmin-tools`: Third-party integration module, including Email, Amazon S3 storage, Local storage, and Alipay.

- `eladmin-generator`: Code generation module supporting automated frontend and backend CRUD generation.

#### Detailed Structure

```
- eladmin-common Shared common module
    - annotation System custom annotations
    - aspect Aspects for custom annotations
    - base Base entity definitions
    - config Common project configurations
        - MyBatis-Plus configuration
        - Web CORS, static resource mapping, Swagger, and temporary upload path configuration
        - Redis, Redisson, and async thread pool configuration
        - Security interceptor configuration: AuthorityConfig, Druid ad-filtering configuration
    - exception Global unified exception handling
    - utils System common utilities (frequently used utility classes)
        - BigDecimaUtils Monetary arithmetic utility
        - RequestHolder HTTP request context holder
        - SecurityUtils Security context utility
        - StringUtils String manipulation utility
        - SpringBeanHolder Spring application context bean holder
        - RedisUtils Redis cache utility
        - EncryptUtils Encryption and hashing utility
        - FileUtil File operations utility
- eladmin-system Core system module (Application entry point)
    - sysrunner Post-startup data initialization runners
	- modules System feature modules (Authentication, Monitoring, Scheduled Tasks, System Admin, DevOps)
- eladmin-logging System audit logging module
- eladmin-tools Third-party tools module
    - email Email dispatch utility
    - amazon Amazon S3 cloud storage utility
    - alipay Alipay payment utility
    - local-storage Local file storage utility
- eladmin-generator System code generation module
```

#### Acknowledgements

- Thanks to [PanJiaChen](https://github.com/PanJiaChen/vue-element-admin) for the frontend template.

- Thanks to [Moxun](https://github.com/moxun1639) for the generic frontend CRUD components.

- Thanks to [zhy6599](https://gitee.com/zhy6599) for the backend DevOps management features.

- Thanks to [j.yao.SUSE](https://github.com/everhopingandwaiting) for anonymous endpoint access and Redis rate limiting features.

#### Donations
Project development thrives on your support. Buy the author a coffee ☕  [Donate](https://eladmin.vip/pages/030101/)

#### Community & Feedback
- QQ Groups: 891137268, 947578238, 659622532
