<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="提示词" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入提示词/标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型" prop="workType">
        <el-select v-model="queryParams.workType" placeholder="请选择类型" clearable>
          <el-option label="AI 绘画" value="image" />
          <el-option label="AI 视频" value="video" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:work:add']"
        >创建作品</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:work:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:work:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:work:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="workId" width="60" />

      <el-table-column label="提示词" align="center" prop="title" :show-overflow-tooltip="true" />

      <el-table-column label="参考图(图生视频)" align="center" width="100">
        <template slot-scope="scope">
          <image-preview v-if="scope.row.refImageUrl" :src="scope.row.refImageUrl" :width="50" :height="50"/>
          <span v-else style="color:#ccc">-</span>
        </template>
      </el-table-column>

      <el-table-column label="类型" align="center" prop="workType" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.workType === 'video'" type="warning">视频</el-tag>
          <el-tag v-else type="success">绘画</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="生成结果" align="center" width="220">
        <template slot-scope="scope">
          <video
            v-if="scope.row.workType === 'video' && scope.row.mediaUrl"
            :src="getAbsUrl(scope.row.mediaUrl)"
            style="width: 180px; height: 100px; border-radius: 4px; object-fit: cover; background: #000;"
            controls preload="metadata">
          </video>
          <image-preview
            v-else-if="scope.row.mediaUrl"
            :src="scope.row.mediaUrl"
            :width="100" :height="100"
          />
          <div v-else>
            <el-tag type="danger" v-if="scope.row.status==='2'">生成失败</el-tag>
            <el-tag type="info" v-else><i class="el-icon-loading"></i> 生成中...</el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="公开状态" align="center" prop="isPublic" width="80">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.isPublic"
            active-value="1"
            inactive-value="0"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>

      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status==='1'" type="success">成功</el-tag>
          <el-tag v-else-if="scope.row.status==='2'" type="danger">失败</el-tag>
          <el-tag v-else type="info">进行中</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:work:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:work:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">

        <el-form-item label="创作类型" prop="workType">
          <el-radio-group v-model="form.workType">
            <el-radio label="image">🎨 AI 绘画 (魔法画室)</el-radio>
            <el-radio label="video">🎬 AI 视频 (导演工作室)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="参考底图" prop="refImageUrl" v-if="form.workType === 'video'">
          <image-upload v-model="form.refImageUrl" :limit="1"/>
          <div style="font-size: 12px; color: #999; line-height: 1.5;">
            <i class="el-icon-info"></i> 选填：上传图片则进行<b>“图生视频”</b>，不传则进行<b>“文生视频”</b>。
          </div>
        </el-form-item>

        <el-form-item label="提示词" prop="title">
          <el-input
            v-model="form.title"
            type="textarea"
            :rows="3"
            placeholder="请输入画面的描述，例如：一只在太空漫步的猫..."
          />
        </el-form-item>

        <el-form-item label="是否公开" prop="isPublic">
          <el-switch
            v-model="form.isPublic"
            active-value="1"
            inactive-value="0"
            active-text="公开到艺术长廊"
            inactive-text="私有"
          ></el-switch>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>

      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          {{ submitLoading ? 'AI 正在生成中 (约1-3分钟)...' : '开始生成' }}
        </el-button>
        <el-button @click="cancel" v-if="!submitLoading">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listWork, getWork, delWork, addWork, updateWork } from "@/api/system/work"

export default {
  name: "Work",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 提交按钮loading
      submitLoading: false,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 用户AI作品表格数据
      workList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        workType: null,
        title: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        title: [
          { required: true, message: "提示词/标题不能为空", trigger: "blur" }
        ],
        workType: [
          { required: true, message: "请选择创作类型", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 辅助方法：处理绝对路径 (防止本地开发视频裂开) */
    getAbsUrl(url) {
      if (!url) return '';
      if (url.startsWith('http')) return url;
      // 拼接若依环境变量
      return process.env.VUE_APP_BASE_API + url;
    },

    /** 查询用户AI作品列表 */
    getList() {
      this.loading = true
      listWork(this.queryParams).then(response => {
        this.workList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        workId: null,
        userId: null,
        workType: "image", // 默认选中绘画
        title: null,
        mediaUrl: null,
        refImageUrl: null, // 初始化参考图
        status: "0",
        isPublic: "0",     // 默认私有
        remark: null
      }
      this.resetForm("form")
      this.submitLoading = false;
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.workId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "创建新作品"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const workId = row.workId || this.ids
      getWork(workId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改作品信息"
      })
    },
    /** 列表开关直接修改状态 */
    handleStatusChange(row) {
      let text = row.isPublic === "1" ? "公开" : "私有";
      this.$modal.confirm('确认要设置为"' + text + '"吗？').then(function() {
        return updateWork({ workId: row.workId, isPublic: row.isPublic });
      }).then(() => {
        this.$modal.msgSuccess(text + "设置成功");
      }).catch(function() {
        row.isPublic = row.isPublic === "0" ? "1" : "0";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.submitLoading = true; // 锁定按钮

          // 提示语优化
          if (this.form.workType === 'video') {
            this.$notify.info({
              title: '正在制作视频',
              message: 'AI 导演正在处理中，耗时较长(约1-3分钟)，请耐心等待，不要关闭窗口...',
              duration: 6000
            });
          }

          if (this.form.workId != null) {
            updateWork(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            }).finally(() => { this.submitLoading = false })
          } else {
            addWork(this.form).then(response => {
              this.$modal.msgSuccess("生成成功！")
              this.open = false
              this.getList()
            }).catch(err => {
              console.error(err)
            }).finally(() => {
              this.submitLoading = false
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const workIds = row.workId || this.ids
      this.$modal.confirm('是否确认删除作品编号为"' + workIds + '"的数据项？').then(function() {
        return delWork(workIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/work/export', {
        ...this.queryParams
      }, `work_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
