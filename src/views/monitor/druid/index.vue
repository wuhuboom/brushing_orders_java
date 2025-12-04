<template>
  <div>
    <i-frame v-model:src="url"></i-frame>
  </div>
</template>

<script setup>
import iFrame from "@/components/iFrame";

import { ref } from "vue";
const config = window.APP_CONFIG;
const url = ref(config.baseApiUrl + "/druid/login.html");
</script>
