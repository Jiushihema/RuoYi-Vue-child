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

/**
 * 用户AI作品Controller
 * 
 * @author ruoyi
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
     */
    @PreAuthorize("@ss.hasPermi('system:work:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUserWork sysUserWork)
    {
        startPage();
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
     * 新增用户AI作品
     */
    @PreAuthorize("@ss.hasPermi('system:work:add')")
    @Log(title = "用户AI作品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysUserWork sysUserWork)
    {
        return toAjax(sysUserWorkService.insertSysUserWork(sysUserWork));
    }

    /**
     * 修改用户AI作品
     */
    @PreAuthorize("@ss.hasPermi('system:work:edit')")
    @Log(title = "用户AI作品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysUserWork sysUserWork)
    {
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
