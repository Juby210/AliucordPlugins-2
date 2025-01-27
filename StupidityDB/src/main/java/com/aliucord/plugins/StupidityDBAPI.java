package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.widget.Toast;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.models.domain.NonceGenerator;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreInviteSettings;
import com.discord.stores.StoreStream;
import com.discord.utilities.rest.RestAPI;
import com.discord.utilities.time.ClockFactory;
import com.discord.widgets.guilds.invite.WidgetGuildInvite;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class StupidityDBAPI {
    //static String serverip="http://192.168.1.35"; local ip used for testing
    static String serverip = "https://mantikralligi1.pythonanywhere.com";
    static long botid = Long.parseLong("915703782174752809");
    public static Cache cache = new Cache();

    public static String getUserData(long userID) {

        try {
            if (cache.isCached(userID)) return cache.getCached(userID);

            String url = serverip + "/getuser?discordid=" + userID;

            String res = Http.simpleGet(url);
            if (res != null) cache.setUserCache(userID, res); // if some http error etc happens dont save it to cache
            return res;
        } catch (IOException e) {
            new Logger("StupidityDB").error(e);
            return null;
        }

    }

    public static String sendUserData(int stupidity, long id) {
        if (StupidityDB.staticSettings.getBool("useOAUTH2", true)) {
            return sendUserDataWithHttp(stupidity, id);
        } else {
            return sendUserDataDiscord(stupidity, id);
        }
    }

    public static boolean isUserinServer() {
        return !(StoreStream.getGuilds().getGuild(Long.parseLong("917308687423533086")) == null);
    }

    public static String sendUserDataDiscord(int stupidity, long id) {
        if (!isUserinServer()) {
            Toast.makeText(Utils.getAppActivity(), "You need to join server to send votes", Toast.LENGTH_SHORT).show();
            WidgetGuildInvite.Companion.launch(Utils.getAppActivity(), new StoreInviteSettings.InviteCode("uJbMFWjMUt", "", null));
            return "Accept Invite and try again";
        } else {
            RxUtils.subscribe(RestAPI.getApi().createOrFetchDM(botid), channel -> {
                RxUtils.subscribe(RestAPI.getApi().sendMessage(ChannelWrapper.getId(channel), createMessage(stupidity, id)), message -> null);
                return null;
            });
            return "Vote Sent";
        }
    }

    public static String sendUserDataWithHttp(int stupidity, long id) {
        if (StupidityDB.staticSettings.getString("token", null) != null) {
            String url = serverip + "/vote";

            try {
                var json = new JSONObject()
                        .put("token", StupidityDB.staticSettings.getString("token", null))
                        .put("discordid", id)
                        .put("stupidity", stupidity);
                return Http.simplePost(url, json.toString());
            } catch (JSONException | IOException e) {
                new Logger("StupidityDBAPI").error(e);
                return "An Error Occured";
            }
        } else {
            Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());

        }
        return "Token is null,Please Authorize";
    }

    public static RestAPIParams.Message createMessage(int stupidity, long id) {
        JSONObject obj = new JSONObject();
        try {
            obj = new JSONObject().put("stupidity", stupidity).put("discordid", id);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new RestAPIParams.Message(
                obj.toString(), // Content
                String.valueOf(NonceGenerator.computeNonce(ClockFactory.get())), // Nonce
                null, // ApplicationId
                null, // Activity
                emptyList(), // stickerIds
                null, // messageReference
                new RestAPIParams.Message.AllowedMentions( // https://discord.com/developers/docs/resources/channel#allowed-mentions-object-allowed-mentions-structure
                        emptyList(), // parse
                        emptyList(), //users
                        emptyList(), // roles
                        false // repliedUser

                ),null,null
        );

    }
}
