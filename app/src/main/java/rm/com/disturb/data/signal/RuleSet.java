package rm.com.disturb.data.signal;

import android.support.annotation.NonNull;
import java.util.List;

/**
 * Created by alex
 */

public interface RuleSet<T> extends Rule<T> {
  @NonNull List<Rule<T>> rules();
}

