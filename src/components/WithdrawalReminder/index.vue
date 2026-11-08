<template><!-- 自动提现提醒，无可见入口。 --></template>

<script setup>
import { computed, h, onBeforeUnmount, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { notification } from 'ant-design-vue';
import useUserStore from '@/store/modules/user';
import { getWithdrawalNotifications } from '@/api/member/withdrawal';
import voiceUrl from '@/assets/audio/new-withdrawal.mp3';
import { createWithdrawalMonitor, POLL_INTERVAL } from './monitor';

const user = useUserStore();
const router = useRouter();
const emit = defineEmits(['pending-count']);
const allowed = computed(() => Boolean(user.token && user.id &&
  (user.permissions.includes('*:*:*') || user.permissions.includes('member:withdrawal:list'))));
const audioReady = ref(false);
const connection = ref('loading');
const audio = new Audio(voiceUrl);
audio.preload = 'auto';
let scope = '';
let monitor;
let timer;
let generation = 0;
let unlocking = false;
let pendingSound = false;
const noticeKey = 'new-withdrawal-reminder';

function openWithdrawals() {
  router.push({ path: '/members/transaction/withdrawal', query: { status: '1', reminder: String(Date.now()) } });
  notification.close(noticeKey);
}

async function playSound() {
  if (unlocking) { pendingSound = true; return; }
  if (!audio.paused) return;
  pendingSound = false;
  const currentGeneration = generation;
  try {
    audio.currentTime = 0;
    await audio.play();
    if (currentGeneration !== generation) return;
    audioReady.value = true;
  } catch {
    if (currentGeneration !== generation) return;
    audioReady.value = false;
    pendingSound = true;
  }
}

// Reuse this media element so a normal click/key press can unlock later audio.
// The silent probe never announces a withdrawal that does not exist.
async function prepareSound() {
  if (!allowed.value || connection.value === 'unauthorized' || unlocking) return;
  if (pendingSound) { void playSound(); return; }
  if (audioReady.value || !audio.paused) return;
  const currentGeneration = generation;
  unlocking = true;
  audio.volume = 0;
  try {
    await audio.play();
    if (currentGeneration !== generation) return;
    audioReady.value = true;
  } catch {
    // A fresh page may disallow autoplay; retry on ordinary interaction.
  } finally {
    if (currentGeneration === generation) {
      audio.pause();
      audio.currentTime = 0;
      audio.volume = 1;
      unlocking = false;
      if (pendingSound) void playSound();
      void monitor?.tick();
    }
  }
}

function stop() {
  generation++;
  clearInterval(timer);
  monitor?.stop();
  monitor = undefined;
  audio.pause();
  audio.volume = 1;
  unlocking = false;
  pendingSound = false;
  audioReady.value = false;
  notification.close(noticeKey);
}

watch(() => [allowed.value, user.id, user.token], () => {
  stop();
  if (!allowed.value) return;
  scope = `withdrawal-reminder:v1:${encodeURIComponent(window.APP_CONFIG.baseApiUrl)}:${user.id}`;
  connection.value = 'loading';
  monitor = createWithdrawalMonitor({
    scope,
    storage: localStorage,
    fetchIds: getWithdrawalNotifications,
    canPlay: () => audioReady.value,
    onState: state => { connection.value = state; },
    onCount: count => emit('pending-count', count),
    onNew: async ids => {
      notification.info({
        key: noticeKey,
        message: '有新的提现申请',
        description: h('a', { onClick: openWithdrawals }, `新增 ${ids.length} 笔，请及时处理。点击查看`),
        duration: 0,
      });
      await playSound();
    },
  });
  void monitor.tick();
  void prepareSound();
  timer = setInterval(() => void monitor?.tick(), POLL_INTERVAL);
}, { immediate: true });

const interactionEvents = ['click', 'keydown', 'touchend'];
interactionEvents.forEach(event => window.addEventListener(event, prepareSound, { capture: true, passive: true }));
onBeforeUnmount(() => {
  stop();
  interactionEvents.forEach(event => window.removeEventListener(event, prepareSound, true));
});
</script>
