<script setup>
import { computed } from 'vue';

const props = defineProps({
  src: String,
  alt: { type: String, default: '' },
  defaultSrc: {
    type: String,
    default: '/default-campaign.svg'
  }
});

const isAbsolute = src =>
  src.startsWith('http://') ||
  src.startsWith('https://') ||
  src.startsWith('blob:');

const fullSrc = computed(() => {
  if (!props.src) return props.defaultSrc;
  if (isAbsolute(props.src)) return props.src;
  if (import.meta.env.DEV && props.src.startsWith('/images/')) {
    return `${import.meta.env.VITE_API_BASE_URL}${props.src}`;
  }
  return props.src;
});

// Export for useage in background-image
defineExpose({ fullSrc });
</script>

<template>
  <img
    :src="fullSrc"
    :alt="alt"
    :class="$attrs.class"
    @error="$event.target.src = defaultSrc"
  />
</template>
