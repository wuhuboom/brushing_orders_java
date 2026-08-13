<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
                  <el-form-item :label="t('member.templateInfo.templateId')" prop="templateId">
                    <el-input
                        v-model="queryParams.templateId"
                        :placeholder="t('member.templateInfo.templateIdPlaceholder')"
                        clearable
                        @keyup.enter="handleQuery"
                    />
                  </el-form-item>
                  <el-form-item :label="t('member.templateInfo.productId')" prop="productId">
                    <el-input
                        v-model="queryParams.productId"
                        :placeholder="t('member.templateInfo.productIdPlaceholder')"
                        clearable
                        @keyup.enter="handleQuery"
                    />
                  </el-form-item>
                  <el-form-item :label="t('member.templateInfo.price')" prop="price">
                    <el-input
                        v-model="queryParams.price"
                        :placeholder="t('member.templateInfo.pricePlaceholder')"
                        clearable
                        @keyup.enter="handleQuery"
                    />
                  </el-form-item>
                  <el-form-item :label="t('member.templateInfo.commissionRatio')" prop="commissionRatio">
                    <el-input
                        v-model="queryParams.commissionRatio"
                        :placeholder="t('member.templateInfo.commissionRatioPlaceholder')"
                        clearable
                        @keyup.enter="handleQuery"
                    />
                  </el-form-item>
                  <el-form-item :label="t('member.templateInfo.orderIndex')" prop="orderIndex">
                    <el-input
                        v-model="queryParams.orderIndex"
                        :placeholder="t('member.templateInfo.orderIndexPlaceholder')"
                        clearable
                        @keyup.enter="handleQuery"
                    />
                  </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{ t('member.templateInfo.search') }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{ t('member.templateInfo.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['member:templateInfo:add']"
        >{{ t('member.templateInfo.add') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['member:templateInfo:edit']"
        >{{ t('member.templateInfo.edit') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['member:templateInfo:remove']"
        >{{ t('member.templateInfo.remove') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['member:templateInfo:export']"
        >{{ t('member.templateInfo.export') }}</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateInfoList" @selection-change="handleSelectionChange" :border="true">
      <el-table-column type="selection" width="55" align="center" />
                <el-table-column :label="t('series.index.type')" align="center" prop="type">
                  <template #default="scope">
                    <el-tag type="primary" v-if="String(scope.row.type) === '1'">{{ t('series.index.typeOne') }}</el-tag>
                    <el-tag type="info" v-else-if="String(scope.row.type) === '0'">{{ t('series.index.typeTwo') }}</el-tag>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
              <el-table-column :label="t('member.templateInfo.id')" align="center" prop="id" />
              <el-table-column :label="t('member.templateInfo.templateName')" align="center" prop="templateName" />
              <el-table-column :label="t('member.templateInfo.productName')" align="center" prop="productName" />
                <el-table-column :label="t('member.templateInfo.orderIndex')" align="center" prop="orderIndex" />
              <el-table-column :label="t('member.templateInfo.operation')" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button circle type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['member:templateInfo:edit']"></el-button>
          <el-button circle type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['member:templateInfo:remove']"></el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="templateInfoRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="t('member.templateInfo.productInfo')">
          <el-input v-model="form.productName" disabled placeholder="-" />
        </el-form-item>
        <el-form-item :label="t('series.index.type')" prop="type">
          <el-select v-model="form.type" :placeholder="t('member.templateInfo.selectType')">
            <el-option :label="t('series.index.typeOne')" value="1" />
            <el-option :label="t('series.index.typeTwo')" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('member.templateInfo.price')" prop="price">
          <el-input-number v-model="form.price" :min="0" :step="0.01" style="width: 100%;" />
        </el-form-item>
        <el-form-item :label="t('member.templateInfo.commissionRatio')" prop="commissionRatio">
          <el-input-number v-model="form.commissionRatio" :min="0" :step="0.1" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
    <SeriesGoodsDrawer :modelValue="goodsDrawerVisible" @update:modelValue="(v)=>goodsDrawerVisible=v" @confirm="onGoodsConfirm" :title="t('member.templateInfo.chooseGoodsTitle')" />
  </div>
</template>

<script setup name="TemplateInfo">
  import { listTemplateInfo, getTemplateInfo, delTemplateInfo, addTemplateInfo, updateTemplateInfo } from "@/api/member/templateInfo";
  import SeriesGoodsDrawer from './SeriesGoodsDrawer.vue';
  import { watch } from 'vue';
  import { useI18n } from 'vue-i18n';

  const { t } = useI18n();

  const props = defineProps({
    templateId: {
      type: [String, Number],
      default: null
    }
  });

  const { proxy } = getCurrentInstance();

  const templateInfoList = ref([]);
  const open = ref(false);
  const goodsDrawerVisible = ref(false);
  const loading = ref(true);
  const showSearch = ref(true);
  const ids = ref([]);
  const single = ref(true);
  const multiple = ref(true);
  const total = ref(0);
  const title = ref("");

  const data = reactive({
    form: {},
    queryParams: {
      pageNum: 1,
      pageSize: 10,
        templateId: null,
        productId: null,
        price: null,
        commissionRatio: null,
        orderIndex: null,
        status: null,
        type: null
    },
    rules: {
      type: [
        { required: true, message: proxy.$t('member.templateInfo.typeRequired'), trigger: 'change' }
      ],
      price: [
        { required: true, message: proxy.$t('member.templateInfo.priceRequired'), trigger: 'blur' }
      ],
      commissionRatio: [
        { required: true, message: proxy.$t('member.templateInfo.commissionRatioRequired'), trigger: 'blur' }
      ],
    }
  });

  const { queryParams, form, rules } = toRefs(data);

  // ref to store goods selected from drawer
  const selectedGoodsFromDrawer = ref([]);


  function getList() {
    loading.value = true;
    listTemplateInfo(queryParams.value).then(response => {
            templateInfoList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    });
  }

  // 取消按钮
  function cancel() {
    open.value = false;
    reset();
  }

  // 表单重置
  function reset() {
    form.value = {
                    id: null,
                    templateId: null,
                    productId: null,
                    price: null,
                    commissionRatio: null,
                    orderIndex: null,
                    status: null,
                    createBy: null,
                    createTime: null,
                    type: null
    };
    proxy.resetForm("templateInfoRef");
  }


  /** 搜索按钮操作 */
  function handleQuery() {
    queryParams.value.pageNum = 1;
    getList();
  }

  /** 重置按钮操作 */
  function resetQuery() {
    proxy.resetForm("queryRef");
    handleQuery();
  }

  // 多选框选中数据
  function handleSelectionChange(selection) {
    ids.value = selection.map(item => item.id);
    single.value = selection.length != 1;
    multiple.value = !selection.length;
  }

  /** 新增按钮操作 */
  function handleAdd() {
    // open goods-only drawer (reuse Series' add UI without user info)
    reset();
    goodsDrawerVisible.value = true;
    title.value = proxy.$t('member.templateInfo.addTitle');
  }

  // when goods drawer confirms selection
  function onGoodsConfirm(goods) {
    // goods is an array of selected goods with fields: id, price, orderIndex, type, commissionRatio
    // create templateInfo entries for each selected good
    const promises = goods.map(g => {
      const payload = {
        templateId: queryParams.value.templateId,
        productId: g.id,
        price: Number(g.price ?? 0),
        orderIndex: Number(g.orderIndex ?? 1),
        commissionRatio: Number(g.commissionRatio ?? 10),
        type: String(g.type ?? '1')
      };
      return addTemplateInfo(payload);
    });
    Promise.all(promises).then(() => {
      proxy.$modal.msgSuccess(proxy.$t('member.templateInfo.addSuccess'));
      goodsDrawerVisible.value = false;
      getList();
    }).catch(() => {});
  }

  /** 修改按钮操作 */
  function handleUpdate(row) {
    reset();
    const _id = row.id || ids.value
    getTemplateInfo(_id).then(response => {
      // normalize types from backend
      const d = response.data || {};
      form.value = {
        ...d,
        type: d.type != null ? String(d.type) : null,
        price: d.price != null ? Number(d.price) : null,
        commissionRatio: d.commissionRatio != null ? Number(d.commissionRatio) : null
      };
      open.value = true;
      title.value = proxy.$t('member.templateInfo.editTitle');
    });
  }

  /** 提交按钮 */
  function submitForm() {
    proxy.$refs["templateInfoRef"].validate(valid => {
      if (valid) {
        if (form.value.id != null) {
          // Only allow updating price-related fields
          const payload = {
            id: form.value.id,
            type: String(form.value.type),
            price: Number(form.value.price),
            commissionRatio: Number(form.value.commissionRatio)
          };
          updateTemplateInfo(payload).then(response => {
            proxy.$modal.msgSuccess(proxy.$t('member.templateInfo.editSuccess'));
            open.value = false;
            getList();
          });
        } else {
          // ensure numeric/string types for add
          const payload = {
            ...form.value,
            price: Number(form.value.price),
            commissionRatio: Number(form.value.commissionRatio),
            type: form.value.type != null ? String(form.value.type) : undefined
          };
          addTemplateInfo(payload).then(response => {
            proxy.$modal.msgSuccess(proxy.$t('member.templateInfo.addSuccess'));
            open.value = false;
            getList();
          });
        }
      }
    });
  }

  /** 删除按钮操作 */
  function handleDelete(row) {
    const _ids = row.id || ids.value;
    proxy.$modal.confirm(proxy.$t('member.templateInfo.deleteConfirm', { ids: _ids })).then(function() {
      return delTemplateInfo(_ids);
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess(proxy.$t('member.templateInfo.deleteSuccess'));
    }).catch(() => {});
  }

  /** 导出按钮操作 */
  function handleExport() {
    proxy.download('member/templateInfo/export', {
      ...queryParams.value
    }, `templateInfo_${new Date().getTime()}.xlsx`)
  }

  // watch prop to filter by templateId
  watch(() => props.templateId, (val) => {
    queryParams.value.templateId = val;
    queryParams.value.pageNum = 1;
    getList();
  }, { immediate: true });

  getList();
</script>
