package rm.com.disturb.data.signal;

import android.support.annotation.NonNull;

/**
 * Created by alex
 */

public final class CallAnsweredRule implements Rule<MessageSignal> {
  @Override public boolean shouldFollow(@NonNull MessageSignal item) {
    return item.type().equals(Signals.CALL_ANSWERED);
  }

  @Override public void follow(@NonNull MessageSignal item) {

  }
}
