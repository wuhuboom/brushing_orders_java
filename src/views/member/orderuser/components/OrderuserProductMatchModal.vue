<template>
  <a-modal
    v-model:open="visible"
    title="修改产品匹配"
    width="520px"
    :mask-closable="false"
    :confirm-loading="submitting"
    :closable="!submitting"
    :keyboard="!submitting"
    :cancel-button-props="{ disabled: submitting }"
    :ok-button-props="{ disabled: loading || !isLoadedUser }"
    ok-text="确定"
    cancel-text="取消"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" :disabled="loading || submitting">
        <a-form-item label="产品匹配" name="productMatching" required>
          <a-radio-group v-model:value="form.productMatching" @change="revalidateRange">
            <a-radio value="1">禁用</a-radio>
            <a-radio value="0">启用</a-radio>
          </a-radio-group>
          <div v-if="form.productMatching === '1'" class="match-tip">禁用后使用全局匹配范围</div>
        </a-form-item>
        <a-form-item label="最小匹配比例" name="productMatchMin" :required="form.productMatching === '0'">
          <a-input-number
            v-model:value="form.productMatchMin"
            :min="0"
            :max="100"
            :precision="2"
            :disabled="loading || submitting || form.productMatching !== '0'"
            addon-after="%"
            placeholder="请输入最小匹配比例"
            class="full-width"
            @change="revalidateMaximum"
          />
        </a-form-item>
        <a-form-item label="最大匹配比例" name="productMatchMax" :required="form.productMatching === '0'">
          <a-input-number
            v-model:value="form.productMatchMax"
            :min="0"
            :max="100"
            :precision="2"
            :disabled="loading || submitting || form.productMatching !== '0'"
            addon-after="%"
            placeholder="请输入最大匹配比例"
            class="full-width"
          />
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup name="OrderuserProductMatchModal">
import { computed, getCurrentInstance, nextTick, reactive, ref, watch } from "vue";
import { getOrderuser, updateOrderuser } from "@/api/member/orderuser";
import { buildProductMatchPayload, productMatchValidationErrors } from "./orderuserProductMatch";

const props = defineProps({ modelValue: Boolean, userId: [Number, String] });
const emit = defineEmits(["update:modelValue", "success"]);
const { proxy } = getCurrentInstance();
const formRef = ref();
const loading = ref(false);
const submitting = ref(false);
let detailRequestSequence = 0;
const defaults = {
  id: null,
  version: null,
  productMatching: "1",
  productMatchMin: null,
  productMatchMax: null,
};
const form = reactive({ ...defaults });
const storedRange = reactive({ productMatchMin: null, productMatchMax: null });
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});
const isLoadedUser = computed(() => props.modelValue && !loading.value
  && form.id != null && String(form.id) === String(props.userId));
const rules = Object.fromEntries(["productMatching", "productMatchMin", "productMatchMax"].map((field) => [
  field,
  [{ validator: () => {
    const error = productMatchValidationErrors(form)[field];
    return error ? Promise.reject(new Error(error)) : Promise.resolve();
  }, trigger: ["change", "blur"] }],
]));

watch(() => [props.modelValue, props.userId], ([open, userId]) => loadProductMatch(open, userId), { immediate: true });

function isCurrentEditSession(userId, sequence) {
  return sequence === detailRequestSequence && props.modelValue
    && String(props.userId) === String(userId);
}

async function loadProductMatch(open, userId) {
  const sequence = ++detailRequestSequence;
  loading.value = false;
  Object.assign(form, defaults);
  Object.assign(storedRange, { productMatchMin: null, productMatchMax: null });
  await nextTick();
  if (sequence !== detailRequestSequence) return;
  formRef.value?.clearValidate();
  if (!open || userId == null) return;
  loading.value = true;
  try {
    const response = await getOrderuser(userId);
    if (!isCurrentEditSession(userId, sequence)) return;
    const data = response.data || {};
    if (data.id == null || String(data.id) !== String(userId)) {
      throw new Error("产品匹配加载失败，请重试");
    }
    Object.assign(form, {
      id: data.id,
      version: data.version,
      productMatching: String(data.productMatching ?? "1"),
      productMatchMin: data.productMatchMin ?? null,
      productMatchMax: data.productMatchMax ?? null,
    });
    Object.assign(storedRange, {
      productMatchMin: form.productMatchMin,
      productMatchMax: form.productMatchMax,
    });
  } catch (error) {
    if (isCurrentEditSession(userId, sequence)) {
      proxy.$modal.msgError(error?.message || "产品匹配加载失败，请重试");
    }
  } finally {
    if (sequence === detailRequestSequence) loading.value = false;
  }
}

function revalidateRange() {
  if (form.productMatching === "1") Object.assign(form, storedRange);
  formRef.value?.clearValidate();
}

function revalidateMaximum() {
  if (form.productMatchMax != null) {
    formRef.value?.validateFields(["productMatchMax"]).catch(() => {});
  }
}

async function handleSubmit() {
  if (submitting.value || loading.value || !isLoadedUser.value) return;
  const userId = form.id;
  const sequence = detailRequestSequence;
  submitting.value = true;
  try {
    try {
      await formRef.value.validate();
    } catch (error) {
      if (!error?.errorFields) proxy.$modal.msgError(error?.message || "产品匹配校验失败");
      return;
    }
    if (!isCurrentEditSession(userId, sequence)) return;
    await updateOrderuser(buildProductMatchPayload(form));
    if (isCurrentEditSession(userId, sequence)) visible.value = false;
    emit("success");
    proxy.$modal.msgSuccess("修改产品匹配成功");
  } catch (error) {
    if (isCurrentEditSession(userId, sequence)) {
      proxy.$modal.msgError(error?.message || "修改产品匹配失败");
    }
  } finally {
    submitting.value = false;
  }
}

function handleCancel() {
  if (submitting.value) return;
  visible.value = false;
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
.match-tip {
  margin-top: 8px;
  color: #8c8c8c;
}
</style>
