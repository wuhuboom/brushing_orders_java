<template>
  <i-frame v-model:src="url"></i-frame>
</template>

<script setup>
import iFrame from "@/components/iFrame";
const config = window.APP_CONFIG;
const url = ref(config.baseApiUrl + "/swagger-ui/index.html");
</script>
