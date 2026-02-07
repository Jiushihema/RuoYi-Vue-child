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
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.common.utils.StringUtils;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 用户AI作品Service业务层处理 (完美融合版)
 */
@Service
public class SysUserWorkServiceImpl implements ISysUserWorkService
{
    @Autowired
    private SysUserWorkMapper sysUserWorkMapper;

    @Override
    public SysUserWork selectSysUserWorkByWorkId(Long workId)
    {
        return sysUserWorkMapper.selectSysUserWorkByWorkId(workId);
    }

    @Override
    public List<SysUserWork> selectSysUserWorkList(SysUserWork sysUserWork)
    {
        return sysUserWorkMapper.selectSysUserWorkList(sysUserWork);
    }

    /**
     * 新增用户AI作品 (含图生视频 + 自动下载功能)
     */
    @Override
    public int insertSysUserWork(SysUserWork sysUserWork)
    {
        // ================== 1. 配置区域 ==================

        // 1. 使用你提供的 Access Token (如果报错，请换回代码里原来的 353b... 那个)
        String apiKey = "Bm7e9Q5lOnm-SXkB-WXHX1C1BnGbBEXx";

        // 2. 使用 Endpoint ID
        String videoEndpoint = "ep-20260207183537-47pmz"; // 视频模型
        String imageEndpoint = "ep-20260207184128-nlmk4"; // 图片模型

        // ===============================================

        sysUserWork.setStatus("0");
        sysUserWork.setCreateTime(DateUtils.getNowDate());

        String type = sysUserWork.getWorkType();
        if (StringUtils.isEmpty(type)) type = "image";

        try {
            String finalMediaUrl = ""; // 最终拿到的 URL

            // ==========================================
            // 🎬 视频生成逻辑 (异步任务模式)
            // ==========================================
            if ("video".equals(type)) {
                String taskUrl = "https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks";

                com.alibaba.fastjson2.JSONArray contentArray = new com.alibaba.fastjson2.JSONArray();

                // ✅✅✅ 【植入的新代码】图生视频逻辑 ✅✅✅
                // 如果前端传了参考图，先把图加进去
                if (StringUtils.isNotEmpty(sysUserWork.getRefImageUrl())) {
                    com.alibaba.fastjson2.JSONObject imageItem = new com.alibaba.fastjson2.JSONObject();
                    imageItem.put("type", "image");
                    imageItem.put("image_url", sysUserWork.getRefImageUrl());
                    contentArray.add(imageItem);
                    System.out.println("【视频】检测到参考图，启用图生视频模式");
                }
                // ✅✅✅ 植入结束 ✅✅✅

                // 构造文本参数
                com.alibaba.fastjson2.JSONObject textItem = new com.alibaba.fastjson2.JSONObject();
                textItem.put("type", "text");
                String promptWithParams = sysUserWork.getTitle() + " --ratio 16:9 --resolution 720p --duration 5";
                textItem.put("text", promptWithParams);
                contentArray.add(textItem);

                // 组装请求体
                com.alibaba.fastjson2.JSONObject json = new com.alibaba.fastjson2.JSONObject();
                json.put("model", videoEndpoint);
                json.put("content", contentArray);

                // 发送任务
                System.out.println("【视频】正在提交任务...");
                String taskId = sendPostRequest(taskUrl, apiKey, json.toString());
                System.out.println("【视频】任务提交成功，ID: " + taskId);

                // 轮询结果 (最多等 180秒)
                for (int i = 0; i < 60; i++) {
                    Thread.sleep(3000); // 等3秒

                    String queryUrl = taskUrl + "/" + taskId;
                    com.alibaba.fastjson2.JSONObject taskResult = sendGetRequest(queryUrl, apiKey);
                    String status = taskResult.getString("status");

                    if ("SUCCEEDED".equalsIgnoreCase(status)) {
                        // 提取视频地址
                        if (taskResult.containsKey("content")) {
                            com.alibaba.fastjson2.JSONObject content = taskResult.getJSONObject("content");
                            if (content != null) {
                                finalMediaUrl = content.getString("video_url");
                            }
                        }
                        if (StringUtils.isEmpty(finalMediaUrl)) throw new RuntimeException("视频生成成功但未返回URL");
                        break;
                    } else if ("FAILED".equalsIgnoreCase(status)) {
                        throw new RuntimeException("视频生成失败: " + taskResult.getString("error"));
                    }
                }

                if (StringUtils.isEmpty(finalMediaUrl)) throw new RuntimeException("视频生成超时");

                // ==========================================
                // 🎨 图片生成逻辑 (同步模式)
                // ==========================================
            } else {
                String apiUrl = "https://ark.cn-beijing.volces.com/api/v3/images/generations";

                com.alibaba.fastjson2.JSONObject json = new com.alibaba.fastjson2.JSONObject();
                json.put("model", imageEndpoint);
                json.put("prompt", sysUserWork.getTitle());
                json.put("width", 1024);
                json.put("height", 1024);

                String responseStr = sendPostRequestAndGetBody(apiUrl, apiKey, json.toString());
                com.alibaba.fastjson2.JSONObject resultJson = com.alibaba.fastjson2.JSONObject.parseObject(responseStr);

                if (resultJson.containsKey("data")) {
                    finalMediaUrl = resultJson.getJSONArray("data").getJSONObject(0).getString("url");
                } else {
                    throw new RuntimeException("图片生成失败: " + responseStr);
                }
            }

            // ==========================================
            // ⬇️ 下载保存逻辑 (保留你同伴写好的逻辑)
            // ==========================================
            String extension = "video".equals(type) ? "mp4" : "png";
            System.out.println("【下载】开始下载: " + finalMediaUrl);
            String localUrl = downloadAndSaveFile(finalMediaUrl, extension);

            sysUserWork.setMediaUrl(localUrl);
            sysUserWork.setStatus("1");

        } catch (Exception e) {
            e.printStackTrace();
            sysUserWork.setStatus("2");
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 450) {
                errorMsg = errorMsg.substring(0, 450) + "...";
            }
            sysUserWork.setRemark(errorMsg);
        }

        return sysUserWorkMapper.insertSysUserWork(sysUserWork);
    }

    @Override
    public int updateSysUserWork(SysUserWork sysUserWork)
    {
        sysUserWork.setUpdateTime(DateUtils.getNowDate());
        return sysUserWorkMapper.updateSysUserWork(sysUserWork);
    }

    @Override
    public int deleteSysUserWorkByWorkIds(Long[] workIds)
    {
        return sysUserWorkMapper.deleteSysUserWorkByWorkIds(workIds);
    }

    @Override
    public int deleteSysUserWorkByWorkId(Long workId)
    {
        return sysUserWorkMapper.deleteSysUserWorkByWorkId(workId);
    }

    // ================= 辅助方法区域 (保留原作者的实现) =================

    private String sendPostRequest(String urlStr, String apiKey, String jsonBody) throws Exception {
        String response = sendPostRequestAndGetBody(urlStr, apiKey, jsonBody);
        com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(response);
        return json.getString("id");
    }

    private String sendPostRequestAndGetBody(String urlStr, String apiKey, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        try(java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("utf-8"));
        }
        if (conn.getResponseCode() != 200) {
            try(InputStream errorStream = conn.getErrorStream()) {
                if(errorStream!=null) {
                    byte[] bytes = new byte[1024];
                    int len = errorStream.read(bytes);
                    if(len > 0) System.err.println("API Error: " + new String(bytes, 0, len));
                }
            }
            throw new RuntimeException("HTTP Error: " + conn.getResponseCode());
        }
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private com.alibaba.fastjson2.JSONObject sendGetRequest(String urlStr, String apiKey) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return com.alibaba.fastjson2.JSONObject.parseObject(sb.toString());
    }

    private String downloadAndSaveFile(String remoteUrl, String extension) {
        try {
            String profile = RuoYiConfig.getProfile();
            String relativePath = "/upload/" + DateUtils.datePath() + "/";
            String fileName = IdUtils.fastUUID() + "." + extension;
            File file = new File(profile + relativePath + fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
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
            return Constants.RESOURCE_PREFIX + relativePath + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return remoteUrl;
        }
    }
}