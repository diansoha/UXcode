package io.playtext.playtext.login;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.playtext.playtext.R;
import io.playtext.playtext.base.BaseActivity;
import io.playtext.playtext.dashboard.DashboardActivity;
import io.playtext.playtext.tools.Ctes;
import io.playtext.playtext.tools.Go;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class GoogleSignInActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    public String TAG = getClass().getSimpleName();
    public GoogleSignInActivity googleSignInActivity;
    private ProgressBar progressBar;
//    private MyUser myNewUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        googleSignInActivity = this;

        progressBar = findViewById(R.id.my_progressBar);

        TextView textView = findViewById(R.id.textView5);
        textView.setText(getString(R.string.loremIpsum));
        textView.append("\n");
        textView.append(Html.fromHtml("<a href=\"" + Ctes.PRIVACY_POLICY + "\">Privacy Policy</a> "));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignin();
            }
        });
    }

    private void googleSignin() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(googleSignInActivity, googleSignInOptions);
        mGoogleSignInClient.signOut();

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Ctes.RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Ctes.RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                showProgressBar(progressBar, true);
                GoogleSignIn
                        .getSignedInAccountFromIntent(data)
                        .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                            @Override
                            public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                                firebaseSignInWithGoogleCredential(googleSignInAccount);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                hideProgressBar(progressBar);
                                showMessage("Get signed in account from intent failed with message: " + e.getMessage());
                            }
                        });
            }
        }
    }

    private void firebaseSignInWithGoogleCredential(GoogleSignInAccount googleSignInAccount) {
        FirebaseAuth
                .getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null))
                .addOnSuccessListener(googleSignInActivity, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser firebaseUser = authResult.getUser();
                        handleSignInResult(firebaseUser);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressBar(progressBar);
                        showMessage("Signin with google credential failed with message: " + e.getMessage());
                    }
                });
    }

    private void handleSignInResult(FirebaseUser firebaseUser) {
        FirebaseFirestore
                .getInstance()
                .collection(Ctes.USERS)
                .document(Objects.requireNonNull(firebaseUser.getEmail()))
                .get()
                .addOnSuccessListener(googleSignInActivity, new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        Log.i(TAG, "onSuccess: asas: documentSnapshot.exists(): " + documentSnapshot.exists());
                        // if firebaseUser does not exist (first device)
                        if (!documentSnapshot.exists()) {
                            addNewUser(firebaseUser);
                            setMyLibraryCountLimit(Ctes.DEFAULT_ITEMS_LIMIT);

                            // if firebaseUser exist (second device)
                        } else {
                            if (Boolean.parseBoolean(Objects.requireNonNull(documentSnapshot.get(Ctes.PENDING_FOR_DELETE)).toString())) {
                                long you_have;
                                you_have = (Ctes.DAYS_BEFORE_ACCOUNT_DELETE - (System.currentTimeMillis() - Long.parseLong(Objects.requireNonNull(documentSnapshot.get(Ctes.DELETE_TIMESTAMP)).toString())) / 1000 / 60 / 60 / 24);
//                                    you_have = (20 - (System.currentTimeMillis() - Long.parseLong(Objects.requireNonNull(documentSnapshot.get(Ctes.DELETE_TIMESTAMP)).toString()))/1000);
                                if (you_have < 0) {
                                    deletePermanentlyYourAccount(firebaseUser);

                                    AlertDialog.Builder builder = new AlertDialog.Builder(googleSignInActivity);
                                    builder.setIcon(R.drawable.ic_delete_forever);
                                    builder.setTitle(R.string.delete_account_pending);
                                    builder.setMessage("Your account have been deleted automatically.");
                                    builder.create();
                                    builder.show();

                                } else {
                                    deletePendingDialog(you_have, firebaseUser);
                                }

                            } else {

                                Log.i(TAG, "onSuccess: asas: firebaseUser.getUid() " + firebaseUser.getUid());
                                updateExistingUser(firebaseUser);
                                setMyLibraryCountLimit(Integer.parseInt(Objects.requireNonNull(documentSnapshot.get(Ctes.ITEMS_LIMIT)).toString()));
                            }
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressBar(progressBar);
                        showMessage("Get document snapshot failed with message: " + e.getMessage());
                    }
                });
    }

    private void deletePermanentlyYourAccount(FirebaseUser firebaseUser) {
        showProgressBar(progressBar, true);
        FirebaseFirestore
                .getInstance()
                .collection(Ctes.USERS)
                .document(Objects.requireNonNull(firebaseUser.getEmail()))
                .delete()
                .addOnSuccessListener(googleSignInActivity, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Objects.requireNonNull(FirebaseAuth
                                .getInstance()
                                .getCurrentUser())
                                .delete()
                                .addOnSuccessListener(googleSignInActivity, new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        FirebaseAuth.getInstance().signOut();
                                        hideProgressBar(progressBar);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        hideProgressBar(progressBar);
                                        Log.i(TAG, "Delete firebase user failed with message: " + e.getMessage());
                                        showMessage("Delete firebase user failed with message: " + e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressBar(progressBar);
                        Log.i(TAG, "Delete user from database failed with message: " + e.getMessage());
                        showMessage("Delete user from database failed with message: " + e.getMessage());
                    }
                });
    }

    private void deletePendingDialog(long you_have, FirebaseUser firebaseUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(googleSignInActivity);
        builder.setIcon(R.drawable.ic_delete_forever);
        builder.setTitle(R.string.delete_account_pending);

        builder.setMessage("Your account delete automatically after " + you_have + " days.\nReactive your account or sign out and pending for delete.");


        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getText(R.string.reactive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG, "PositiveButton");
                showProgressBar(progressBar, true);
                FirebaseFirestore
                        .getInstance()
                        .collection(Ctes.USERS)
                        .document(Objects.requireNonNull(firebaseUser.getEmail()))
                        .update("pending_for_delete", false, Ctes.DELETE_TIMESTAMP, 0)
                        .addOnSuccessListener(googleSignInActivity, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                hideProgressBar(progressBar);
                                dialog.dismiss();
                                restoreDatabases(googleSignInActivity, firebaseUser);
                            }
                        })
                        .addOnFailureListener(googleSignInActivity, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                hideProgressBar(progressBar);
                                showMessage("Fail to put you pending for delete! with message: " + e.getMessage());
                                Log.i(TAG, "signIn: 2b - Google SignIn on failure with message: " + e.getMessage());
                            }
                        });


            }
        });
        builder.setNegativeButton(getResources().getText(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG, "NegativeButton");
                hideProgressBar(progressBar);
                dialog.dismiss();
            }
        });

        builder.create();
        builder.show();
    }


    private void addNewUser(FirebaseUser firebaseUser) {

        MyUser myNewUser = new MyUser();
        myNewUser.setDisplay_name(firebaseUser.getDisplayName());
        myNewUser.setEmail(firebaseUser.getEmail());
        myNewUser.setPhoto_url("https://lh6.googleusercontent.com" + Objects.requireNonNull(firebaseUser.getPhotoUrl()).getPath());
        myNewUser.setUid(firebaseUser.getUid());



        myNewUser.setCreation_timestamp(Objects.requireNonNull(firebaseUser.getMetadata()).getCreationTimestamp());
        myNewUser.setLast_signin_timestamp(Objects.requireNonNull(firebaseUser.getMetadata()).getCreationTimestamp());
        myNewUser.setDelete_timestamp(0);

        myNewUser.setItems_limit(Ctes.DEFAULT_ITEMS_LIMIT);
        myNewUser.setPending_for_delete(false);
        myNewUser.setEarn_1(0);
        myNewUser.setEarn_2(0);
        myNewUser.setEarn_3(0);
        myNewUser.setEarn_4(0);
        myNewUser.setEarn_5(0);
        myNewUser.setEarn_6(0);
        myNewUser.setEarn_7(0);
        myNewUser.setEarn_8(0);
        myNewUser.setEarn_9(0);

        FirebaseFirestore
                .getInstance()
                .collection(Ctes.USERS)
                .document(Objects.requireNonNull(firebaseUser.getEmail()))
                .set(myNewUser)
                .addOnSuccessListener(googleSignInActivity, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        hideProgressBar(progressBar);
                        Go.to(googleSignInActivity, DashboardActivity.class);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressBar(progressBar);
                        showMessage("Add new user failed with message: " + e.getMessage());
                    }
                });
    }

    public void updateExistingUser(FirebaseUser firebaseUser) {
        // Update last_signin_timestamp in ExistingUser
        FirebaseFirestore
                .getInstance()
                .collection(Ctes.USERS)
                .document(Objects.requireNonNull(firebaseUser.getEmail()))
                .update(Ctes.LAST_SIGNIN_TIMESTAMP, System.currentTimeMillis())
                .addOnSuccessListener(googleSignInActivity, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        hideProgressBar(progressBar);
                        restoreDatabases(googleSignInActivity, firebaseUser);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showMessage("Last signin timestamp update failed with message: " + e.getMessage());
                    }
                });
    }



    public void restoreDatabases(Context context, FirebaseUser firebaseUser) {
        showProgressBar(progressBar, true);
        Observable.just("")
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {

                        String source = backupFolderPath(context) + File.separator + firebaseUser.getUid() + ".zip";
                        Log.i(TAG, "decompressMyLibrary asas: apply: source: " + source);
                        String destination = databaseFolderPath(context);
                        Log.i(TAG, "decompressMyLibrary asas: apply: destination: databaseFolderPath(context): " + databaseFolderPath(context));

                        final File file = new File(source);
                        if (file.exists()) {
                            FileInputStream fis = new FileInputStream(source);
                            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
                            ZipEntry entry = zis.getNextEntry();

                            // iterates over entries in the zip file
                            while (entry != null) {
                                Log.i(TAG, "Download Checkup: decompressMyLibrary: entry.getName() : " + entry.getName());
                                String unzipDestinationPath = destination + File.separator + entry.getName();

                                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipDestinationPath));
                                byte[] bytesIn = new byte[Ctes.BUFFER_SIZE];
                                int read;
                                while ((read = zis.read(bytesIn)) != -1) {
                                    bos.write(bytesIn, 0, read);
                                }
                                bos.flush();
                                bos.close();

                                entry = zis.getNextEntry();
                            }
                            zis.close();
                        }

                        return "";

                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new io.reactivex.Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i(TAG, "onSubscribe: ");
                    }

                    @Override
                    public void onNext(String folders) {
                        Log.i(TAG, "onNext: " + folders);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "onError: " + e);
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG, "onComplete: ");
                        hideProgressBar(progressBar);
                        setCount(itemsCountChapters());
                        Go.to(googleSignInActivity, DashboardActivity.class);
                    }
                });

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.i(TAG, "onConnectionFailed:" + connectionResult);
//        showMessage("Google Play Services error.");
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }
}