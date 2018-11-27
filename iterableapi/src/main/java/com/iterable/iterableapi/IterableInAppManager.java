package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;

import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iterable.iterableapi.IterableInAppHandler.InAppResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by David Truong dt@iterable.com.
 *
 * The IterableInAppManager handles creating and rendering different types of InApp Notifications received from the IterableApi
 */
public class IterableInAppManager {
    static final String TAG = "IterableInAppManager";

    private final IterableInAppStorage storage = new IterableInAppMemoryStorage();
    private final IterableInAppHandler handler;

    IterableInAppManager(IterableInAppHandler handler) {
        this.handler = handler;
    }

    /**
     *
     * @return
     */
    public List<IterableInAppMessage> getMessages() {
        return storage.getMessages();
    }

    public void syncInApp() {
        IterableApi.getInstance().getInAppMessages(10, new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(String payload) {
                if (payload != null && !payload.isEmpty()) {
                    try {
                        List<IterableInAppMessage> messages = new ArrayList<>();
                        JSONObject mainObject = new JSONObject(payload);
                        JSONArray jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject messageJson = jsonArray.optJSONObject(i);
                                IterableInAppMessage message = IterableInAppMessage.fromJSON(storage, messageJson);
                                if (message != null) {
                                    messages.add(message);
                                }
                            }
                        }
                        syncWithRemoteQueue(messages);
                    } catch (JSONException e) {
                        IterableLogger.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    public void showMessage(IterableInAppMessage message) {
        showMessage(message, true, null);
    }

    public void showMessage(IterableInAppMessage message, boolean consume, IterableHelper.IterableActionHandler clickCallback) {
        Activity currentActivity = IterableActivityMonitor.getCurrentActivity();
        // Prevent double display
        if (currentActivity != null) {
            if (IterableInAppManager.showIterableNotificationHTML(currentActivity, message.getContent().html, message.getMessageId(), new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String data) {
                    if (data != null && data.startsWith("itbl://")) {
                        IterableActionRunner.executeAction(IterableApi.getInstance().getMainActivityContext(), IterableAction.actionCustomAction(data), IterableActionSource.IN_APP);
                    } else {
                        IterableActionRunner.executeAction(IterableApi.getInstance().getMainActivityContext(), IterableAction.actionOpenUrl(data), IterableActionSource.IN_APP);
                    }
                }
            }, 0.0, message.getContent().padding)) {
                IterableApi.sharedInstance.inAppConsume(message.getMessageId());
            }
        }
    }

    private void syncWithRemoteQueue(List<IterableInAppMessage> remoteQueue) {
        Map<String, IterableInAppMessage> remoteQueueMap = new HashMap<>();
        for (IterableInAppMessage message : remoteQueue) {
            remoteQueueMap.put(message.getMessageId(), message);
            if (storage.getMessage(message.getMessageId()) == null) {
                storage.addMessage(message);
            }
        }
        for (IterableInAppMessage localMessage : storage.getMessages()) {
            if (!remoteQueueMap.containsKey(localMessage.getMessageId())) {
                storage.removeMessage(localMessage);
            }
        }
        processMessages();
    }

    private void processMessages() {
        if (!IterableActivityMonitor.isInForeground() || isShowingInApp()) {
            return;
        }

        List<IterableInAppMessage> messages = getMessages();
        for (IterableInAppMessage message : messages) {
            if (!message.isProcessed()) {
                InAppResponse response = handler.onNewInApp(message);
                message.setProcessed(true);
                if (response == InAppResponse.SHOW) {
                    showMessage(message);
                    return;
                }
            }
        }
    }

    private boolean isShowingInApp() {
        return IterableInAppHTMLNotification.getInstance() != null;
    }

    /**
     * Displays an html rendered InApp Notification
     * @param context
     * @param htmlString
     * @param messageId
     * @param clickCallback
     * @param backgroundAlpha
     * @param padding
     */
    public static boolean showIterableNotificationHTML(Context context, String htmlString, String messageId, IterableHelper.IterableActionHandler clickCallback, double backgroundAlpha, Rect padding) {
        if (context instanceof Activity) {
            Activity currentActivity = (Activity) context;
            if (htmlString != null) {
                if (IterableInAppHTMLNotification.getInstance() != null) {
                    IterableLogger.w(TAG, "Skipping the in-app notification: another notification is already being displayed");
                    return false;
                }

                IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.createInstance(context, htmlString);
                notification.setTrackParams(messageId);
                notification.setCallback(clickCallback);
                notification.setBackgroundAlpha(backgroundAlpha);
                notification.setPadding(padding);
                notification.setOwnerActivity(currentActivity);
                notification.show();
                return true;
            }
        } else {
            IterableLogger.w(TAG, "To display in-app notifications, the context must be of an instance of: Activity");
        }
        return false;
    }

}