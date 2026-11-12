/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.proprietary;

import android.util.Pair;
import android.util.SparseArray;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;

public class AyuHistoryHook {

    public static Pair<Integer, Integer> getMinAndMaxIds(ArrayList<MessageObject> messArr) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        if (messArr != null && !messArr.isEmpty()) {
            for (MessageObject obj : messArr) {
                if (obj != null) {
                    int id = obj.getId();
                    if (id < min) {
                        min = id;
                    }
                    if (id > max) {
                        max = id;
                    }
                }
            }
        }

        if (min == Integer.MAX_VALUE) {
            min = 0;
        }
        if (max == Integer.MIN_VALUE) {
            max = 0;
        }

        return new Pair<>(min, max);
    }

    public static void doHook(int currentAccount, ArrayList<MessageObject> messArr, SparseArray<MessageObject> messagesDict, int startId, int endId, long dialogId, int limit, int topicId, boolean isSecretChat) {
        // AyuHistoryHook implementation hook
    }
}
