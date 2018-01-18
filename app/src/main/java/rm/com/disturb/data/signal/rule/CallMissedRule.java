package rm.com.disturb.data.signal.rule;

import android.support.annotation.NonNull;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import rm.com.disturb.data.signal.MessageSignal;
import rm.com.disturb.data.signal.MessageSignals;
import rm.com.disturb.data.storage.Storage;
import rm.com.disturb.data.telegram.command.TelegramCommand;
import rm.com.disturb.data.telegram.command.TelegramParams;
import rm.com.disturb.inject.qualifier.Update;
import rm.com.disturb.utils.Formats;

/**
 * Created by alex
 */

@Singleton //
public final class CallMissedRule implements Rule<MessageSignal> {

  private final TelegramCommand<Optional<String>> update;
  private final Storage<MessageSignal> signalStorage;

  @Inject CallMissedRule(@NonNull @Update TelegramCommand<Optional<String>> update,
      @NonNull Storage<MessageSignal> signalStorage) {
    this.update = update;
    this.signalStorage = signalStorage;
  }

  @Override public boolean shouldApply(@NonNull MessageSignal item) {
    return item.type().equals(MessageSignals.CALL_MISSED);
  }

  @Override public void apply(@NonNull MessageSignal signal) {
    final MessageSignal saved = signalStorage.get(signal.key()).orElse(MessageSignal.EMPTY_MESSAGE);

    if (saved.key().equals(MessageSignals.EMPTY) && !saved.type()
        .equals(MessageSignals.CALL_RINGING)) {
      return;
    }

    final TelegramParams params = new TelegramParams.Builder().messageId(saved.remoteKey())
        .text(Formats.callMissedOf(saved.sender()))
        .build();

    signalStorage.delete(signal.key());
    update.send(params).subscribe();
  }
}
