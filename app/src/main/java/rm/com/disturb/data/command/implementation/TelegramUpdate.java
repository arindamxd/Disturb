package rm.com.disturb.data.command.implementation;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Response;
import rm.com.disturb.data.async.AsyncPipeline;
import rm.com.disturb.data.async.AsyncResult;
import rm.com.disturb.data.command.Update;
import rm.com.disturb.data.storage.ChatId;
import rm.com.disturb.data.telegram.TelegramApi;
import rm.com.disturb.data.telegram.response.MessageResponse;

/**
 * Created by alex
 */

@Singleton //
public final class TelegramUpdate implements Update {
  private static final String EMPTY_MESSAGE_ID = "-1";

  private final TelegramApi api;
  private final String chatId;
  private final AsyncPipeline<String> pipeline;

  @Inject TelegramUpdate(@NonNull ExecutorService executor, @NonNull Handler mainThreadHandler,
      @NonNull TelegramApi api, @NonNull @ChatId Provider<String> chatIdProvider) {
    this.api = api;
    this.chatId = chatIdProvider.get();
    this.pipeline = new AsyncPipeline.Builder<>(EMPTY_MESSAGE_ID) //
        .executor(executor) //
        .handler(mainThreadHandler) //
        .build();
  }

  @Override public void sendAsync(@NonNull String messageId, @NonNull String message) {
    pipeline.newBuilder().task(updateMessageCallable(messageId, message)).build().invoke();
  }

  @Override public void sendAsync(@NonNull String messageId, @NonNull String message,
      @NonNull AsyncResult<String> result) {
    pipeline.newBuilder()
        .task(updateMessageCallable(messageId, message))
        .reply(result)
        .build()
        .invoke();
  }

  @WorkerThread @NonNull @Override
  public String sendBlocking(@NonNull String messageId, @NonNull String message) {
    final Call<MessageResponse> editMessage = api.editMessage(chatId, messageId, message);

    try {
      final Response<MessageResponse> response = editMessage.execute();
      final MessageResponse body = response.body();
      final boolean isOk = isResponseBodyValid(body) && response.isSuccessful();

      if (!isOk) {
        return EMPTY_MESSAGE_ID;
      }

      return String.valueOf(body.message().messageId());
    } catch (IOException e) {
      return EMPTY_MESSAGE_ID;
    }
  }

  @NonNull private Callable<String> updateMessageCallable( //
      @NonNull final String messageId, //
      @NonNull final String message //
  ) {
    return new Callable<String>() {
      @Override public String call() throws Exception {
        return sendBlocking(messageId, message);
      }
    };
  }

  private boolean isResponseBodyValid(@Nullable MessageResponse body) {
    return body != null && body.isOk() && body.message() != null;
  }
}