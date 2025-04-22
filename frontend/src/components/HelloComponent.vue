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
          Welcome <span class="rainbow-text">{{ msg }}</span>, stay a while and listen. I am Unseen
          Servant, the online tool for
          managing your tabletop RPG games.
        </h3>
      </div>
    </template>

    <style scoped>
    @keyframes rainbow {
      0% {
        background-position: 0 50%;
      }
      50% {
        background-position: 100% 50%;
      }
      100% {
        background-position: 0 50%;
      }
    }

    .rainbow-text {
      background: linear-gradient(90deg, red, orange, yellow, green, blue, indigo, violet);
      background-size: 400% 400%;
      -webkit-background-clip: text;
      background-clip: text; /* Standard property for compatibility */
      -webkit-text-fill-color: transparent;
      animation: rainbow 3s infinite;
    }
    </style>
