<!-- src/components/CharacterImage.vue -->
<script setup>
import { computed } from 'vue';

const props = defineProps({
  src: String,
  alt: { type: String, default: '' },
  defaultSrc: {
    type: String,
    default: '/defaultCharacter.svg'
  }
});
  const isAbsolute = src =>
    src.startsWith('http://') ||
    src.startsWith('https://') ||
    src.startsWith('blob:');

  const fullSrc = computed(() => {
    if (!props.src) return props.defaultSrc;
    if (isAbsolute(props.src)) return props.src;
    if (import.meta.env.DEV) {
      // In dev: prefix with api-url if you are using a relative path
      return `${import.meta.env.VITE_API_BASE_URL}${props.src}`;
    }
    // In prod: relativ paths (or nginx should proxy)
    return props.src;
});

</script>

<template>
  <img
    :src="fullSrc"
    :alt="alt"
    :class="$attrs.class"
    @error="$event.target.src = defaultSrc"
  />
</template>
