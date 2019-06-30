package io.playtext.playtext.dashboard;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;
import org.codechimp.apprater.AppRater;
import org.codechimp.apprater.GoogleMarket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.playtext.playtext.R;
import io.playtext.playtext.about.BuyMoreActivity;
import io.playtext.playtext.base.AppearanceActivity;
import io.playtext.playtext.base.BaseActivity;
import io.playtext.playtext.test.SettingsActivity;
import io.playtext.playtext.tools.AppInfo;
import io.playtext.playtext.tools.Cache;
import io.playtext.playtext.tools.CircleTransform;
import io.playtext.playtext.tools.Ctes;
import io.playtext.playtext.tools.Go;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DashboardActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    public DashboardActivity dashboardActivity;
    PrimaryDrawerItem about, statistics, mode, feedback, rate, privacy_policy,
            myTest, close, buyMore, announces, version;
    Drawer drawer;
    Toolbar toolbar;

    public boolean isAnnounceIconVisible() {
        return isAnnounceIconVisible;
    }

    public void setAnnounceIconVisible(boolean announceIconVisible) {
        isAnnounceIconVisible = announceIconVisible;
    }

    private boolean isAnnounceIconVisible;

    private GoogleApiClient googleApiClient;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dashboardActivity = this;
        setAnnounceIconVisible(true);
        setContentView(R.layout.activity_dashboard);



        progressBar = findViewById(R.id.my_horizontal_progressBar);

        runOnlyOnce();


        if (firebaseUser == null) {
            Go.GoToGoogleSignInActivity(this);
        } else {
            displayToolbar();

            googleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .build();
            setAppRater();

            setNavigationDrawer();

            ImageButton imageButton_go_to_library = findViewById(R.id.imageButton_go_to_library);
            imageButton_go_to_library.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    goto_ChaptersActivity_AfterRequestPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Ctes.RC_PERMISSION_EXTERNAL_STORAGE,
                            "Write External Storage",
                            "Playtext need your Write External Storage Permission to read and write in to your device. \n" +
                                    "You can grant them in App Settings page."
                    );
                }
            });

            displayFCMinDashboard();
        }
    }

    private void googleSignOut() {
        // signOut
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        backupDatabases(dashboardActivity);
                    }
                });
    }



    public void backupDatabases(Context context) {
//        showProgressBar(progressBar, true);
        Observable.just("")
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {

                        String source = databaseFolderPath(context);
                        String destination = backupFolderPath(context) + File.separator + firebaseUser.getUid() + ".zip";

                        Log.i(TAG, "asas backupDatabases: source: " + source);
                        Log.i(TAG, "asas backupDatabases: destination: " + destination);

                        List<String> filesListInDir = new ArrayList<>();
                        File sourceFile = new File(source);
                        File[] files = sourceFile.listFiles();
                        for (File file : files) {
                            Log.i(TAG, "populateFilesList: " + file.getName());
                            if (file.isFile()) {
                                filesListInDir.add(file.getAbsolutePath());
                            }
                        }

                        // now zip files one by one
                        // create ZipOutputStream to write to the zip file
                        FileOutputStream fos = new FileOutputStream(destination);
                        ZipOutputStream zos = new ZipOutputStream(fos);
                        for (String filePath : filesListInDir) {
                            //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                            ZipEntry ze = new ZipEntry(filePath.substring(sourceFile.getAbsolutePath().length() + 1));
                            zos.putNextEntry(ze);
                            //read the file and write to ZipOutputStream
                            FileInputStream fis = new FileInputStream(filePath);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                            zos.closeEntry();
                            fis.close();
                        }
                        zos.close();
                        fos.close();

                        return "";
                    }

                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i(TAG, "onSubscribe: ");
                    }

                    @Override
                    public void onNext(String folders) {
                        Log.i(TAG, "onNext: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "onError: ");
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG, "onComplete: ");
                        hideProgressBar(progressBar);
                        deleteDatabase(databaseFolderPath(dashboardActivity));
                        FirebaseAuth.getInstance().signOut();
                        Go.reload(dashboardActivity);
                    }
                });
    }



    private void runOnlyOnce() {
        Cache runOnlyOnce = new Cache("runOnlyOnce", this);
        boolean isFirstRun = runOnlyOnce.getBoolean("Dashboard_Activity_First_Run", true);

        if (isFirstRun) {
            // Code to run once -----------------------------------------------------------------

            setSort(cache_ChaptersActivity, "title");
            setOrder(cache_ChaptersActivity, "ASC");

            setSort(cache_StoreActivity, "status");
            setOrder(cache_StoreActivity, "DESC");

            setSort(cache_GutenbergActivity, "title");
            setOrder(cache_GutenbergActivity, "ASC");


            setSort(cache_DownloadsActivity, "title");
            setOrder(cache_DownloadsActivity, "ASC");

            setSort(cache_FolderActivity, "title");
            setOrder(cache_FolderActivity, "ASC");


            try {
//                FileUtils.forceMkdir(new java.io.File(qrcodeFolderPath(this)));
                FileUtils.forceMkdir(new java.io.File(backupFolderPath(this)));
            } catch (IOException e) {
                e.printStackTrace();
            }


            Log.i(TAG, "signOut 5: signOutDialog: createEmptyDbs:");


            // End of Code to run once
            // ----------------------------------------------------------------------------------
            runOnlyOnce.putBoolean("Dashboard_Activity_First_Run", false);
        }
    }

    public void goto_ChaptersActivity_AfterRequestPermission(String permission, int requestCode, String denialDialogTitle, String denialDialogMessage) {

        Dexter.withActivity(dashboardActivity)
                .withPermission(permission)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // we need permission for:


                        // traveling 1a dashboardActivity >>>> ChaptersActivity
                        Log.i(TAG, "traveling 1a dashboardActivity >>>> ChaptersActivity");

                        Go.dashboard_to_chapters(dashboardActivity);

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        // check for permanent denial of permission
                        if (response.isPermanentlyDenied()) {

                            /**
                             * Showing Alert Dialog with Settings option
                             * Navigates user to app settings
                             * NOTE: Keep proper title and message depending on your app
                             */
                            AlertDialog.Builder builder = new AlertDialog.Builder(dashboardActivity);
                            builder.setTitle(denialDialogTitle);
                            builder.setMessage(denialDialogMessage);
                            builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();

                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivityForResult(intent, requestCode);
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            builder.show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        showMessage("Error occurred! " + error.toString());
                    }
                })
                .check();

    }

    private void displayToolbar() {
        toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle(firebaseUser.getDisplayName());
        toolbar.setSubtitle(firebaseUser.getEmail());

        setSupportActionBar(toolbar);

    }

    private void setNavigationDrawer() {

        buildUserProfile();
        AccountHeader accountHeader = buildAccountHeader();
        buildDrawer(accountHeader);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.item_announces_badge).setVisible(isAnnounceIconVisible());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        getMenuInflater().inflate(R.menu.announce, menu);
        displayAnnouncesBadge(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.item_announces_badge) {
            setAnnounceIconVisible(false);
            invalidateOptionsMenu();
            Go.GoToAnnouncesActivity(dashboardActivity);
            setBack_to("AnnouncesActivity");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * FirebaseUsers has clicked the 'Invite' button, launch the invitation UI with the proper
     * title, message, and deep link
     */
    // [START on_invite_clicked]
    // mySteps 1 Start by building an Intent using the AppInviteInvitation.IntentBuilder class:
    private void onInviteClicked() {
        Log.i(TAG, "onInviteClicked: mySteps 1");
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(Ctes.PLAYTEXT_IO_DEEP_LINK))
                .setCustomImage(Uri.parse(Ctes.INVITATION_CUSTOM_IMAGE))
                .setCallToActionText(getString(R.string.invitation_cta))
                .setGoogleAnalyticsTrackingId("Paragraph Invitation")
                .build();
        startActivityForResult(intent, Ctes.REQUEST_INVITE);
    }

    // [START on_activity_result]
    // mySteps 2 Launching the AppInviteInvitation intent opens the contact chooser where the user selects
    // the contacts to invite. Invites are sent via email or SMS. After the user chooses contacts and sends
    // the invite, your app receives a callback to onActivityResult:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult: mySteps 2: requestCode: " + requestCode);
        Log.i(TAG, "onActivityResult:  mySteps 2: resultCode: " + resultCode);

        if (requestCode == Ctes.REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {

                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);

                // Check how many invitations were sent.
                Log.i(TAG, "onActivityResult:  mySteps 2: ids.length: " + ids.length);

                // Get the invitation IDs of all sent messages
                for (String id : ids) {
                    Log.i(TAG, "onActivityResult:  mySteps 2: id " + id);
                }
            } else {
                // Sending failed or it was canceled, show failure message to the user
                // [START_EXCLUDE]
//                MyClass.showMessage(coordinatorLayout, getString(R.string.google_play_services_error));
//                MyClass.showToastMessage(dashboardActivity, getString(R.string.google_play_services_error));
                Toast.makeText(dashboardActivity, getString(R.string.google_play_services_error), Toast.LENGTH_SHORT).show();

                Log.i(TAG, "onActivityResult:  mySteps 2: Sending failed ");


                // [END_EXCLUDE]
            }
        }

        if (requestCode == Ctes.RC_PERMISSION_EXTERNAL_STORAGE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            showMessage(getString(R.string.returned_from_app_settings_to_activity));
        }


        if (requestCode == Ctes.RC_APPEARANCE) {
            if (resultCode == RESULT_OK) {
                //Write your code if there's a result
                Log.i(TAG, "onActivityResult: resultCode: " + resultCode);

                int actualMode = cache_appearance.getInt("mode", AppCompatDelegate.MODE_NIGHT_NO);
                getDelegate().setLocalNightMode(actualMode);
                Go.reload(this);


            }
            if (resultCode == RESULT_CANCELED) {
                Log.i(TAG, "onActivityResult: resultCode: " + resultCode);

            }
        }


    }

    private void buildUserProfile() {
        //initialize and create the image loader logic
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder, String tag) {
                Picasso.get().
                        load(uri).
                        transform(new CircleTransform()).
                        into(imageView);

            }

            @Override
            public void cancel(ImageView imageView) {

            }

            @Override
            public Drawable placeholder(Context ctx, String tag) {
                if (DrawerImageLoader.Tags.PROFILE.name().equals(tag)) {
                    return DrawerUIUtils.getPlaceHolder(ctx);
                } else if (DrawerImageLoader.Tags.ACCOUNT_HEADER.name().equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(com.mikepenz.materialdrawer.R.color.primary).sizeDp(56);
                } else if ("customUrlItem".equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(R.color.md_red_500).sizeDp(56);
                }


                return super.placeholder(ctx, tag);
            }
        });
    }

    private AccountHeader buildAccountHeader() {


        AccountHeaderBuilder accountHeaderBuilder = new AccountHeaderBuilder();
        accountHeaderBuilder.withActivity(this);


        // khatar Header Background
//        accountHeaderBuilder.withHeaderBackground(primaryColor(myTheme));


        addSignInUserToHeader(firebaseUser, accountHeaderBuilder);

        addSignOutButtonToHeader(accountHeaderBuilder);

        addDeleteAccountButtonToHeader(accountHeaderBuilder);


        accountHeaderBuilder.withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
            @Override
            public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                return false;
            }
        });

        return accountHeaderBuilder.build();
    }

    private void buildDrawer(AccountHeader accountHeader) {

        Log.i(TAG, "getMyLimit 3: getMyLimit(): " + getMyLimit());

//        Drawer Items

        mode = new PrimaryDrawerItem()
                .withTag(R.string.mode_and_theme)
                .withName(R.string.mode_and_theme)
                .withIcon(FontAwesome.Icon.faw_moon);


        statistics = new PrimaryDrawerItem()
                .withTag(R.string.statistics)
                .withName(R.string.statistics)
                .withIcon(FontAwesome.Icon.faw_chart_bar);


        version = new PrimaryDrawerItem()
                .withTag(R.string.light_version)
                .withName(R.string.light_version)
                .withIcon(FontAwesome.Icon.faw_cart_plus)
                .withDescriptionTextColorRes(R.color.colorAccent);


        about = new PrimaryDrawerItem()
                .withTag(R.string.about)
                .withName(R.string.about)
                .withIcon(FontAwesome.Icon.faw_info_circle);


        announces = new PrimaryDrawerItem()
                .withTag(R.string.announces)
                .withName(R.string.announces)
                .withIcon(FontAwesome.Icon.faw_bullhorn);

        buyMore = new PrimaryDrawerItem()
                .withTag(R.string.buy_more)
                .withName(R.string.buy_more)
                .withIcon(FontAwesome.Icon.faw_cart_plus);

        feedback = new PrimaryDrawerItem()
                .withTag(R.string.feedback)
                .withName(R.string.feedback)
                .withIcon(FontAwesome.Icon.faw_envelope);

        rate = new PrimaryDrawerItem()
                .withTag(R.string.rate)
                .withName(R.string.rate)
                .withIcon(FontAwesome.Icon.faw_star_half);

        privacy_policy = new PrimaryDrawerItem()
                .withTag(R.string.privacy_policy)
                .withName(R.string.privacy_policy)
                .withIcon(FontAwesome.Icon.faw_lock);

        myTest = new PrimaryDrawerItem()
                .withTag(R.string.test)
                .withName(R.string.test)
                .withIcon(FontAwesome.Icon.faw_taxi);

        close = new PrimaryDrawerItem()
                .withTag(R.string.close)
                .withName(R.string.close)
                .withIcon(FontAwesome.Icon.faw_door_closed);

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withTranslucentStatusBar(false)
                .withActionBarDrawerToggle(true)
                .withAccountHeader(accountHeader)
                .withSelectedItem(-1)
                .withCloseOnClick(true)
                .withDelayDrawerClickEvent(Ctes.DELAY_DRAWER_CLICK_EVENT)
                .addDrawerItems(
                        version,
                        mode,
                        statistics,
                        buyMore,
                        new SectionDrawerItem().withName(R.string.contactUs),
                        announces,
                        rate,
                        feedback,
                        new SectionDrawerItem().withName(R.string.about),
                        about,
                        privacy_policy,
                        myTest,
                        close

                )

                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {

                        if (drawerItem != null) {
                            if (drawerItem instanceof Nameable) {
                                String drawer_item_name = ((Nameable) drawerItem).getName().getText(dashboardActivity);

                                if (drawer_item_name.equals(getResources().getString(R.string.about))) {

                                    int actualMode = cache_appearance.getInt("mode", AppCompatDelegate.MODE_NIGHT_NO);


                                    Libs.ActivityStyle about_style = null;


                                    switch (actualMode) {
                                        case AppCompatDelegate.MODE_NIGHT_NO:
                                            about_style = Libs.ActivityStyle.LIGHT_DARK_TOOLBAR;
                                            break;
                                        case AppCompatDelegate.MODE_NIGHT_YES:
                                            about_style = Libs.ActivityStyle.DARK;
                                            break;
                                    }

                                    new LibsBuilder()
                                            .withActivityStyle(about_style)
                                            .withAboutIconShown(true)
                                            .withAboutVersionShown(true)
                                            .withAboutDescription(getString(R.string.about_description))
                                            .withLicenseDialog(true)
                                            .withLicenseShown(true)
//                                            .withActivityTheme(R.style.MaterialTheme_ActionBar_DayNight)
                                            .start(dashboardActivity);

                                } else if (drawer_item_name.equals(getResources().getString(R.string.mode_and_theme))) {

                                    Intent intent = new Intent(dashboardActivity, AppearanceActivity.class);
                                    startActivityForResult(intent, Ctes.RC_APPEARANCE);
                                    drawer.deselect();

                                } else if (drawer_item_name.equals(getResources().getString(R.string.statistics))) {

                                    Intent intent = new Intent(dashboardActivity, StatisticsActivity.class);
                                    startActivity(intent);
                                    drawer.deselect();

                                } else if (drawer_item_name.equals(getResources().getString(R.string.light_version))) {


                                    String[] upgrade = getResources().getStringArray(R.array.upgrade_your_limit);

                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(dashboardActivity);
                                    builder.setIcon(R.drawable.ic_sort);
                                    builder.setTitle(R.string.upgrade);
                                    builder.setSingleChoiceItems(upgrade, -1, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                                            if (selectedIndex == 0) {
                                                Go.to(dashboardActivity, BuyMoreActivity.class);
                                                drawer.deselect();

                                            } else if (selectedIndex == 1) {


                                            }

                                        }
                                    });
                                    builder.create();
                                    builder.show();


                                    drawer.deselect();

                                } else if (drawer_item_name.equals(getResources().getString(R.string.announces))) {
                                    Go.GoToAnnouncesActivity(dashboardActivity);
                                    setBack_to("AnnouncesActivity");
                                    drawer.deselect();
                                } else if (drawer_item_name.equals(getResources().getString(R.string.feedback))) {
                                    sendFeedback();
                                    drawer.deselect();
                                } else if (drawer_item_name.equals(getResources().getString(R.string.rate))) {
                                    AppRater.rateNow(dashboardActivity);
                                    drawer.deselect();
                                } else if (drawer_item_name.equals(getResources().getString(R.string.privacy_policy))) {
                                    Go.to(dashboardActivity, PrivacyPolicyActivity.class);
                                    drawer.deselect();
                                } else if (drawer_item_name.equals(getResources().getString(R.string.test))) {
                                    Go.to(dashboardActivity, SettingsActivity.class);
                                    drawer.deselect();
                                } else if (drawer_item_name.equals(getResources().getString(R.string.close))) {
                                    finishAskDialog();
                                    drawer.deselect();
                                } else if (drawer_item_name.equals(getResources().getString(R.string.buy_more))) {
                                    Go.to(dashboardActivity, BuyMoreActivity.class);
                                    drawer.deselect();
                                }
                            }
                        }
                        return true;
                    }
                })
                .build();


//        result.deselect();
//        result.setSelection(1);
    }

    private void addDeleteAccountButtonToHeader(AccountHeaderBuilder accountHeaderBuilder) {
        accountHeaderBuilder.addProfiles(new ProfileSettingDrawerItem()
                .withName(getString(R.string.delete_account))
                .withTextColor(getResources().getColor(R.color.md_red_900))
//                .withIcon(CommunityMaterial.Icon.cmd_delete_forever)
                .withIcon(FontAwesome.Icon.faw_clock)
                .withIconColor(getResources().getColor(R.color.md_red_900))
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        drawer.closeDrawer();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                deleteAccountDialog();
                            }
                        }, Ctes.DELAY_DRAWER_CLICK_EVENT);

                        return true;
                    }
                }));
    }

    private void addSignOutButtonToHeader(AccountHeaderBuilder accountHeaderBuilder) {
        accountHeaderBuilder.addProfiles(new ProfileSettingDrawerItem()
                .withName(getString(R.string.drawer_item_sign_out))
                .withIcon(FontAwesome.Icon.faw_sign_out_alt)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        drawer.closeDrawer();

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                signOutDialog();
                            }
                        }, Ctes.DELAY_DRAWER_CLICK_EVENT);

                        return true;
                    }
                }));
    }

    private void addSignInUserToHeader(FirebaseUser firebaseUser, AccountHeaderBuilder accountHeaderBuilder) {

        accountHeaderBuilder.addProfiles(new ProfileDrawerItem()
                .withName(firebaseUser.getDisplayName())
                .withEmail(firebaseUser.getEmail())
                .withIcon(firebaseUser.getPhotoUrl()));
    }

    private void setAppRater() {
        AppRater.app_launched(this);
        AppRater.setVersionNameCheckEnabled(true);
        //AppRater.rateNow(this);
        AppRater.setMarket(new GoogleMarket());
        //AppRater.setMarket(new AmazonMarket());
    }

    public void finishAskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.are_you_sure_you_want_to_exit))
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        dashboardActivity.finish();
//                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.i(TAG, "onConnectionFailed:" + connectionResult);

        View view = findViewById(R.id.sign_in_button);
        Snackbar google_play_services_error = Snackbar.make(view, "Google Play Services error.", Snackbar.LENGTH_LONG);
        google_play_services_error.show();
    }

    private void sendFeedback() {
        final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", Ctes.FEEDBACK_EMAIL, null));
        intent.putExtra(Intent.EXTRA_SUBJECT, AppInfo.createAppInfo(this).getAppName());
        dashboardActivity.startActivity(Intent.createChooser(intent, getString(R.string.Choose_your_client)));
    }

    public void signOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sign_out);
        builder.setIcon(R.drawable.ic_qrcode);
        builder.setMessage(getString(R.string.are_you_sure_you_want_to_sign_out));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.sign_out, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                googleSignOut();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create();
        builder.show();
    }

    public void deleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_account);
        builder.setIcon(R.drawable.delete);
        builder.setMessage(getString(R.string.are_you_sure_you_want_to_sign_out_and_delete_account));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showProgressBar(progressBar, true);
                // Google revoke access
                Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {

                                FirebaseFirestore
                                        .getInstance()
                                        .collection(Ctes.USERS)
                                        .document(Objects.requireNonNull(firebaseUser.getEmail()))
                                        .update(Ctes.PENDING_FOR_DELETE, true, Ctes.DELETE_TIMESTAMP, System.currentTimeMillis())
                                        .addOnSuccessListener(dashboardActivity, new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i(TAG, "You are now pending for delete!");
                                                backupDatabases(dashboardActivity);
                                            }
                                        })
                                        .addOnFailureListener(dashboardActivity, new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                hideProgressBar(progressBar);

                                                showMessage("Fail to put you pending for delete! with message: " + e.getMessage());
                                                Log.i(TAG, "signIn: 2b - Google SignIn on failure with message: " + e.getMessage());

                                            }
                                        });


                            }
                        });
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                dialog.cancel();
            }
        });
        builder.create();
        builder.show();
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        finishAskDialog();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(IconicsContextWrapper.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }
}