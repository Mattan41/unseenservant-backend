<script setup>
import { useUserStore } from '@/features/user/userStore.js'
import HelloComponent from './HelloComponent.vue'
import { storeToRefs } from 'pinia'

const userStore = useUserStore()
const { displayName } = storeToRefs(userStore)
</script>

<template>
  <section class="text-center">
    <h1>Unseen Servant</h1>
    <p class="text-lg">A service that does not get in your way</p>
    <div class="logo-container">
      <img
        src="@/assets/logo.svg"
        alt="Unseen Servant Logo"
        class="logo max-w-[150px] h-auto my-4 mx-auto block"
      />
    </div>
    <HelloComponent :msg="displayName" />
  </section>
</template>

<style scoped>
.logo-container {
  position: relative;
  height: 150px;
  width: 100%;
  perspective: 1000px;
  overflow: hidden;
}

@keyframes float {
  0% {
    transform: translateX(-50px) translateY(0) rotate(0deg);
  }
  25% {
    transform: translateX(0) translateY(-15px) rotate(5deg);
  }
  50% {
    transform: translateX(50px) translateY(0) rotate(0deg);
  }
  75% {
    transform: translateX(0) translateY(15px) rotate(-5deg);
  }
  100% {
    transform: translateX(-50px) translateY(0) rotate(0deg);
  }
}

@keyframes hover {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.logo {
  position: relative;
  animation:
    float 15s infinite ease-in-out,
    hover 3s infinite ease-in-out;
  filter: drop-shadow(0 5px 5px rgba(0, 0, 0, 0.1));
  transform-origin: center center;
  transition: all 0.3s ease;
}

.logo:hover {
  animation-play-state: paused;
  transform: scale(1.1);
}
</style>
