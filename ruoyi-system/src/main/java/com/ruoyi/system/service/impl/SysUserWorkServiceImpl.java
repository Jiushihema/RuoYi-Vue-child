package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysUserWorkMapper;
import com.ruoyi.system.domain.SysUserWork;
import com.ruoyi.system.service.ISysUserWorkService;

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
     * 新增用户AI作品
     * 
     * @param sysUserWork 用户AI作品
     * @return 结果
     */
    @Override
    public int insertSysUserWork(SysUserWork sysUserWork)
    {
        sysUserWork.setCreateTime(DateUtils.getNowDate());
        return sysUserWorkMapper.insertSysUserWork(sysUserWork);
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
