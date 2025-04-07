<template>
  <div class="about">
    <h2>This is an about page about Unseen Servant</h2>
    <p>
      Unseen Servant is a web application that helps you manage your 5e system
      campaigns. It provides tools for tracking characters, within a campaign or not. Inside the
      campaign
      the GM can manage the players and their characters, and the players can manage their
      characters.
    </p>
  </div>
</template>

<script setup>
import {useNotificationStore} from "@/stores/notificationStore";

const notificationStore = useNotificationStore();

</script>

<style>
@media (min-width: 1024px) {
  .about {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }
}
</style>
