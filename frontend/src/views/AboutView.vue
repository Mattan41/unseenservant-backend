<template>
  <div class="about">
    <div class="about-content max-w-4xl mx-auto p-6 bg-primary-50 rounded-lg shadow-md">
      <h1 class="font-bold mb-6 text-xs">About Unseen Servant</h1>

      <div class="mb-8">
        <h3 class="text-xl font-semibold mb-3">Your Tabletop Companion</h3>
        <p class="mb-4 text-gray-700">
          Unseen Servant is a web application designed to enhance your tabletop roleplaying experience,
          specifically for the 5th Edition of the world's most popular roleplaying game.
          Like its namesake spell, it works diligently in the background,
          handling the administrative tasks so you can focus on the adventure.
        </p>
      </div>

      <div class="grid md:grid-cols-2 gap-6 mb-8">
        <div class="feature-card">
          <h3 class="feature-title">🎭 Character Management</h3>
          <p class="text-gray-600">
            Create, update, and manage your characters with our intuitive interface.
            Track stats, inventory, spells, and abilities with ease whether your character
            is part of a campaign or a standalone adventurer.
          </p>
        </div>

        <div class="feature-card">
          <h3 class="feature-title">🏰 Campaign Organization</h3>
          <p class="text-gray-600">
            Game Masters can create and manage campaigns, invite players,
            track campaign details, and keep everything organized in one place.
            Players can join campaigns and assign their characters to adventures.
          </p>
        </div>

        <div class="feature-card">
          <h3 class="feature-title">👥 Collaborative Play</h3>
          <p class="text-gray-600">
            Designed for both Game Masters and players, Unseen Servant facilitates
            collaboration between all participants with clear role distinctions and
            shared campaign information.
          </p>
        </div>

        <div class="feature-card">
          <h3 class="feature-title">📱 Responsive Design</h3>
          <p class="text-gray-600">
            Access your campaigns and characters from any device.
            Our responsive design ensures a seamless experience whether
            you're on desktop, tablet, or mobile.
          </p>
        </div>
      </div>

      <div class="bg-primary-100 p-4 rounded-md border border-primary-200 mb-6">
        <h3 class="text-lg font-semibold mb-2">Our Mission</h3>
        <p class="text-gray-700">
          We believe that the administrative aspects of tabletop roleplaying games
          shouldn't get in the way of the storytelling and adventure.
          Unseen Servant aims to reduce bookkeeping and increase playtime,
          making your gaming sessions more enjoyable for everyone involved.
        </p>
      </div>

      <div class="text-center text-sm text-third-700 mt-6 border-t border-primary-200 pt-4">
        <p>
          <strong>AI-Generated Content Notice:</strong> The content on this About page was generated with the assistance of artificial intelligence. While we've reviewed and approved the content, we welcome your feedback to make it better reflect our actual service and mission.
        </p>
        <p class="mt-2">Unseen Servant is a fan-created tool and is not affiliated with or endorsed by the official game publishers.</p>
        <p class="mt-2">© {{ new Date().getFullYear() }} Unseen Servant</p>
      </div>

    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.about {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  background: linear-gradient(135deg, var(--color-primary-50) 0%, var(--color-primary-100) 100%);
}

.feature-card {
  background: white;
  padding: 1.25rem;
  border-radius: 0.5rem;
  border: 1px solid var(--color-primary-200);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  height: 100%; /* Säkerställer att alla kort har samma höjd */
}

.feature-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.feature-title {
  font-size: 1.1rem; /* Mindre rubrikstorlek */
  line-height: 1.3;
  margin-bottom: 0.75rem; /* Konsekvent marginal */
  min-height: 1.75rem; /* Ger plats för två rader vid behov */
  display: flex;
  align-items: center;
}

@media (min-width: 768px) {
  .feature-title {
    font-size: 1.15rem; /* Lite större på desktop */
    /* Säkerställer att rubriker inte bryter på olämpliga ställen */
    hyphens: auto;
    word-wrap: break-word;
  }
}

@media (max-width: 768px) {
  .about {
    padding: 1rem;
    align-items: flex-start;
  }

  .about-content {
    margin-top: 2rem;
    margin-bottom: 2rem;
  }
}
</style>
