package com.ruoyi.web.controller.system;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysUserWork;
import com.ruoyi.system.service.ISysUserWorkService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils; // ✅ 引入安全工具类，用于获取当前用户ID

/**
 * 用户AI作品Controller
 * * @author ruoyi
 * @date 2026-02-06
 */
@RestController
@RequestMapping("/system/work")
public class SysUserWorkController extends BaseController
{
    @Autowired
    private ISysUserWorkService sysUserWorkService;

    /**
     * 查询用户AI作品列表
     * (已修改：支持艺术长廊模式)
     */
    @PreAuthorize("@ss.hasPermi('system:work:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUserWork sysUserWork)
    {
        startPage();

        // ✅ 核心逻辑修改：区分“艺术长廊”和“个人中心”

        if ("1".equals(sysUserWork.getIsPublic())) {
            // 场景 A: 艺术长廊
            // 前端传了 isPublic=1，说明想看所有公开作品。
            // 这里我们不设置 userId，让 Mapper 去查所有人的。
            sysUserWork.setUserId(null);
        } else {
            // 场景 B: 个人中心 (默认)
            // 如果没传 isPublic，或者 isPublic=0，强制只查当前登录用户。
            sysUserWork.setUserId(SecurityUtils.getUserId());
        }

        List<SysUserWork> list = sysUserWorkService.selectSysUserWorkList(sysUserWork);
        return getDataTable(list);
    }

    /**
     * 导出用户AI作品列表
     */
    @PreAuthorize("@ss.hasPermi('system:work:export')")
    @Log(title = "用户AI作品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUserWork sysUserWork)
    {
        // 导出时也遵循同样的权限逻辑
        if (!"1".equals(sysUserWork.getIsPublic())) {
            sysUserWork.setUserId(SecurityUtils.getUserId());
        }

        List<SysUserWork> list = sysUserWorkService.selectSysUserWorkList(sysUserWork);
        ExcelUtil<SysUserWork> util = new ExcelUtil<SysUserWork>(SysUserWork.class);
        util.exportExcel(response, list, "用户AI作品数据");
    }

    /**
     * 获取用户AI作品详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:work:query')")
    @GetMapping(value = "/{workId}")
    public AjaxResult getInfo(@PathVariable("workId") Long workId)
    {
        return success(sysUserWorkService.selectSysUserWorkByWorkId(workId));
    }

    /**
     * 新增用户AI作品 (调用AI生成)
     */
    @PreAuthorize("@ss.hasPermi('system:work:add')")
    @Log(title = "用户AI作品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysUserWork sysUserWork)
    {
        // ✅ 核心逻辑修改：绑定当前用户
        // 在插入数据库前，自动识别是谁点击的生成按钮
        sysUserWork.setUserId(SecurityUtils.getUserId());

        return toAjax(sysUserWorkService.insertSysUserWork(sysUserWork));
    }

    /**
     * 修改用户AI作品 (例如修改公开状态)
     */
    @PreAuthorize("@ss.hasPermi('system:work:edit')")
    @Log(title = "用户AI作品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysUserWork sysUserWork)
    {
        // 可以加一层校验，防止修改别人的作品(可选)
        // sysUserWork.setUserId(SecurityUtils.getUserId());
        return toAjax(sysUserWorkService.updateSysUserWork(sysUserWork));
    }

    /**
     * 删除用户AI作品
     */
    @PreAuthorize("@ss.hasPermi('system:work:remove')")
    @Log(title = "用户AI作品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{workIds}")
    public AjaxResult remove(@PathVariable Long[] workIds)
    {
        return toAjax(sysUserWorkService.deleteSysUserWorkByWorkIds(workIds));
    }
}