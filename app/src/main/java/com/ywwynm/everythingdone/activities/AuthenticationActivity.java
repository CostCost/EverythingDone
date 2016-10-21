package com.ywwynm.everythingdone.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;

import com.ywwynm.everythingdone.App;
import com.ywwynm.everythingdone.Def;
import com.ywwynm.everythingdone.R;
import com.ywwynm.everythingdone.helpers.AuthenticationHelper;
import com.ywwynm.everythingdone.helpers.RemoteActionHelper;
import com.ywwynm.everythingdone.model.Thing;

/**
 * Created by ywwynm on 2016/6/21
 * An Activity used when user operated a private thing.
 */
public class AuthenticationActivity extends AppCompatActivity {

    public static Intent getOpenIntent(
            Context context, String senderName, long id, int position,
            String action, String actionTitle) {
        final Intent intent = new Intent(context, AuthenticationActivity.class);
        intent.setAction(action);
        intent.putExtra(Def.Communication.KEY_SENDER_NAME, senderName);
        intent.putExtra(Def.Communication.KEY_DETAIL_ACTIVITY_TYPE,
                DetailActivity.UPDATE);
        intent.putExtra(Def.Communication.KEY_ID, id);
        intent.putExtra(Def.Communication.KEY_POSITION, position);
        intent.putExtra(Def.Communication.KEY_TITLE, actionTitle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        Intent intent = getIntent();

        long id = intent.getLongExtra(Def.Communication.KEY_ID, -1);
        int position = intent.getIntExtra(Def.Communication.KEY_POSITION, -1);

        Pair<Thing, Integer> pair = App.getThingAndPosition(this, id, position);

        if (pair.first == null) {
            finish();
            return;
        }

        tryToAuthenticate(pair.first, pair.second);
    }

    private void tryToAuthenticate(final Thing thing, final int position) {
        Intent intent = getIntent();
        final String action = intent.getAction();
        if (thing.isPrivate()) {
            String cp = getSharedPreferences(Def.Meta.PREFERENCES_NAME, MODE_PRIVATE)
                    .getString(Def.Meta.KEY_PRIVATE_PASSWORD, null);
            if (cp == null) {
                // I hope this will never happen, directly act for the time being
                act(action, thing, position);
                return;
            }

            String title = intent.getStringExtra(Def.Communication.KEY_TITLE);
            int color = thing.getColor();
            AuthenticationHelper.authenticate(
                    this, color, title, cp,
                    new AuthenticationHelper.AuthenticationCallback() {
                        @Override
                        public void onAuthenticated() {
                            act(action, thing, position);
                        }

                        @Override
                        public void onCancel() {
                            finish();
                            overridePendingTransition(0, 0);
                        }
                    });
        } else {
            act(action, thing, position);
        }
    }

    private void act(String action, Thing thing, int position) {
        if (Def.Communication.AUTHENTICATE_ACTION_FINISH.equals(action)) {
            actFinish(thing, position);
        } else if (Def.Communication.AUTHENTICATE_ACTION_DELAY.equals(action)) {
            Intent intent = DelayReminderActivity.getOpenIntent(
                    this, thing.getId(), position, thing.getColor());
            startActivity(intent);
        } else {
            actView();
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private void actFinish(Thing thing, int position) {
        if (thing.getType() != Thing.HABIT) { // reminder or goal
            RemoteActionHelper.finishReminder(this, thing, position);
        } else {
            long time = getIntent().getLongExtra(Def.Communication.KEY_TIME, 0);
            RemoteActionHelper.finishHabitOnce(this, thing, position, time);
        }
    }

    private void actView() {
        Intent intent = getIntent();
        intent.setClass(this, DetailActivity.class);
        startActivity(intent);
    }
}
