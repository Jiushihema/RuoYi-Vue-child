package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysUserWorkMapper;
import com.ruoyi.system.domain.SysUserWork;
import com.ruoyi.system.service.ISysUserWorkService;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.common.utils.StringUtils;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * 用户AI作品Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
@Service
public class SysUserWorkServiceImpl implements ISysUserWorkService 
{
    @Autowired
    private SysUserWorkMapper sysUserWorkMapper;

    /**
     * 查询用户AI作品
     * 
     * @param workId 用户AI作品主键
     * @return 用户AI作品
     */
    @Override
    public SysUserWork selectSysUserWorkByWorkId(Long workId)
    {
        return sysUserWorkMapper.selectSysUserWorkByWorkId(workId);
    }

    /**
     * 查询用户AI作品列表
     * 
     * @param sysUserWork 用户AI作品
     * @return 用户AI作品
     */
    @Override
    public List<SysUserWork> selectSysUserWorkList(SysUserWork sysUserWork)
    {
        return sysUserWorkMapper.selectSysUserWorkList(sysUserWork);
    }


    /**
     * 新增用户AI作品 (含自动下载功能)
     */
    @Override
    public int insertSysUserWork(SysUserWork sysUserWork)
    {
        // ================== 1. 配置区域 ==================
        String apiKey = "353b64fb-ac13-437a-baa4-ea879d0da78a"; // TODO: 你的 API Key

        // ⚠️ 填你视频模型的 Endpoint ID (如 ep-20250206...-video)
        String videoEndpoint = "ep-20260206214932-tw888";

        // ⚠️ 填你画图模型的 Endpoint ID
        String imageEndpoint = "ep-20260206210518-m4rdj";
        // ===============================================

        sysUserWork.setStatus("0");
        sysUserWork.setCreateTime(DateUtils.getNowDate());

        String type = sysUserWork.getWorkType();
        if (StringUtils.isEmpty(type)) type = "image";

        try {
            String finalMediaUrl = ""; // 最终拿到的 URL

            if ("video".equals(type)) {
                // ==========================================
                // 🎬 视频生成逻辑 (异步任务模式)
                // ==========================================
                String taskUrl = "https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks";

                // 1. 构造复杂的 content 参数 (按你提供的 curl 格式)
                com.alibaba.fastjson2.JSONObject contentItem = new com.alibaba.fastjson2.JSONObject();
                contentItem.put("type", "text");
                // 自动把参数拼接到提示词后面 (模仿你的示例)
                String promptWithParams = sysUserWork.getTitle() + " --ratio 16:9 --resolution 720p --duration 5";
                contentItem.put("text", promptWithParams);

                com.alibaba.fastjson2.JSONArray contentArray = new com.alibaba.fastjson2.JSONArray();
                contentArray.add(contentItem);

                com.alibaba.fastjson2.JSONObject json = new com.alibaba.fastjson2.JSONObject();
                json.put("model", videoEndpoint);
                json.put("content", contentArray);

                // 2. 发送任务申请 (POST)
                System.out.println("【视频】正在提交任务...");
                String taskId = sendPostRequest(taskUrl, apiKey, json.toString());
                System.out.println("【视频】任务提交成功，ID: " + taskId);

                // 3. 轮询查结果 (守株待兔)
                // 最多等 60次 * 3秒 = 180秒 (3分钟)
                for (int i = 0; i < 60; i++) {
                    Thread.sleep(3000); // 等3秒

                    // 查询任务详情 (GET)
                    String queryUrl = taskUrl + "/" + taskId;
                    com.alibaba.fastjson2.JSONObject taskResult = sendGetRequest(queryUrl, apiKey);

                    // 获取状态
                    String status = taskResult.getString("status"); // 通常是 RUNNING, SUCCEEDED, FAILED
                    System.out.println("【视频】第" + (i+1) + "次查询状态: " + status);

                    if ("SUCCEEDED".equalsIgnoreCase(status)) {
                        System.out.println("【调试】任务成功！完整JSON: " + taskResult.toString());

                        // ==========================================
                        // ✅ 修复点：根据日志里的真实结构提取 URL
                        // 结构是: content -> video_url
                        // ==========================================
                        if (taskResult.containsKey("content")) {
                            com.alibaba.fastjson2.JSONObject content = taskResult.getJSONObject("content");
                            if (content != null) {
                                finalMediaUrl = content.getString("video_url");
                            }
                        }

                        // 如果没拿到，再抛错
                        if (StringUtils.isEmpty(finalMediaUrl)) {
                            // 截取前200个字符防止炸库
                            String safeJson = taskResult.toString();
                            if(safeJson.length() > 200) safeJson = safeJson.substring(0, 200) + "...";
                            throw new RuntimeException("解析失败，未找到video_url。JSON: " + safeJson);
                        }

                        break; // 成功！跳出循环
                    }else if ("FAILED".equalsIgnoreCase(status)) {
                        throw new RuntimeException("视频生成失败: " + taskResult.getString("error"));
                    }
                }

                if (StringUtils.isEmpty(finalMediaUrl)) {
                    throw new RuntimeException("视频生成超时，未获取到结果");
                }

            } else {
                // ==========================================
                // 🎨 图片生成逻辑 (同步模式)
                // ==========================================
                String apiUrl = "https://ark.cn-beijing.volces.com/api/v3/images/generations";

                // 构造请求参数
                com.alibaba.fastjson2.JSONObject json = new com.alibaba.fastjson2.JSONObject();
                json.put("model", imageEndpoint); // 你的图片模型ID
                json.put("prompt", sysUserWork.getTitle());
                json.put("width", 1024);
                json.put("height", 1024);

                // 发送请求
                String responseStr = sendPostRequestAndGetBody(apiUrl, apiKey, json.toString());

                // 解析结果
                com.alibaba.fastjson2.JSONObject resultJson = com.alibaba.fastjson2.JSONObject.parseObject(responseStr);

                // 1. 拿到临时 URL (这个 URL 一小时后会失效)
                finalMediaUrl = resultJson.getJSONArray("data").getJSONObject(0).getString("url");

                // 2. 【关键修改】立即下载保存！不要直接存临时 URL！
                System.out.println("【下载】正在保存图片...");
                String localUrl = downloadAndSaveFile(finalMediaUrl, "png");

                // 3. 把永久的本地路径存入对象
                sysUserWork.setMediaUrl(localUrl);
                sysUserWork.setStatus("1");
            }

            // 4. 下载并保存 (调用之前修好的下载方法)
            String extension = "video".equals(type) ? "mp4" : "png";
            System.out.println("【下载】开始下载: " + finalMediaUrl);
            String localUrl = downloadAndSaveFile(finalMediaUrl, extension);

            sysUserWork.setMediaUrl(localUrl);
            sysUserWork.setStatus("1");

        } catch (Exception e) {
            e.printStackTrace();
            sysUserWork.setStatus("2");

            // ✅ 修复点：截断错误信息，防止超过数据库 remark 字段长度 (通常是500)
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 450) {
                errorMsg = errorMsg.substring(0, 450) + "...(错误信息过长已截断)";
            }
            sysUserWork.setRemark(errorMsg);
        }

        return sysUserWorkMapper.insertSysUserWork(sysUserWork);
    }

    // ================= 辅助方法区域 =================

    // 辅助方法1: 发送 POST 并返回 Task ID (专门用于视频任务)
    private String sendPostRequest(String urlStr, String apiKey, String jsonBody) throws Exception {
        String response = sendPostRequestAndGetBody(urlStr, apiKey, jsonBody);
        com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(response);
        // 视频任务接口通常返回 { "id": "task_xxxxx", ... }
        return json.getString("id");
    }

    // 辅助方法2: 通用 POST 请求
    private String sendPostRequestAndGetBody(String urlStr, String apiKey, String jsonBody) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        try(java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("utf-8"));
        }
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("HTTP Error: " + conn.getResponseCode());
        }
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    // 辅助方法3: 通用 GET 请求 (查询任务状态用)
    private com.alibaba.fastjson2.JSONObject sendGetRequest(String urlStr, String apiKey) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return com.alibaba.fastjson2.JSONObject.parseObject(sb.toString());
    }

    /**
     * 辅助方法：下载远程文件保存到若依的上传目录下
     */
    private String downloadAndSaveFile(String remoteUrl, String extension) {
        try {
            // 1. 获取若依的基础路径 (比如 D:/ruoyi/uploadPath)
            // 注意：这里改成了 getProfile()，它是根目录，不会带 /upload
            String profile = RuoYiConfig.getProfile();

            // 2. 咱们要存到 /upload/2026/02/06/ 文件夹下
            String relativePath = "/upload/" + DateUtils.datePath() + "/";
            String fileName = IdUtils.fastUUID() + "." + extension;

            // 3. 拼接出硬盘上的绝对路径
            File file = new File(profile + relativePath + fileName);

            // 确保父目录存在
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            // 4. 下载文件
            URL url = new URL(remoteUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // 5. 返回给前端的 URL (必须以 /profile 开头)
            // 最终结果类似：/profile/upload/2026/02/06/xxxx.png
            return Constants.RESOURCE_PREFIX + relativePath + fileName;

        } catch (Exception e) {
            e.printStackTrace();
            // 如果下载失败，为了不让用户看到裂开的图，我们临时返回远程链接顶一下
            return remoteUrl;
        }
    }

    /**
     * 修改用户AI作品
     * 
     * @param sysUserWork 用户AI作品
     * @return 结果
     */
    @Override
    public int updateSysUserWork(SysUserWork sysUserWork)
    {
        sysUserWork.setUpdateTime(DateUtils.getNowDate());
        return sysUserWorkMapper.updateSysUserWork(sysUserWork);
    }

    /**
     * 批量删除用户AI作品
     * 
     * @param workIds 需要删除的用户AI作品主键
     * @return 结果
     */
    @Override
    public int deleteSysUserWorkByWorkIds(Long[] workIds)
    {
        return sysUserWorkMapper.deleteSysUserWorkByWorkIds(workIds);
    }

    /**
     * 删除用户AI作品信息
     * 
     * @param workId 用户AI作品主键
     * @return 结果
     */
    @Override
    public int deleteSysUserWorkByWorkId(Long workId)
    {
        return sysUserWorkMapper.deleteSysUserWorkByWorkId(workId);
    }
}
