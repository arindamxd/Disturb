package rm.com.disturb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.telephony.SmsMessage;
import java.util.List;
import javax.inject.Inject;

import static rm.com.disturb.Lists.listOfArray;
import static rm.com.disturb.Lists.map;
import static rm.com.disturb.Lists.reduce;

public final class SmsReceiver extends BroadcastReceiver {
  private static final String KEY_PDU_CHUNKS = "pdus";

  @Inject Notifier notifier;
  @Inject ContactBook contactBook;

  @Override public void onReceive(Context context, Intent intent) {
    if (!Intents.matches(intent, Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
      return;
    }

    ((DisturbApplication) context.getApplicationContext()).injector().inject(this);

    final List<SmsMessage> receivedChunks = unwrapMessage(intent);
    final String number = receivedChunks.get(0).getOriginatingAddress();
    final String messageText = unwrapMessageText(receivedChunks);

    if (Permissions.isReadContactsPermissionGranted(context)) {
      notifyWithContactName(number, messageText);
    } else {
      notifySms(number, messageText);
    }
  }

  private void notifySms(@NonNull String from, @NonNull String text) {
    notifier.notify(Formats.smsOf(from, text));
  }

  private void notifyWithContactName(@NonNull final String number,
      @NonNull final String messageText) {
    contactBook.findNameAsync(number, new AsyncResult<String>() {
      @Override public void ready(@NonNull String contactName) {
        final String from = Formats.contactNameOf(contactName, number);

        notifySms(from, messageText);
      }
    });
  }

  @NonNull private List<SmsMessage> unwrapMessage(@NonNull Intent intent) {
    final List<Object> pduChunks = listOfArray((Object[]) intent.getExtras().get(KEY_PDU_CHUNKS));

    return map(pduChunks, new Lists.Transformer<Object, SmsMessage>() {
      @Override public SmsMessage invoke(Object item) {
        //noinspection deprecation
        return SmsMessage.createFromPdu((byte[]) item);
      }
    });
  }

  @NonNull private String unwrapMessageText(@NonNull List<SmsMessage> receivedChunks) {
    return reduce(receivedChunks, "", new Lists.Accumulator<SmsMessage, String>() {
      @Override public String collect(String result, SmsMessage item) {
        return result + item.getMessageBody();
      }
    });
  }
}