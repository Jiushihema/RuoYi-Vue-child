package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户AI作品对象 sys_user_work
 * 
 * @author ruoyi
 * @date 2026-02-06
 */
public class SysUserWork extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 作品ID */
    private Long workId;

    /** 用户ID (关联sys_user表) */
    private Long userId;

    /** 作品类型 (image=图, video=视频) */
    @Excel(name = "作品类型 (image=图, video=视频)")
    private String workType;

    /** 提示词/标题 (Prompt) */
    @Excel(name = "提示词/标题 (Prompt)")
    private String title;

    /** 作品地址 (存URL) */
    @Excel(name = "作品地址 (存URL)")
    private String mediaUrl;

    /** 状态 (0=生成中, 1=成功, 2=失败) */
    @Excel(name = "状态 (0=生成中, 1=成功, 2=失败)")
    private String status;

    /** 是否公开 (0=私有, 1=公开) */
    @Excel(name = "是否公开 (0=私有, 1=公开)")
    private String isPublic;

    public void setWorkId(Long workId) 
    {
        this.workId = workId;
    }

    public Long getWorkId() 
    {
        return workId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setWorkType(String workType) 
    {
        this.workType = workType;
    }

    public String getWorkType() 
    {
        return workType;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setMediaUrl(String mediaUrl) 
    {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaUrl() 
    {
        return mediaUrl;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setIsPublic(String isPublic) 
    {
        this.isPublic = isPublic;
    }

    public String getIsPublic() 
    {
        return isPublic;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("workId", getWorkId())
            .append("userId", getUserId())
            .append("workType", getWorkType())
            .append("title", getTitle())
            .append("mediaUrl", getMediaUrl())
            .append("status", getStatus())
            .append("isPublic", getIsPublic())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
