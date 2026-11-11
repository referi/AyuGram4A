/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.proprietary;

import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.messages.AyuSavePreferences;
import org.telegram.tgnet.TLRPC;

public class AyuMessageUtils {

    public static void map(AyuSavePreferences prefs, EditedMessage revision) {
        if (prefs == null || revision == null) {
            return;
        }
        revision.userId = prefs.getUserId();
        revision.dialogId = prefs.getDialogId();
        revision.messageId = prefs.getMessageId();
        revision.topicId = prefs.getTopicId();
        revision.entityCreateDate = prefs.getRequestCatchTime();
        TLRPC.Message msg = prefs.getMessage();
        if (msg != null) {
            revision.text = msg.message;
            revision.date = msg.date;
            revision.editDate = msg.edit_date;
        }
    }

    public static void mapMedia(AyuSavePreferences prefs, EditedMessage revision, boolean copyMedia) {
    }

    public static void map(AyuSavePreferences prefs, DeletedMessage deletedMessage) {
        if (prefs == null || deletedMessage == null) {
            return;
        }
        deletedMessage.userId = prefs.getUserId();
        deletedMessage.dialogId = prefs.getDialogId();
        deletedMessage.messageId = prefs.getMessageId();
        deletedMessage.topicId = prefs.getTopicId();
        deletedMessage.entityCreateDate = prefs.getRequestCatchTime();
        TLRPC.Message msg = prefs.getMessage();
        if (msg != null) {
            deletedMessage.text = msg.message;
            deletedMessage.date = msg.date;
            deletedMessage.editDate = msg.edit_date;
        }
    }

    public static void mapMedia(AyuSavePreferences prefs, DeletedMessage deletedMessage, boolean copyMedia) {
    }

    public static void map(EditedMessage editedMessage, TLRPC.TL_message msg, int currentAccount) {
        if (editedMessage == null || msg == null) {
            return;
        }
        msg.id = editedMessage.messageId;
        msg.message = editedMessage.text;
        msg.date = editedMessage.date;
        msg.edit_date = editedMessage.editDate;
    }

    public static void mapMedia(EditedMessage editedMessage, TLRPC.TL_message msg) {
    }
}
