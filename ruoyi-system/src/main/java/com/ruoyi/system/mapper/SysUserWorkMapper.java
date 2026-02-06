package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysUserWork;

/**
 * 用户AI作品Mapper接口
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public interface SysUserWorkMapper 
{
    /**
     * 查询用户AI作品
     * 
     * @param workId 用户AI作品主键
     * @return 用户AI作品
     */
    public SysUserWork selectSysUserWorkByWorkId(Long workId);

    /**
     * 查询用户AI作品列表
     * 
     * @param sysUserWork 用户AI作品
     * @return 用户AI作品集合
     */
    public List<SysUserWork> selectSysUserWorkList(SysUserWork sysUserWork);

    /**
     * 新增用户AI作品
     * 
     * @param sysUserWork 用户AI作品
     * @return 结果
     */
    public int insertSysUserWork(SysUserWork sysUserWork);

    /**
     * 修改用户AI作品
     * 
     * @param sysUserWork 用户AI作品
     * @return 结果
     */
    public int updateSysUserWork(SysUserWork sysUserWork);

    /**
     * 删除用户AI作品
     * 
     * @param workId 用户AI作品主键
     * @return 结果
     */
    public int deleteSysUserWorkByWorkId(Long workId);

    /**
     * 批量删除用户AI作品
     * 
     * @param workIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserWorkByWorkIds(Long[] workIds);
}
