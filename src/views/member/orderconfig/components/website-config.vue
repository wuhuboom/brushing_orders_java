<!-- website-config-dialog.vue (子组件) -->
<template>
  <el-form
    ref="websiteFormRef"
    :model="localForm"
    :rules="localRules"
    label-position="top"
  >
    <!-- 第一行：序号 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="序号" prop="sort">
          <el-input v-model.number="localForm.sort" placeholder="请输入序号" />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二行：网站名称，货币单位，版权 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="网站名称" prop="siteName">
          <el-input v-model="localForm.siteName" placeholder="请输入网站名称" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="货币单位" prop="currencyUnit">
          <el-input
            v-model="localForm.currencyUnit"
            placeholder="请输入货币单位 (e.g., CNY)"
            maxlength="10"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="版权" prop="copyright">
          <el-input
            v-model="localForm.copyright"
            placeholder="请输入版权信息"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第三行：网站 Logo，开屏广告图片，网站背景 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="网站 Logo">
          <image-upload v-model="localForm.siteLogo" :limit="1" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="开屏广告图片">
          <image-upload v-model="localForm.splashAdImage" :limit="1" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="网站背景">
          <image-upload v-model="localForm.siteBackground" :limit="1" />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第四行：启用汇率换算，默认连单价格类型，启用商品自动更新 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="启用汇率换算">
          <el-radio-group v-model="localForm.enableExchangeRate">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="默认连单价格类型">
          <el-radio-group v-model="localForm.defaultOrderPriceType">
            <el-radio label="0">商品价格</el-radio>
            <el-radio label="1">交易后负余额</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="启用商品自动更新">
          <el-radio-group v-model="localForm.enableProductAutoUpdate">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第五行：启用注册会员是否允许邀请，启用图片隐藏，是否隔离客服 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="启用注册会员是否允许邀请">
          <el-radio-group v-model="localForm.enableRegisterInvitation">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="启用图片隐藏">
          <el-radio-group v-model="localForm.enableImageHide">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="是否隔离客服">
          <el-radio-group v-model="localForm.isolateCustomerService">
            <el-radio label="1">否</el-radio>
            <el-radio label="2">独立客服</el-radio>
            <el-radio label="3">组织客服</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第六行：图片显示时间范围，图片根地址 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="图片显示时间范围" prop="imageDisplayTimeRange">
          <el-time-picker
            v-model="localForm.imageDisplayTimeRange"
            is-range
            range-separator="-"
            format="HH:mm"
            value-format="HH:mm"
            class="time-picker-narrow"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="图片根地址" prop="imageRootUrl">
          <el-input
            v-model="localForm.imageRootUrl"
            placeholder="e.g., https://example.com/images/"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第七行：重定向地址 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="重定向地址" prop="redirectUrl">
          <el-input
            v-model="localForm.redirectUrl"
            placeholder="请输入重定向地址"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第八行：域名列表 (文本框，逗号分隔) -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="域名列表">
          <el-input
            v-model="domainInput"
            type="textarea"
            :rows="2"
            placeholder="请输入域名，用逗号分隔 (e.g., example.com, www.example.com)"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第九行：客服脚本 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="客服脚本" prop="customerServiceScript">
          <el-input
            v-model="localForm.customerServiceScript"
            type="textarea"
            :rows="3"
            placeholder="请输入客服脚本"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第十行：客服样式 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="客服样式" prop="customerServiceStyle">
          <el-input
            v-model="localForm.customerServiceStyle"
            type="textarea"
            :rows="3"
            placeholder="请输入客服样式 (CSS)"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  loading: Boolean,
});

const emit = defineEmits(["update:form", "submit", "cancel"]);

const websiteFormRef = ref();
const localForm = reactive({ ...props.form }); // 局部 form，扩展网站字段

// 立即删除 localForm 中的 content 字段，避免嵌套
delete localForm.content;

// 域名输入（文本框，逗号分隔）
const domainInput = ref("");

// 所有规则都在子组件中定义（移除 name 规则，因为不编辑 name）
const localRules = reactive({
  sort: [{ required: true, message: "序号不能为空", trigger: "blur" }],
  siteName: [{ required: true, message: "网站名称不能为空", trigger: "blur" }],
  currencyUnit: [
    { required: true, message: "货币单位不能为空", trigger: "blur" },
  ],
  copyright: [{ required: false }],
  splashAdImage: [{ required: false }],
  siteBackground: [{ required: false }],
  enableExchangeRate: [
    { required: true, message: "请选择是否启用汇率换算", trigger: "change" },
  ],
  defaultOrderPriceType: [
    { required: true, message: "请选择默认连单价格类型", trigger: "change" },
  ],
  enableProductAutoUpdate: [
    {
      required: true,
      message: "请选择是否启用商品自动更新",
      trigger: "change",
    },
  ],
  enableRegisterInvitation: [
    { required: true, message: "请选择是否启用注册邀请", trigger: "change" },
  ],
  enableImageHide: [
    { required: true, message: "请选择是否启用图片隐藏", trigger: "change" },
  ],
  isolateCustomerService: [
    { required: true, message: "请选择客服隔离方式", trigger: "change" },
  ],
});

// 初始化：监听父 form 变化，parse content 到 localForm（仅网站字段）
watch(
  () => props.form.content,
  (newContent) => {
    if (newContent) {
      try {
        const parsed = JSON.parse(newContent);
        // 只合并网站特定字段，排除通用字段
        const websiteFields = {
          siteName: parsed.siteName,
          currencyUnit: parsed.currencyUnit,
          copyright: parsed.copyright,
          siteLogo: parsed.siteLogo,
          splashAdImage: parsed.splashAdImage,
          siteBackground: parsed.siteBackground,
          enableExchangeRate: parsed.enableExchangeRate,
          defaultOrderPriceType: parsed.defaultOrderPriceType,
          enableProductAutoUpdate: parsed.enableProductAutoUpdate,
          enableRegisterInvitation: parsed.enableRegisterInvitation,
          enableImageHide: parsed.enableImageHide,
          isolateCustomerService: parsed.isolateCustomerService,
          imageDisplayTimeRange: parsed.imageDisplayTimeRange,
          imageRootUrl: parsed.imageRootUrl,
          redirectUrl: parsed.redirectUrl,
          domainList: parsed.domainList,
          customerServiceScript: parsed.customerServiceScript,
          customerServiceStyle: parsed.customerServiceStyle,
          // 如果 sort 是网站特定，也包括；否则从父 form 取
          sort: parsed.sort || localForm.sort,
        };
        Object.assign(localForm, websiteFields);
        // 更新域名输入
        domainInput.value = Array.isArray(parsed.domainList)
          ? parsed.domainList.join(", ")
          : "";
      } catch (e) {
        ElMessage.error("解析配置失败");
      }
    }
  },
  { immediate: true }
);

// 更新域名列表：从输入解析为数组
watch(domainInput, (val) => {
  if (val) {
    localForm.domainList = val
      .split(",")
      .map((d) => d.trim())
      .filter((d) => d);
  } else {
    localForm.domainList = [];
  }
});

// 更新父 form：localForm 变化时，过滤不想要的字段后 stringify 到 content，并同步 sort
watch(
  localForm,
  () => {
    // 过滤掉不想要的字段（通用 + content + sort，如果 sort 不想在 content）
    const toSave = { ...localForm };
    [
      "id",
      "type",
      "name",
      "createTime",
      "updateTime",
      "content",
      "sort",
    ].forEach((key) => {
      delete toSave[key];
    });

    emit("update:form", {
      ...props.form,
      sort: localForm.sort, // 单独同步 sort 到父 form（数据库字段）
      content: JSON.stringify(toSave),
    });
  },
  { deep: true }
);

// 提交
function handleSubmit() {
  websiteFormRef.value.validate((valid) => {
    if (valid) {
      emit("submit");
    }
  });
}

// 取消
function handleCancel() {
  emit("cancel");
}

// 初始化 domainList 为数组
if (!localForm.domainList) localForm.domainList = [];
</script>
