package rm.com.disturb.ui;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import rm.com.disturb.R;
import rm.com.disturb.data.storage.BooleanPreference;
import rm.com.disturb.data.storage.StringPreference;
import rm.com.disturb.data.telegram.command.TelegramCommand;
import rm.com.disturb.data.telegram.command.TelegramParams;
import rm.com.disturb.data.telegram.model.User;
import rm.com.disturb.data.telegram.source.Source;
import rm.com.disturb.inject.qualifier.ChatId;
import rm.com.disturb.inject.qualifier.Finished;
import rm.com.disturb.inject.qualifier.HandleCalls;
import rm.com.disturb.inject.qualifier.HandleSms;
import rm.com.disturb.inject.qualifier.Magnify;
import rm.com.disturb.inject.qualifier.Missed;
import rm.com.disturb.inject.qualifier.Notify;
import rm.com.disturb.inject.qualifier.Password;
import rm.com.disturb.ui.widget.BottomSheetCallbackProxy;
import rm.com.disturb.ui.adapter.SheetAdapter;
import rm.com.disturb.utils.Permissions;

import static android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED;
import static android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED;
import static rm.com.disturb.utils.BottomSheets.BEHAVIOR_ACTIONS;
import static rm.com.disturb.utils.Users.avatarColorFilterOf;
import static rm.com.disturb.utils.Users.iconLettersOf;

/**
 * Created by alex
 */

public final class NotifyFragment extends BaseFragment
    implements PasswordDialogFragment.OnPasswordConfirmationListener {
  private static final int REQ_PERMISSION = 1;

  static final ButterKnife.Setter<ViewGroup, Boolean> VISIBLE =
      (view, value, index) -> view.setVisibility(value ? View.VISIBLE : View.GONE);

  @BindString(R.string.message_test_notification) String messageTestNotification;
  @BindString(R.string.description_test_notification) String descriptionTestNotification;
  @BindString(R.string.permissions_rationale) String permissionsRationale;
  @BindString(R.string.loading_text) String loadingText;

  @BindView(R.id.notify_permissions_overlay) LinearLayout permissionsOverlay;

  @BindViews({ //
      R.id.settings_calls_header, R.id.settings_sms_header
  }) List<TextView> settingsHeaders;

  @BindViews({
      R.id.settings_calls_finished, R.id.settings_calls_missed
  }) List<ViewGroup> settingsCallsItems;

  @BindViews({
      R.id.settings_sms_magnify_item
  }) List<ViewGroup> settingsSmsItems;

  @BindView(R.id.settings_sms_toggle) SwitchCompat settingsSmsToggle;
  @BindView(R.id.settings_calls_toggle) SwitchCompat settingsCallsToggle;

  @BindView(R.id.settings_calls_finished_behavior) TextView settingsCallsFinished;
  @BindView(R.id.settings_calls_missed_behavior) TextView settingsCallsMissed;
  @BindView(R.id.settings_sms_magnify) SwitchCompat settingsSmsMagnify;
  @BindView(R.id.bottom_sheet_actions) RecyclerView bottomSheetActions;
  @BindView(R.id.bottom_sheet) LinearLayout bottomSheet;
  @BindView(R.id.bottom_sheet_dim) View sheetDim;

  @Inject @Notify TelegramCommand<Optional<String>> notify;
  @Inject @ChatId StringPreference chatIdPreference;
  @Inject @Password StringPreference passwordPreference;
  @Inject @HandleCalls BooleanPreference callsPreference;
  @Inject @HandleSms BooleanPreference smsPreference;
  @Inject @Magnify BooleanPreference magnifyPreference;
  @Inject @Missed StringPreference missedPreference;
  @Inject @Finished StringPreference finishedPreference;
  @Inject @ChatId Provider<String> chatId;
  @Inject Source<User, String> userSource;

  private final SheetAdapter sheetAdapter = new SheetAdapter();
  private BottomSheetBehavior<LinearLayout> sheetBehavior;

  @NonNull public static NotifyFragment newInstance() {
    return new NotifyFragment();
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_notify, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    injector().inject(this);
    attachToolbar();

    if (areAnyPermissionsGranted()) {
      indicateNotificationsAvailable();
    }

    if (!areAllPermissionsGranted()) {
      requestAllPermissions();
    }

    loadUser();
    showSettings();
    attachBottomSheet();
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode != REQ_PERMISSION) {
      return;
    }

    if (areAnyPermissionsGranted()) {
      indicateNotificationsAvailable();
    } else {
      Toast.makeText(getActivity(), permissionsRationale, Toast.LENGTH_LONG).show();
    }
  }

  @Override public void onPasswordConfirmed() {
    passwordPreference.delete();
    chatIdPreference.delete();
    navigateTo(LoginFragment.newInstance());
  }

  @OnClick(R.id.settings_notify) void onSendTestNotification() {
    notify.send(TelegramParams.ofMessage(messageTestNotification)).subscribe();
  }

  @OnClick(R.id.notify_permissions_icon) void onRequestPermissions() {
    if (!areAnyPermissionsGranted()) {
      requestAllPermissions();
    }
  }

  @OnClick(R.id.settings_calls_finished) void onCallsFinishedClicked() {
    sheetBehavior.setState(STATE_EXPANDED);
    sheetAdapter.setOnClickListener((position, item) -> {
      sheetBehavior.setState(STATE_COLLAPSED);
      finishedPreference.set(item.text);
      settingsCallsFinished.setText(item.text);
    });
    bottomSheetActions.setAdapter(sheetAdapter);
  }

  @OnClick(R.id.settings_calls_missed) void onCallsMissedClicked() {
    sheetBehavior.setState(STATE_EXPANDED);
    sheetAdapter.setOnClickListener((position, item) -> {
      sheetBehavior.setState(STATE_COLLAPSED);
      missedPreference.set(item.text);
      settingsCallsMissed.setText(item.text);
    });
    bottomSheetActions.setAdapter(sheetAdapter);
  }

  @OnClick(R.id.bottom_sheet_dim) void onBottomSheetDimClicked() {
    sheetBehavior.setState(STATE_COLLAPSED);
  }

  @OnCheckedChanged({ R.id.settings_calls_toggle, R.id.settings_sms_toggle }) //
  void onFeatureToggled(@NonNull SwitchCompat toggle, boolean isChecked) {
    if (toggle.getId() == R.id.settings_calls_toggle) {
      ButterKnife.apply(settingsCallsItems, VISIBLE, isChecked);
      callsPreference.set(isChecked);
    }

    if (toggle.getId() == R.id.settings_sms_toggle) {
      ButterKnife.apply(settingsSmsItems, VISIBLE, isChecked);
      smsPreference.set(isChecked);
    }
  }

  @OnClick({ R.id.settings_calls_toggle_item, R.id.settings_sms_toggle_item }) //
  void onFeatureItemClicked(@NonNull ViewGroup item) {
    final SwitchCompat toggle = (SwitchCompat) item.getChildAt(1);
    toggle.setChecked(!toggle.isChecked());
    onFeatureToggled(toggle, toggle.isChecked());
  }

  @OnCheckedChanged(R.id.settings_sms_magnify) //
  void onCodesMagnifierToggled(boolean isChecked) {
    magnifyPreference.set(isChecked);
  }

  @OnClick(R.id.settings_sms_magnify_item) //
  void onCodesMagnifierItemClicked() {
    settingsSmsMagnify.setChecked(!settingsSmsMagnify.isChecked());
    onCodesMagnifierToggled(settingsSmsMagnify.isChecked());
  }

  @OnClick(R.id.settings_logout) void onLogout() {
    if (passwordPreference.get().isEmpty()) {
      onPasswordConfirmed();
      return;
    }

    PasswordDialogFragment.show(fragmentManager(), this);
  }

  @SuppressWarnings("ConstantConditions") //
  private void showUserInfo(@NonNull User user) {
    avatarEmpty.setText(iconLettersOf(user.firstName()));
    avatarEmpty.getBackground().setColorFilter(avatarColorFilterOf(user.firstName()));

    title.setText(String.format("%s %s", user.firstName(), user.lastName()));
    subtitle.setText(String.format("@%s", user.username()));

    if (user.username().isEmpty()) {
      subtitle.setVisibility(View.GONE);
      title.setTextSize(20);
    }

    if (user.photoUrl().isEmpty()) {
      return;
    }

    Glide.with(this)
        .load(user.photoUrl())
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(avatar);
  }

  private void showSettings() {
    settingsCallsToggle.setChecked(callsPreference.get());
    settingsSmsToggle.setChecked(smsPreference.get());
    settingsCallsFinished.setText(finishedPreference.get());
    settingsCallsMissed.setText(missedPreference.get());
    settingsSmsMagnify.setChecked(magnifyPreference.get());
  }

  private void attachBottomSheet() {
    sheetAdapter.updateData(BEHAVIOR_ACTIONS);
    bottomSheetActions.setAdapter(sheetAdapter);
    sheetBehavior = BottomSheetBehavior.from(bottomSheet);
    sheetBehavior.setBottomSheetCallback(new BottomSheetCallbackProxy() {
      @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        super.onSlide(bottomSheet, slideOffset);
        Log.e("DBG", "Offset: " + slideOffset);
        sheetDim.setAlpha(slideOffset);
        sheetDim.setVisibility(slideOffset == 0 ? View.GONE : View.VISIBLE);
      }
    });
  }

  private void loadUser() {
    userSource.retrieve(chatId.get())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .subscribe(this::showUserInfo);
  }

  private boolean areAnyPermissionsGranted() {
    return Permissions.isReadPhoneStatePermissionGranted(parent())
        || Permissions.isReceiveSmsPermissionGranted(parent());
  }

  private boolean areAllPermissionsGranted() {
    return Permissions.isReadPhoneStatePermissionGranted(parent())
        && Permissions.isReceiveSmsPermissionGranted(parent());
  }

  private void indicateNotificationsAvailable() {
    permissionsOverlay.setVisibility(View.GONE);
  }

  private void requestAllPermissions() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return;
    }

    requestPermissions(new String[] {
        Manifest.permission.READ_CONTACTS, Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE
    }, REQ_PERMISSION);
  }
}
