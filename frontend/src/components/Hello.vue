<script setup>
defineProps({
  msg: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <div class="text-center">
    <h3>
      Welcome <span class="text-secondary">{{ msg }}</span> to Unseen Servant, the online tool for
      managing your tabletop RPG games.
    </h3>
  </div>
</template>

<style scoped>
</style>
