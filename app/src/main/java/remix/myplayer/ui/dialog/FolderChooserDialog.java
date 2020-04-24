package remix.myplayer.ui.dialog;

/**
 * Created by Remix on 2016/11/2.
 */

import static remix.myplayer.theme.Theme.getBaseDialog;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import remix.myplayer.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class FolderChooserDialog extends DialogFragment implements MaterialDialog.ListCallback {

  private final static String DEFAULT_TAG = "[MD_FOLDER_SELECTOR]";

  private File parentFolder;
  private File[] parentContents;
  private boolean canGoUp = true;
  private FolderCallback mCallback;

  public interface FolderCallback {

    void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder);
  }

  public FolderChooserDialog() {
  }

  String[] getContentsArray() {
    if (parentContents == null) {
      if (canGoUp) {
        return new String[]{"..."};
      }
      return new String[]{};
    }
    String[] results = new String[parentContents.length + (canGoUp ? 1 : 0)];
    if (canGoUp) {
      results[0] = "...";
    }
    for (int i = 0; i < parentContents.length; i++) {
      results[canGoUp ? i + 1 : i] = parentContents[i].getName();
    }
    return results;
  }

  File[] listFiles() {
    File[] contents = parentFolder.listFiles();
    List<File> results = new ArrayList<>();
    if (contents != null) {
      for (File fi : contents) {
        if (fi.isDirectory()) {
          results.add(fi);
        }
      }
      Collections.sort(results, new FolderSorter());
      return results.toArray(new File[results.size()]);
    }
    return null;
  }

  @SuppressWarnings("ConstantConditions")
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
            !=
            PackageManager.PERMISSION_GRANTED) {
      return new MaterialDialog.Builder(getActivity())
          .title(R.string.error)
          .content(R.string.need_permission)
          .positiveText(android.R.string.ok)
          .build();
    }

    if (getArguments() == null || !getArguments().containsKey("builder")) {
      throw new IllegalStateException("You must create a FolderChooserDialog using the Builder.");
    }
    if (!getArguments().containsKey("current_path")) {
      getArguments().putString("current_path", getBuilder().mInitialPath);
    }
    parentFolder = new File(getArguments().getString("current_path"));
    parentContents = listFiles();
    MaterialDialog.Builder builder = getBaseDialog(getActivity())
        .title(parentFolder.getAbsolutePath())
        .items(getContentsArray())
        .itemsCallback(this)
        .onPositive(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            dialog.dismiss();
            mCallback.onFolderSelection(FolderChooserDialog.this, parentFolder);
          }
        })
        .onNegative(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            dialog.dismiss();
          }
        })
        .autoDismiss(false)
        .positiveText(getBuilder().mChooseButton)
        .negativeText(getBuilder().mCancelButton)
        .positiveColorAttr(R.attr.text_color_primary)
        .negativeColorAttr(R.attr.text_color_primary)
        .neutralColorAttr(R.attr.text_color_primary)
        .backgroundColorAttr(R.attr.background_color_dialog);
    if (getBuilder().mAllowNewFolder) {
      builder.neutralText(getBuilder().mNewFolderButton);
      builder.onNeutral(new MaterialDialog.SingleButtonCallback() {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
          createNewFolder();
        }
      });
    }
    return builder.build();
  }

  private void createNewFolder() {
    new MaterialDialog.Builder(getActivity())
        .title(getBuilder().mNewFolderButton)
        .input(0, 0, false, new MaterialDialog.InputCallback() {
          @Override
          public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
            //noinspection ResultOfMethodCallIgnored
            final File newFi = new File(parentFolder, input.toString());
            if (!newFi.mkdir()) {
              Toast.makeText(getActivity(),
                  "无法创建目录 " + newFi.getAbsolutePath() + ", 请确保已授予应用外部存储读写的权限.", Toast.LENGTH_SHORT)
                  .show();
            } else {
              reload();
            }
          }
        }).show();
  }

  @Override
  public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
    if (canGoUp && i == 0) {
      parentFolder =
          parentFolder.getParentFile() != null ? parentFolder.getParentFile() : parentFolder;
      if (parentFolder.getAbsolutePath().equals("/storage/emulated")) {
        parentFolder = parentFolder.getParentFile();
      }
      canGoUp = parentFolder.getParent() != null;
    } else {
      parentFolder = parentContents[canGoUp ? i - 1 : i];
      canGoUp = true;
      if (parentFolder.getAbsolutePath().equals("/storage/emulated")) {
        parentFolder = Environment.getExternalStorageDirectory();
      }
    }
    reload();
  }

  private void reload() {
    parentContents = listFiles();
    MaterialDialog dialog = (MaterialDialog) getDialog();
    dialog.setTitle(parentFolder.getAbsolutePath());
    getArguments().putString("current_path", parentFolder.getAbsolutePath());
    dialog.setItems(getContentsArray());
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mCallback = (FolderCallback) activity;
  }

  public void show(FragmentActivity context) {
    final String tag = getBuilder().mTag;
    Fragment frag = context.getSupportFragmentManager().findFragmentByTag(tag);
    if (frag != null) {
      ((DialogFragment) frag).dismiss();
      context.getSupportFragmentManager().beginTransaction()
          .remove(frag).commit();
    }
    show(context.getSupportFragmentManager(), tag);
  }

  public static class Builder implements Serializable {

    @NonNull
    protected final transient AppCompatActivity mContext;
    @StringRes
    protected int mChooseButton;
    @StringRes
    protected int mCancelButton;
    protected String mInitialPath;
    protected String mTag;
    protected boolean mAllowNewFolder;
    @StringRes
    protected int mNewFolderButton;

    public <ActivityType extends AppCompatActivity & FolderCallback> Builder(
        @NonNull ActivityType context) {
      mContext = context;
      mChooseButton = R.string.choose_folder;
      mCancelButton = android.R.string.cancel;
      mInitialPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public Builder(Context context) {
      mContext = (AppCompatActivity) context;
      mChooseButton = R.string.choose_folder;
      mCancelButton = android.R.string.cancel;
      mInitialPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @NonNull
    public Builder chooseButton(@StringRes int text) {
      mChooseButton = text;
      return this;
    }

    @NonNull
    public Builder cancelButton(@StringRes int text) {
      mCancelButton = text;
      return this;
    }

    @NonNull
    public Builder allowNewFolder(boolean allow, @StringRes int buttonLabel) {
      mAllowNewFolder = allow;
      if (buttonLabel == 0) {
        buttonLabel = R.string.new_folder;
      }
      mNewFolderButton = buttonLabel;
      return this;
    }

    @NonNull
    public Builder initialPath(@Nullable String initialPath) {
      if (initialPath == null) {
        initialPath = File.separator;
      }
      mInitialPath = initialPath;
      return this;
    }

    @NonNull
    public Builder tag(@Nullable String tag) {
      if (tag == null) {
        tag = DEFAULT_TAG;
      }
      mTag = tag;
      return this;
    }

    @NonNull
    public FolderChooserDialog build() {
      FolderChooserDialog dialog = new FolderChooserDialog();
      Bundle args = new Bundle();
      args.putSerializable("builder", this);
      dialog.setArguments(args);
      return dialog;
    }

    @NonNull
    public FolderChooserDialog show() {
      FolderChooserDialog dialog = build();
      dialog.show(mContext);
      return dialog;
    }
  }

  @SuppressWarnings("ConstantConditions")
  @NonNull
  private Builder getBuilder() {
    return (Builder) getArguments().getSerializable("builder");
  }

  private static class FolderSorter implements Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
      return lhs.getName().compareTo(rhs.getName());
    }
  }
}
