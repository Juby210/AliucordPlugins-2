package com.aliucord.plugins;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.plugins.dataclasses.Response;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.plugins.dataclasses.User;
import com.aliucord.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;

public class UserReviewsAPI {

    public static final String API_URL = "https://manti.vendicated.dev";

    public static List<Review> getReviews(long userid) {
        try {
            String response = Http.simpleGet(API_URL + "/getUserReviews?discordid=" + userid);
            return GsonUtils.fromJson(response, TypeToken.getParameterized(List.class, Review.class).type);
        } catch (IOException e) {
            UserReviews.logger.error(e);
            return null;
        }
    }

    public static int getLastReviewID(long userid) {
        try {
            return Integer.parseInt(Http.simpleGet(API_URL +"/getLastReviewID?discordid=" + userid));
        } catch (IOException | NumberFormatException e) {
            UserReviews.logger.error(e);
            return 0;
        }
    }

    public static String reportReview(String token,int reviewID) {
        JSONObject json = new JSONObject();
        try {
            json.put("token",token);
            json.put("reviewid",reviewID);
            return Http.simplePost(API_URL +"/reportReview",json.toString());
        } catch (JSONException | IOException e) {
            UserReviews.logger.error(e);
            return "An Error Occured";
        }
    }

    public static Response addReview(String comment, Long userid, String token) {
        try {
            JSONObject json = new JSONObject();
            json.put("comment", comment);
            json.put("star", -1);
            json.put("token", token);
            json.put("userid", userid);
            var response = Http.simplePost(API_URL + "/addUserReview", json.toString());

            if (response.equals("Updated your review")) {
                return new Response(true, true, response);
            } else if (response.equals("Added your review")) {
                return new Response(false, true, response);
            } else {
                return new Response(false, false, response);
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            new Logger("guh").error(e);
            return new Response(false, false, "An Error Occured");
        }
    }


}
