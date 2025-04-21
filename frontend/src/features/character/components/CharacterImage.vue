<!-- src/components/CharacterImage.vue -->
<script setup>
import { computed } from 'vue';

const props = defineProps({
  src: String,
  alt: { type: String, default: '' },
  defaultSrc: {
    type: String,
    default: '/src/assets/defaultCharacter.svg'
  }
});

const fullSrc = computed(() => {
  // If no src is provided, return defaultSrc
  if (!props.src) return props.defaultSrc;

  // If local asset or blob URL, return as is
  if (props.src.startsWith('/src/') ||
    props.src.startsWith('blob:') ||
    props.src.startsWith('http://') ||
    props.src.startsWith('https://')) {
    return props.src;
  }

  // Assuming src is a relative URL, prepend the base URL
  return `${import.meta.env.VITE_API_BASE_URL}${props.src}`;
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
