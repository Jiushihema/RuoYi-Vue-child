<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID (关联sys_user表)" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID (关联sys_user表)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="提示词/标题 (Prompt)" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入提示词/标题 (Prompt)"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
        >新增</el-button>
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
      <el-table-column label="作品ID" align="center" prop="workId" />
      <el-table-column label="作品类型 (image=图, video=视频)" align="center" prop="workType" />
      <el-table-column label="提示词/标题 (Prompt)" align="center" prop="title" />
<!--      <el-table-column label="作品地址 (存URL)" align="center" prop="mediaUrl" width="100">-->
<!--        <template slot-scope="scope">-->
<!--          <image-preview :src="scope.row.mediaUrl" :width="50" :height="50"/>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="作品预览" align="center" width="200">
        <template slot-scope="scope">
          <video
            v-if="scope.row.workType === 'video' && scope.row.mediaUrl"
            :src="'http://localhost:8080' + scope.row.mediaUrl"
            style="width: 160px; height: 90px; border-radius: 4px; object-fit: cover;"
            controls>
          </video>

          <el-image
            v-else-if="scope.row.mediaUrl"
            style="width: 100px; height: 100px; border-radius: 4px"
            :src="'http://localhost:8080' + scope.row.mediaUrl"
            :preview-src-list="['http://localhost:8080' + scope.row.mediaUrl]">
          </el-image>

          <span v-else style="color: #999">生成中...</span>
        </template>
      </el-table-column>
      <el-table-column label="状态 (0=生成中, 1=成功, 2=失败)" align="center" prop="status" />
      <el-table-column label="是否公开 (0=私有, 1=公开)" align="center" prop="isPublic" />
      <el-table-column label="备注 (存失败原因等)" align="center" prop="remark" />
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

    <!-- 添加或修改用户AI作品对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户ID (关联sys_user表)" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID (关联sys_user表)" />
        </el-form-item>
        <el-form-item label="提示词/标题 (Prompt)" prop="title">
          <el-input v-model="form.title" placeholder="请输入提示词/标题 (Prompt)" />
        </el-form-item>
        <el-form-item label="创作类型" prop="workType">
          <el-radio-group v-model="form.workType">
            <el-radio label="image">🎨 AI 绘画</el-radio>
            <el-radio label="video">🎬 AI 视频</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作品地址 (存URL)" prop="mediaUrl">
          <image-upload v-model="form.mediaUrl"/>
        </el-form-item>
        <el-form-item label="备注 (存失败原因等)" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
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
        mediaUrl: null,
        status: null,
        isPublic: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
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
        title: null,
        mediaUrl: null,
        status: null,
        workType: "image",
        isPublic: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      }
      this.resetForm("form")
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
      this.title = "添加用户AI作品"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const workId = row.workId || this.ids
      getWork(workId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改用户AI作品"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.workId != null) {
            updateWork(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addWork(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const workIds = row.workId || this.ids
      this.$modal.confirm('是否确认删除用户AI作品编号为"' + workIds + '"的数据项？').then(function() {
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
