<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-50 to-primary-100 flex items-center justify-center p-4">
    <div class="w-full max-w-2xl bg-primary-50 border-4 border-primary-400 shadow-xl relative p-8">
      <!-- Parchment texture effect -->
      <div class="absolute inset-0 bg-gradient-to-b from-primary-50 to-primary-100 opacity-90"></div>
      <div class="absolute inset-2 border-2 border-primary-300 border-dashed"></div>
      <div class="absolute inset-4 border border-primary-200 border-dashed opacity-50"></div>

      <!-- Content -->
      <div class="relative z-10">
        <!-- Header -->
        <div class="text-center border-b-2 border-primary-400 pb-4 mb-6">
          <div class="text-6xl mb-4 text-secondary-600">🎲</div>
          <h1 class="text-3xl font-bold text-primary-800 mb-2">⚔️ The Tavern is Closed ⚔️</h1>
          <p class="text-lg italic text-primary-600">The innkeeper is still preparing the establishment</p>
        </div>

        <!-- Quest Log -->
        <div class="bg-primary-100 border-l-4 border-primary-400 pl-4 py-3 mb-6">
          <h2 class="text-xl font-semibold text-primary-700 border-b border-primary-400 pb-2 mb-3 flex items-center gap-2">
            <span>📜</span>
            <span>Quest Log</span>
          </h2>
          <p class="text-primary-700 mb-3">
            The grand opening of this tavern is still in preparation. Our master builders and enchanters are working tirelessly to complete the construction.
          </p>
          <p class="text-primary-700">
            Currently, only selected adventurers with special invitations may enter. If you're part of the testing party, verify your credentials and try again.
          </p>
        </div>

        <!-- Construction Notice -->
        <div class="bg-primary-100 border-l-4 border-primary-400 pl-4 py-3 mb-6">
          <h3 class="text-xl font-semibold text-primary-700 border-b border-primary-400 pb-2 mb-3 flex items-center gap-2">
            <span>🏗️</span>
            <span>Construction Notice</span>
          </h3>
          <p class="text-primary-700">
            The mages are still weaving the magical infrastructure, and the carpenters are reinforcing the beams. We appreciate your patience as we prepare this establishment for all adventurers.
          </p>
        </div>

        <!-- Features -->
        <div class="mb-6">
          <p class="text-primary-700 mb-3">Return when the banners are raised to find:</p>
          <ul class="list-none pl-5 space-y-2">
            <li class="flex items-center gap-2 text-primary-700">
              <span class="text-secondary-500">📜</span>
              <span>Create and manage your 5E RPG campaigns</span>
            </li>
            <li class="flex items-center gap-2 text-primary-700">
              <span class="text-secondary-500">👥</span>
              <span>Track all your characters in one place</span>
            </li>
            <li class="flex items-center gap-2 text-primary-700">
              <span class="text-secondary-500">📊</span>
              <span>Organize campaign notes and sessions</span>
            </li>
            <li class="flex items-center gap-2 text-primary-700">
              <span class="text-secondary-500">🔍</span>
              <span>Search and reference all your content</span>
            </li>
          </ul>
        </div>


        <!-- Footer -->
        <div class="pt-4 border-t border-primary-300 text-center">
          <p class="text-primary-600 italic">May your dice roll true when we open our doors!</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FantasyUnderConstruction'
}
</script>
