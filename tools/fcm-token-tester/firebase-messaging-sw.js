// FCM 웹 푸시는 서비스 워커가 있어야 토큰을 발급받고 백그라운드 알림을 받을 수 있다.
// 설정값은 등록 URL의 쿼리스트링으로 전달된다(index.html 참고).
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import { getMessaging, onBackgroundMessage } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-sw.js";

const params = new URL(self.location).searchParams;
const app = initializeApp({
  apiKey: params.get("apiKey"),
  projectId: params.get("projectId"),
  messagingSenderId: params.get("messagingSenderId"),
  appId: params.get("appId"),
});

onBackgroundMessage(getMessaging(app), (payload) => {
  const notification = payload.notification ?? {};
  self.registration.showNotification(notification.title ?? "해미 알림", {
    body: notification.body ?? "",
  });
});
