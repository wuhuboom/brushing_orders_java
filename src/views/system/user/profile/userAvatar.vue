<template>
  <div class="user-info-head" @click="editCropper">
    <img :src="options.img" title="点击上传头像" class="img-circle img-lg" />

    <a-modal v-model:open="open" :title="title" width="800px" :footer="null" @cancel="closeDialog">
      <a-row :gutter="20">
        <a-col :xs="24" :md="12">
          <div class="cropper-panel">
            <vue-cropper
              v-if="visible"
              ref="cropper"
              :img="options.img"
              :info="true"
              :autoCrop="options.autoCrop"
              :autoCropWidth="options.autoCropWidth"
              :autoCropHeight="options.autoCropHeight"
              :fixedBox="options.fixedBox"
              :outputType="options.outputType"
              @realTime="realTime"
            />
          </div>
        </a-col>
        <a-col :xs="24" :md="12">
          <div class="cropper-panel preview-panel">
            <div class="avatar-upload-preview">
              <img :src="options.previews.url || options.img" :style="options.previews.img" />
            </div>
          </div>
        </a-col>
      </a-row>

      <div class="avatar-actions">
        <a-upload :show-upload-list="false" accept="image/*" :before-upload="beforeUpload">
          <a-button>
            <UploadOutlined />
            选择
          </a-button>
        </a-upload>
        <a-space>
          <a-button title="放大" @click="changeScale(1)"><PlusOutlined /></a-button>
          <a-button title="缩小" @click="changeScale(-1)"><MinusOutlined /></a-button>
          <a-button title="向左旋转" @click="rotateLeft"><RotateLeftOutlined /></a-button>
          <a-button title="向右旋转" @click="rotateRight"><RotateRightOutlined /></a-button>
        </a-space>
        <a-button type="primary" @click="uploadImg">提交</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import "vue-cropper/dist/index.css";
import { VueCropper } from "vue-cropper";
import {
  MinusOutlined,
  PlusOutlined,
  RotateLeftOutlined,
  RotateRightOutlined,
  UploadOutlined,
} from "@ant-design/icons-vue";
import { uploadAvatar } from "@/api/system/user";
import useUserStore from "@/store/modules/user";

const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const config = window.APP_CONFIG;
const cropper = ref();
const open = ref(false);
const visible = ref(false);
const title = ref("修改头像");

const options = reactive({
  img: userStore.avatar,
  autoCrop: true,
  autoCropWidth: 200,
  autoCropHeight: 200,
  fixedBox: true,
  outputType: "png",
  filename: "avatar",
  previews: {},
});

function editCropper() {
  open.value = true;
  visible.value = true;
}

function rotateLeft() {
  cropper.value?.rotateLeft();
}

function rotateRight() {
  cropper.value?.rotateRight();
}

function changeScale(num = 1) {
  cropper.value?.changeScale(num);
}

function beforeUpload(file) {
  if (!file.type.includes("image/")) {
    proxy.$modal.msgError("文件格式错误，请上传图片类型文件。");
    return false;
  }

  const reader = new FileReader();
  reader.readAsDataURL(file);
  reader.onload = () => {
    options.img = reader.result;
    options.filename = file.name;
  };
  return false;
}

function uploadImg() {
  cropper.value?.getCropBlob((data) => {
    const formData = new FormData();
    formData.append("avatarfile", data, options.filename);
    uploadAvatar(formData).then((response) => {
      open.value = false;
      visible.value = false;
      options.img = config.baseApiUrl + response.imgUrl;
      userStore.avatar = options.img;
      proxy.$modal.msgSuccess("修改成功");
    });
  });
}

function realTime(data) {
  options.previews = data;
}

function closeDialog() {
  options.img = userStore.avatar;
  visible.value = false;
}
</script>

<style lang="scss" scoped>
.user-info-head {
  position: relative;
  display: inline-block;
  width: 112px;
  height: 112px;
  cursor: pointer;
}

.img-circle {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 0 0 1px #e5e7eb;
}

.user-info-head:hover::after {
  content: "+";
  position: absolute;
  inset: 0;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.45);
  font-size: 26px;
  line-height: 112px;
  text-align: center;
  border-radius: 50%;
}

.cropper-panel {
  height: 350px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background: #fafafa;
}

.preview-panel {
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-upload-preview {
  width: 180px;
  height: 180px;
  overflow: hidden;
  border-radius: 50%;
  box-shadow: 0 0 0 1px #d9d9d9;

  img {
    max-width: none;
  }
}

.avatar-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}
</style>
