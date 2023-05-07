package com.aliucord.plugins;

import static com.aliucord.plugins.UserReviewsAPI.AUTH_URL;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.views.Button;
import com.aliucord.widgets.BottomSheet;
import com.discord.app.AppActivity;
import com.discord.restapi.RestAPIParams;
import com.discord.views.CheckedSetting;
import com.discord.widgets.auth.WidgetOauth2Authorize;
import com.discord.widgets.auth.WidgetOauth2Authorize$authorizeApplication$2;

public class BottomShit extends BottomSheet {
    SettingsAPI settings;


    public BottomShit(SettingsAPI settings) {
        this.settings = settings;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var context = requireContext();
        setPadding(20);

        TextView title = new TextView(context, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        title.setText("UserReviews");
        title.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        title.setGravity(Gravity.START);

        Button crashing = new Button(context);
        crashing.setText("Crashing?");
        crashing.setOnClickListener(v -> {
            var dialog = new InputDialog();
            dialog.setOnDialogShownListener(view1 -> {
                dialog.setTitle("WARNING");
                dialog.getBody().setText("If your aliucord is crashing during authorization, long click the 'Enter Token Manually' button in settings and it will redirect you to the API to get a token. After you get your token click the 'Enter Token Manually' button again and paste it in there.");
                dialog.getInputLayout().setVisibility(View.GONE);
            });

            dialog.show(getParentFragmentManager(), "fart");

        });

        Button authorizate = new Button(context);
        authorizate.setText("Authorize");
        authorizate.setOnClickListener(oc -> UserReviewsAPI.authorize());

        Button enterTokenManually = new Button(context);
        enterTokenManually.setText("Enter OAUTH Token Manually");
        enterTokenManually.setOnClickListener(oc -> {
            var dialog = new InputDialog().setTitle("Enter Token").setDescription("Long click to get token (Discord sometimes ratelimits API requests so if you're getting an error that's probably why)");
            dialog.setOnOkListener(v -> {
                var token = dialog.getInput();
                if (!token.equals("")) settings.setString("token", token);
                else
                    Toast.makeText(context, "Please Enter Token", Toast.LENGTH_SHORT).show();
            });
            dialog.show(getParentFragmentManager(), "uga");
        });
        enterTokenManually.setOnLongClickListener(v -> {
            Utils.launchUrl(AUTH_URL);
            return true;
        });

        var notifyNewReviews = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Notify for new reviews", "");
        notifyNewReviews.setChecked(settings.getBool("notifyNewReviews", true));

        notifyNewReviews.setOnCheckedListener(aBoolean -> {
            settings.setBool("notifyNewReviews", aBoolean);

        });

        var disableAds = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Disables Ads in Reviews", "");
        disableAds.setChecked(settings.getBool("disableAds", false));

        disableAds.setOnCheckedListener(aBoolean -> {
            settings.setBool("disableAds", aBoolean);
        });

        var disableWarnings = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Disables Warnings in Reviews", "You will still get banned if you do stupit");
        disableWarnings.setChecked(settings.getBool("disableWarnings", false));

        disableWarnings.setOnCheckedListener(aBoolean -> {
            settings.setBool("disableWarnings", aBoolean);
        });

        addView(title);
        addView(crashing);
        addView(authorizate);
        addView(enterTokenManually);
        addView(notifyNewReviews);
        addView(disableAds);
        addView(disableWarnings);


    }
}
