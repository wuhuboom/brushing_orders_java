<template>
  <div class="errPage-container">
    <a-button class="pan-back-btn" type="primary" @click="back">
      <template #icon><ArrowLeftOutlined /></template>
      返回
    </a-button>
    <div class="error-content">
      <div class="error-text">
        <h1 class="text-jumbo text-ginormous">401</h1>
        <h2>您没有访问权限</h2>
        <h6>抱歉，当前账号没有访问该页面的权限，您可以返回首页。</h6>
        <ul class="list-unstyled">
          <li class="link-type">
            <router-link to="/">回首页</router-link>
          </li>
        </ul>
      </div>
      <div class="error-visual">
        <img :src="errGif" width="313" height="428" alt="No permission">
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeftOutlined } from "@ant-design/icons-vue"
import errImage from "@/assets/401_images/401.gif"

let { proxy } = getCurrentInstance()

const errGif = ref(errImage + "?" + +new Date())

function back() {
  if (proxy.$route.query.noGoBack) {
    proxy.$router.push({ path: "/" })
  } else {
    proxy.$router.go(-1)
  }
}
</script>

<style lang="scss" scoped>
.errPage-container {
  width: 800px;
  max-width: 100%;
  margin: 100px auto;

  .pan-back-btn {
    margin-bottom: 24px;
  }

  .error-content {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 313px;
    gap: 32px;
    align-items: center;
  }

  .error-visual {
    text-align: right;
  }

  .text-jumbo {
    font-size: 60px;
    font-weight: 700;
    color: #484848;
  }

  .list-unstyled {
    font-size: 14px;

    li {
      padding-bottom: 5px;
    }

    a {
      color: #008489;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 768px) {
  .errPage-container {
    margin-top: 48px;
    padding: 0 24px;

    .error-content {
      grid-template-columns: 1fr;
    }

    .error-visual {
      text-align: left;
    }
  }
}
</style>
