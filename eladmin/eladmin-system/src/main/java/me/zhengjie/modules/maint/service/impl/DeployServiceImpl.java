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
package me.zhengjie.modules.maint.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.maint.domain.App;
import me.zhengjie.modules.maint.domain.Deploy;
import me.zhengjie.modules.maint.domain.DeployHistory;
import me.zhengjie.modules.maint.domain.Server;
import me.zhengjie.modules.maint.domain.dto.DeployQueryCriteria;
import me.zhengjie.modules.maint.mapper.DeployMapper;
import me.zhengjie.modules.maint.mapper.DeployServerMapper;
import me.zhengjie.modules.maint.service.DeployHistoryService;
import me.zhengjie.modules.maint.service.DeployService;
import me.zhengjie.modules.maint.service.ServerService;
import me.zhengjie.modules.maint.util.ExecuteShellUtil;
import me.zhengjie.modules.maint.util.ScpClientUtil;
import me.zhengjie.modules.maint.domain.enums.MsgType;
import me.zhengjie.modules.maint.service.websocket.SocketMsg;
import me.zhengjie.modules.maint.service.websocket.WebSocketServer;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author zhanghouying
 * @date 2019-08-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeployServiceImpl extends ServiceImpl<DeployMapper, Deploy> implements DeployService {

	private final String FILE_SEPARATOR = "/";
	private final DeployMapper deployMapper;
	private final DeployServerMapper deployServerMapper;
	private final ServerService serverService;
	private final DeployHistoryService deployHistoryService;
	/**
	 * Number of loop iterations
	 */
	private final Integer count = 30;

	@Override
	public PageResult<Deploy> queryAll(DeployQueryCriteria criteria, Page<Object> page) {
		criteria.setOffset(page.offset());
		List<Deploy> deploys = deployMapper.findAll(criteria);
		Long total = deployMapper.countAll(criteria);
		return PageUtil.toPage(deploys, total);
	}

	@Override
	public List<Deploy> queryAll(DeployQueryCriteria criteria) {
		return deployMapper.findAll(criteria);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(Deploy resources) {
		resources.setAppId(resources.getApp().getId());
		save(resources);
		// Save association
		deployServerMapper.insertData(resources.getId(), resources.getDeploys());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Deploy resources) {
		Deploy deploy = getById(resources.getId());
		deploy.copy(resources);
		saveOrUpdate(deploy);
		// Update association
		deployServerMapper.deleteByDeployId(resources.getId());
		deployServerMapper.insertData(resources.getId(), resources.getDeploys());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Set<Long> ids) {
		removeBatchByIds(ids);
		// Delete association
		deployServerMapper.deleteByDeployIds(ids);
	}

	@Override
	public void deploy(String fileSavePath, Long id) {
		deployApp(fileSavePath, id);
	}

	/**
	 * @param fileSavePath Local path
	 * @param id ID
	 */
	private void deployApp(String fileSavePath, Long id) {
		Deploy deploy = deployMapper.getDeployById(id);
		if (deploy == null) {
			sendMsg("Deployment information does not exist", MsgType.ERROR);
			throw new BadRequestException("Deployment information does not exist");
		}
		App app = deploy.getApp();
		if (app == null) {
			sendMsg("Application information corresponding to package does not exist", MsgType.ERROR);
			throw new BadRequestException("Application information corresponding to package does not exist");
		}
		int port = app.getPort();
		// This is the server deployment path
		String uploadPath = app.getUploadPath();
		StringBuilder sb = new StringBuilder();
		String msg;
		Set<Server> deploys = deploy.getDeploys();
		for (Server server : deploys) {
			String ip = server.getIp();
			ExecuteShellUtil executeShellUtil = getExecuteShellUtil(ip);
			// Check if this is the first deployment
			boolean flag = checkFile(executeShellUtil, app);
			// First step: confirm this directory exists on server
			executeShellUtil.execute("mkdir -p " + app.getUploadPath());
			executeShellUtil.execute("mkdir -p " + app.getBackupPath());
			executeShellUtil.execute("mkdir -p " + app.getDeployPath());
			// Upload file
			msg = String.format("Logging into server: %s", ip);
			ScpClientUtil scpClientUtil = getScpClientUtil(ip);
			log.info(msg);
			sendMsg(msg, MsgType.INFO);
			msg = String.format("Uploading file to server: %s<br>Directory: %s, please wait...", ip, uploadPath);
			sendMsg(msg, MsgType.INFO);
			scpClientUtil.putFile(fileSavePath, uploadPath);
			if (flag) {
				sendMsg("Stopping original application", MsgType.INFO);
				// Stop application
				stopApp(port, executeShellUtil);
				sendMsg("Backing up original application", MsgType.INFO);
				// Back up application
				backupApp(executeShellUtil, ip, app.getDeployPath()+FILE_SEPARATOR, app.getName(), app.getBackupPath()+FILE_SEPARATOR, id);
			}
			sendMsg("Deploying application", MsgType.INFO);
			// Deploy file and start application
			String deployScript = app.getDeployScript();
			executeShellUtil.execute(deployScript);
			sleep(3);
			sendMsg("Application deploying, please wait for deployment result or check status manually later", MsgType.INFO);
			int i  = 0;
			boolean result = false;
			// Since starting the application takes time, poll status in a loop; if exceeding 30 times, consider it failed
			while (i++ < count){
				result = checkIsRunningStatus(port, executeShellUtil);
				if(result){
					break;
				}
				// Sleep for 6 seconds
				sleep(6);
			}
			sb.append("Server: ").append(server.getName()).append("<br>App: ").append(app.getName());
			sendResultMsg(result, sb);
			executeShellUtil.close();
		}
	}

	private void sleep(int second) {
		try {
			Thread.sleep(second * 1000L);
		} catch (InterruptedException e) {
			log.error(e.getMessage(),e);
		}
	}

	private void backupApp(ExecuteShellUtil executeShellUtil, String ip, String fileSavePath, String appName, String backupPath, Long id) {
		String deployDate = DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN);
		StringBuilder sb = new StringBuilder();
		backupPath += appName + FILE_SEPARATOR + deployDate + "\n";
		sb.append("mkdir -p ").append(backupPath);
		sb.append("mv -f ").append(fileSavePath);
		sb.append(appName).append(" ").append(backupPath);
		log.info("Backup application script: " + sb.toString());
		executeShellUtil.execute(sb.toString());
		// Save restore information to database
		DeployHistory deployHistory = new DeployHistory();
		deployHistory.setAppName(appName);
		deployHistory.setDeployUser(SecurityUtils.getCurrentUsername());
		deployHistory.setIp(ip);
		deployHistory.setDeployId(id);
		deployHistoryService.create(deployHistory);
	}

	/**
	 * Stop App
	 *
	 * @param port Port
	 * @param executeShellUtil /
	 */
	private void stopApp(int port, ExecuteShellUtil executeShellUtil) {
		// Send stop command
		executeShellUtil.execute(String.format("lsof -i :%d|grep -v \"PID\"|awk '{print \"kill -9\",$2}'|sh", port));

	}

	/**
	 * Check whether program on specified port is running
	 *
	 * @param port Port
	 * @param executeShellUtil /
	 * @return true running, false stopped
	 */
	private boolean checkIsRunningStatus(int port, ExecuteShellUtil executeShellUtil) {
		String result = executeShellUtil.executeForResult(String.format("fuser -n tcp %d", port));
		return result.indexOf("/tcp:")>0;
	}

	private void sendMsg(String msg, MsgType msgType) {
		try {
			WebSocketServer.sendInfo(new SocketMsg(msg, msgType), "deploy");
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public String serverStatus(Deploy resources) {
		Set<Server> servers = resources.getDeploys();
		App app = resources.getApp();
		for (Server server : servers) {
			StringBuilder sb = new StringBuilder();
			ExecuteShellUtil executeShellUtil = getExecuteShellUtil(server.getIp());
			sb.append("Server: ").append(server.getName()).append("<br>App: ").append(app.getName());
			boolean result = checkIsRunningStatus(app.getPort(), executeShellUtil);
			if (result) {
				sb.append("<br>Running");
				sendMsg(sb.toString(), MsgType.INFO);
			} else {
				sb.append("<br>Stopped!");
				sendMsg(sb.toString(), MsgType.ERROR);
			}
			log.info(sb.toString());
			executeShellUtil.close();
		}
		return "Execution completed";
	}

	private boolean checkFile(ExecuteShellUtil executeShellUtil, App app) {
		String deployPath = app.getDeployPath();
		String appName = app.getName();
		// Use secure command execution to avoid direct string concatenation, https://github.com/elunez/eladmin/issues/873
		String[] command = {"find", deployPath, "-name", appName};
		String result = executeShellUtil.executeForResult(Arrays.toString(command));
		return result.contains(appName);
	}

	/**
	 * Start service
	 * @param resources /
	 * @return /
	 */
	@Override
	public String startServer(Deploy resources) {
		Set<Server> deploys = resources.getDeploys();
		App app = resources.getApp();
		for (Server deploy : deploys) {
			StringBuilder sb = new StringBuilder();
			ExecuteShellUtil executeShellUtil = getExecuteShellUtil(deploy.getIp());
			// Stop application first to prevent duplicate startup
			stopApp(app.getPort(), executeShellUtil);
			sb.append("Server: ").append(deploy.getName()).append("<br>App: ").append(app.getName());
			sendMsg("Issuing start command", MsgType.INFO);
			executeShellUtil.execute(app.getStartScript());
			sleep(3);
			sendMsg("Application starting, please wait for result or check status manually later", MsgType.INFO);
			int i  = 0;
			boolean result = false;
			// Since starting the application takes time, poll status in a loop; if exceeding 30 times, consider it failed
			while (i++ < count){
				result = checkIsRunningStatus(app.getPort(), executeShellUtil);
				if(result){
					break;
				}
				// Sleep for 6 seconds
				sleep(6);
			}
			sendResultMsg(result, sb);
			log.info(sb.toString());
			executeShellUtil.close();
		}
		return "Execution completed";
	}

	/**
	 * Stop service
	 * @param resources /
	 * @return /
	 */
	@Override
	public String stopServer(Deploy resources) {
		Set<Server> deploys = resources.getDeploys();
		App app = resources.getApp();
		for (Server deploy : deploys) {
			StringBuilder sb = new StringBuilder();
			ExecuteShellUtil executeShellUtil = getExecuteShellUtil(deploy.getIp());
			sb.append("Server: ").append(deploy.getName()).append("<br>App: ").append(app.getName());
			sendMsg("Issuing stop command", MsgType.INFO);
			// Stop application
			stopApp(app.getPort(), executeShellUtil);
			sleep(1);
			boolean result = checkIsRunningStatus(app.getPort(), executeShellUtil);
			if (result) {
				sb.append("<br>Stop failed!");
				sendMsg(sb.toString(), MsgType.ERROR);
			} else {
				sb.append("<br>Stop successful!");
				sendMsg(sb.toString(), MsgType.INFO);
			}
			log.info(sb.toString());
			executeShellUtil.close();
		}
		return "Execution completed";
	}

	@Override
	public String serverReduction(DeployHistory resources) {
		Long deployId = resources.getDeployId();
		Deploy deployInfo = getById(deployId);
		String deployDate = DateUtil.format(resources.getDeployDate(), DatePattern.PURE_DATETIME_PATTERN);
		App app = deployInfo.getApp();
		if (app == null) {
			sendMsg("Application information does not exist: " + resources.getAppName(), MsgType.ERROR);
			throw new BadRequestException("Application information does not exist: " + resources.getAppName());
		}
		String backupPath = app.getBackupPath()+FILE_SEPARATOR;
		backupPath += resources.getAppName() + FILE_SEPARATOR + deployDate;
		// This is the server deployment path
		String deployPath = app.getDeployPath();
		String ip = resources.getIp();
		ExecuteShellUtil executeShellUtil = getExecuteShellUtil(ip);
		String msg;

		msg = String.format("Logging into server: %s", ip);
		log.info(msg);
		sendMsg(msg, MsgType.INFO);
		sendMsg("Stopping original application", MsgType.INFO);
		// Stop application
		stopApp(app.getPort(), executeShellUtil);
		// Delete original application
		sendMsg("Deleting application", MsgType.INFO);
		executeShellUtil.execute("rm -rf " + deployPath + FILE_SEPARATOR + resources.getAppName());
		// Restore application
		sendMsg("Restoring application", MsgType.INFO);
		executeShellUtil.execute("cp -r " + backupPath + "/. " + deployPath);
		sendMsg("Starting application", MsgType.INFO);
		executeShellUtil.execute(app.getStartScript());
		sendMsg("Application starting, please wait for start result or check status manually later", MsgType.INFO);
		int i  = 0;
		boolean result = false;
		// Since starting the application takes time, poll status in a loop; if exceeding 30 times, consider it failed
		while (i++ < count){
			result = checkIsRunningStatus(app.getPort(), executeShellUtil);
			if(result){
				break;
			}
			// Sleep for 6 seconds
			sleep(6);
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Server: ").append(ip).append("<br>App: ").append(resources.getAppName());
		sendResultMsg(result, sb);
		executeShellUtil.close();
		return "";
	}

	private ExecuteShellUtil getExecuteShellUtil(String ip) {
		Server server = serverService.findByIp(ip);
		if (server == null) {
			sendMsg("Server information corresponding to IP does not exist: " + ip, MsgType.ERROR);
			throw new BadRequestException("Server information corresponding to IP does not exist: " + ip);
		}
		return new ExecuteShellUtil(ip, server.getAccount(), server.getPassword(), server.getPort());
	}

	private ScpClientUtil getScpClientUtil(String ip) {
		Server server = serverService.findByIp(ip);
		if (server == null) {
			sendMsg("Server information corresponding to IP does not exist: " + ip, MsgType.ERROR);
			throw new BadRequestException("Server information corresponding to IP does not exist: " + ip);
		}
		return ScpClientUtil.getInstance(ip, server.getPort(), server.getAccount(), server.getPassword());
	}

	private void sendResultMsg(boolean result, StringBuilder sb) {
		if (result) {
			sb.append("<br>Start successful!");
			sendMsg(sb.toString(), MsgType.INFO);
		} else {
			sb.append("<br>Start failed!");
			sendMsg(sb.toString(), MsgType.ERROR);
		}
	}

	@Override
	public void download(List<Deploy> deploys, HttpServletResponse response) throws IOException {
		List<Map<String, Object>> list = new ArrayList<>();
		for (Deploy deploy : deploys) {
			Map<String,Object> map = new LinkedHashMap<>();
			map.put("Application Name", deploy.getApp().getName());
			map.put("Servers", deploy.getServers());
			map.put("Deployment Date", deploy.getCreateTime());
			list.add(map);
		}
		FileUtil.downloadExcel(list, response);
	}
}
