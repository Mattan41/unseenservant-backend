import {defineStore} from "pinia";
import {ref} from "vue";

export const useNotificationStore = defineStore("notification", () => {
  const notifications = ref([]);

  const addNotification = (message, type = "success", timeout = 3000) => {
    const id = Date.now();
    notifications.value.push({id, message, type});

    // remove the notification after the specified timeout
    setTimeout(() => {
      console.log("Removing notification with id:", id);
      removeNotification(id);
    }, timeout);

  };

  const removeNotification = (id) => {
    console.log("Before removing:", notifications.value);
    notifications.value = notifications.value.filter((n) => n.id !== id);
    console.log("After removing:", notifications.value);
  };


  return {
    notifications,
    addNotification,
    removeNotification,
  };
});
