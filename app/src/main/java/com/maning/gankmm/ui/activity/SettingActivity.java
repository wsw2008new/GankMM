package com.maning.gankmm.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.sdk.android.feedback.impl.FeedbackAPI;
import com.maning.gankmm.R;
import com.maning.gankmm.bean.AppUpdateInfo;
import com.maning.gankmm.constant.Constants;
import com.maning.gankmm.skin.SkinManager;
import com.maning.gankmm.ui.base.BaseActivity;
import com.maning.gankmm.ui.iView.ISettingView;
import com.maning.gankmm.ui.presenter.impl.SettingPresenterImpl;
import com.maning.gankmm.ui.view.MySettingItemView;
import com.maning.gankmm.utils.DialogUtils;
import com.maning.gankmm.utils.InstallUtils;
import com.maning.gankmm.utils.IntentUtils;
import com.maning.gankmm.utils.MySnackbar;
import com.maning.gankmm.utils.NetUtils;
import com.maning.gankmm.utils.SharePreUtil;
import com.socks.library.KLog;
import com.umeng.analytics.MobclickAgent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingActivity extends BaseActivity implements ISettingView {

    private static final String TAG = "SettingActivity";
    private Context context;

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.iv_push)
    ImageView ivPush;
    @Bind(R.id.iv_night_mode)
    ImageView ivNightMode;

    @Bind(R.id.item_clean_cache)
    MySettingItemView itemCleanCache;
    @Bind(R.id.item_feedback)
    MySettingItemView itemFeedback;
    @Bind(R.id.item_app_update)
    MySettingItemView itemAppUpdate;

    private SettingPresenterImpl settingPresenter;
    private MaterialDialog dialogUpdate;
    private MaterialDialog dialogCloseWarn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);

        context = this;

        initMyToolBar();

        initPresenter();

        settingPresenter.initPushState();

        settingPresenter.initNightModeState();

        settingPresenter.initCache();

        settingPresenter.initAppUpdateState();

        settingPresenter.initFeedBack();

    }

    private void initMyToolBar() {
        int currentSkinType = SkinManager.getCurrentSkinType(this);
        if (SkinManager.THEME_DAY == currentSkinType) {
            initToolBar(toolbar, "设置", R.drawable.icon_arrow_back);
        } else {
            initToolBar(toolbar, "设置", R.drawable.icon_arrow_back_night);
        }
    }

    public void initPresenter() {
        settingPresenter = new SettingPresenterImpl(this, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.item_feedback)
    void item_feedback() {
        //保存状态
        SharePreUtil.saveBooleanData(this, Constants.SPFeedback, false);
        setFeedbackState(false);
        IntentUtils.startToFeedBackPage(this);

    }

    @OnClick(R.id.item_app_update)
    public void item_app_update() {
        settingPresenter.checkAppUpdate();
    }

    @OnClick(R.id.item_clean_cache)
    void item_clean_cache() {
        showCacheDialog();
    }


    private void showCacheDialog() {
        DialogUtils.showMyDialog(this, "缓存清理", "确定要清除图片的缓存吗？", "确定", "取消", new DialogUtils.OnDialogClickListener() {

            @Override
            public void onConfirm() {
                settingPresenter.cleanCache();
            }

            @Override
            public void onCancel() {

            }
        });

    }

    @OnClick(R.id.iv_push)
    void iv_push() {
        settingPresenter.changePushState();
    }

    @OnClick(R.id.iv_night_mode)
    void iv_night_mode() {
        settingPresenter.clickNightMode();
    }

    @Override
    public void openPush() {
        ivPush.setImageResource(R.drawable.icon_setting_on);
    }

    @Override
    public void closePush() {
        ivPush.setImageResource(R.drawable.icon_setting_off);
    }

    @Override
    public void openNightMode() {
        ivNightMode.setImageResource(R.drawable.icon_setting_on);
    }

    @Override
    public void closeNightMode() {
        ivNightMode.setImageResource(R.drawable.icon_setting_off);
    }

    @Override
    public void recreateActivity() {
        startActivity(new Intent(this.getApplicationContext(), SettingActivity.class));
        overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
        this.finish();
    }

    @Override
    public void setCacheSize(String cacheSize) {
        itemCleanCache.setRightText(cacheSize);
    }

    @Override
    public void setFeedbackState(final boolean flag) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                itemFeedback.setRedDot(flag);
            }
        });
    }

    @Override
    public void setAppUpdateState(boolean flag) {
        itemAppUpdate.setRedDot(flag);
    }

    @Override
    public void showAppUpdateDialog(final AppUpdateInfo appUpdateInfo) {
        String title = "检测到新版本:V" + appUpdateInfo.getVersionShort();
        Double appSize = Double.parseDouble(appUpdateInfo.getBinary().getFsize() + "") / 1024 / 1024;
        DecimalFormat df = new DecimalFormat(".##");
        String resultSize = df.format(appSize) + "M";
        boolean isWifi = NetUtils.isWifiConnected(this);
        String content = appUpdateInfo.getChangelog() +
                "\n\n新版大小：" + resultSize +
                "\n当前网络：" + (isWifi ? "wifi" : "非wifi环境(注意)");

        DialogUtils.showMyDialog(this,
                title, content, "立马更新", "稍后更新",
                new DialogUtils.OnDialogClickListener() {
                    @Override
                    public void onConfirm() {
                        //更新版本
                        showDownloadDialog(appUpdateInfo);
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void showDownloadDialog(AppUpdateInfo appUpdateInfo) {
        dialogUpdate = new MaterialDialog.Builder(SettingActivity.this)
                .title("正在下载最新版本")
                .content("请稍等")
                .canceledOnTouchOutside(false)
                .progress(false, 100, false)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialog) {
                        if(dialogUpdate.isCancelled()){
                            return;
                        }
                        dialogCloseWarn = DialogUtils.showMyDialog(SettingActivity.this, "警告", "当前正在下载APK，是否关闭进度框？", "关闭", "取消", new DialogUtils.OnDialogClickListener() {
                            @Override
                            public void onConfirm() {
                                dialog.dismiss();
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
                    }
                })
                .show();

        new InstallUtils(context, appUpdateInfo.getInstall_url(), Constants.UpdateAPKPath, "GankMM_" + appUpdateInfo.getVersionShort(), new InstallUtils.DownloadCallBack() {
            @Override
            public void onStart() {
                KLog.i("onStart");
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.setProgress(0);
            }

            @Override
            public void onComplete(String path) {
                KLog.i("onComplete:" + path);
                InstallUtils.installAPK(context, path);
                if (dialogCloseWarn != null) {
                    dialogCloseWarn.dismiss();
                }
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.dismiss();
            }

            @Override
            public void onLoading(long total, long current) {
                KLog.i("onLoading:-----total:" + total + ",current:" + current);
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.setProgress((int) (current * 100 / total));
            }

            @Override
            public void onFail(Exception e) {
                if (dialogCloseWarn != null) {
                    dialogCloseWarn.dismiss();
                }
                if (dialogUpdate.isCancelled()) {
                    return;
                }
                dialogUpdate.dismiss();
            }

        }).downloadAPK();
    }

    @Override
    public void showBaseProgressDialog(String msg) {
        showProgressDialog(msg);
    }

    @Override
    public void hideBaseProgressDialog() {
        dissmissProgressDialog();
    }

    @Override
    public void showBasesProgressSuccess(String msg) {
        showProgressSuccess(msg);
    }

    @Override
    public void showToast(String msg) {
        MySnackbar.makeSnackBarBlack(toolbar, msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(TAG);
        MobclickAgent.onResume(this);       //统计时长
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPageStart(TAG);
        MobclickAgent.onPause(this);
    }


    @Override
    protected void onDestroy() {
        settingPresenter.detachView();
        super.onDestroy();
    }
}
