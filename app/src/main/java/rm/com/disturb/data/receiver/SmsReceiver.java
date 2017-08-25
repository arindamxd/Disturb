package rm.com.disturb.data.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import javax.inject.Inject;
import rm.com.disturb.DisturbApplication;
import rm.com.disturb.data.rule.MessageSignal;
import rm.com.disturb.data.rule.RuleSet;
import rm.com.disturb.data.rule.Signals;
import rm.com.disturb.utils.Intents;
import rm.com.disturb.utils.Sms;

public final class SmsReceiver extends BroadcastReceiver {

  @Inject RuleSet<MessageSignal> signalRuleSet;

  @Override public void onReceive(Context context, Intent intent) {
    if (!Intents.matches(intent, Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
      return;
    }

    ((DisturbApplication) context.getApplicationContext()).injector().inject(this);

    // slight performance dip here, but it helps readability
    signalRuleSet.consume(new MessageSignal.Builder() //
        .data(Sms.textOf(intent)) //
        .key(Sms.numberOf(intent)) //
        .type(Signals.SMS_RECEIVED) //
        .build());
  }
}