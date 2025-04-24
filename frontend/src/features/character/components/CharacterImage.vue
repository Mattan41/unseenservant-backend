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

const fullSrc = computed(() => {
  if (!props.src) return props.defaultSrc
  if (
    props.src.startsWith('blob:') ||
    props.src.startsWith('http://') ||
    props.src.startsWith('https://') ||
    props.src.startsWith('/')
  ) {
    return props.src
  }
  // Om uppladdad bild saknar / i början, lägg till den!
  return '/' + props.src
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
