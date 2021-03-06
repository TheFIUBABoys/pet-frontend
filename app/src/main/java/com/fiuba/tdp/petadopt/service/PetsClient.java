/**
 * Created by joaquinstankus on 06/09/15.
 * Example HTTP Client
 */

package com.fiuba.tdp.petadopt.service;

import android.util.Log;

import com.fiuba.tdp.petadopt.model.Pet;
import com.fiuba.tdp.petadopt.model.Question;
import com.fiuba.tdp.petadopt.model.User;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class PetsClient extends HttpClient {


    private String auth_token;
    private static PetsClient singletonClient;
    public String base_url = null;

    private PetsClient() {

    }

    public static PetsClient instance() {
        if (singletonClient == null) {
            singletonClient = new PetsClient();
        }
        return singletonClient;
    }

    public void setAuth_token(String auth_token) {
        this.auth_token = auth_token;
        //client.addHeader("user_token", auth_token);
    }

    public void getPetsForHome(JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets/top.json");
        RequestParams params = new RequestParams();
        client.get(url, params, handler);
    }

    public void getPet(String petId, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + petId + ".json");
        RequestParams params = new RequestParams();
        client.get(url, params, handler);
    }

    public void getQuestions(String petId, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + petId + "/questions.json");
        RequestParams params = new RequestParams();
        client.get(url, params, handler);
    }


    public void postQuestion(String id, Question question, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + id + "/questions.json");
        try {
            UTF8StringEntity entity = new UTF8StringEntity(question.toJson());

            client.post(ActivityContext, url, entity, "application/json", handler);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


    }

    public void answerQuestion(String petId, String questionId, String answer, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + petId + "/questions/" + questionId + "/answer.json");
        RequestParams params = new RequestParams();
        Map<String, String> answerMap = new HashMap<>();
        answerMap.put("body", answer);
        params.put("pet_question_answer", answerMap);
        client.post(url, params, handler);
    }

    public void getMyPets(JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets.json");
        User user = User.user();
        RequestParams params = new RequestParams();
        params.add("user_id", String.valueOf(user.getId()));
        params.add("published", String.valueOf(false));
        client.get(url, params, handler);
    }

    public void getMyRequestedPets(JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets.json");
        RequestParams params = new RequestParams();
        params.add("requested_by_me", String.valueOf(true));
        ;
        client.get(url, params, handler);
    }

    public void getAdoptersForPet(String petId, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + petId + "/adoption_requests.json");
        User user = User.user();
        RequestParams params = new RequestParams();
        client.get(url, params, handler);
    }

    @Override
    public String getApiUrl(String relativeUrl) {
        String url = super.getApiUrl(relativeUrl);

        if (auth_token != null) {
            return url + "?user_token=" + auth_token;
        }
        return url;
    }

    public void createPet(Pet pet, JsonHttpResponseHandler handler) {
        try {
            String url = getApiUrl("/pets.json");
            UTF8StringEntity entity = new UTF8StringEntity(pet.toJson());
            entity.setContentEncoding("utf8");
            client.post(ActivityContext, url, entity, "application/json", handler);
        } catch (UnsupportedEncodingException e) {
            Log.e("Error in post request", e.getLocalizedMessage());
        }
    }

    public void simpleQueryPets(String query, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets.json");
        RequestParams params = new RequestParams();
        params.put("metadata", query);
        Log.v("after intent", auth_token);
        client.get(url, params, handler);
    }

    public void advanceSearch(HashMap<String, String> petFilter, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets.json");
        RequestParams params = buildParameters(petFilter);
        Log.v("after intent", auth_token);
        client.get(url, params, handler);
    }

    private RequestParams buildParameters(HashMap<String, String> petFilter) {
        RequestParams params = new RequestParams();
        Iterator it = petFilter.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            params.add(pair.getKey().toString(), pair.getValue().toString());
        }

        return params;
    }

    public void uploadImage(String petId, String path, JsonHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + petId + "/images.json");

        RequestParams params = new RequestParams();

        try {
            params.put("pet_image[image]", new File(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

        client.post(url, params, handler);

    }

    public void askForAdoption(String petId, AsyncHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + petId + "/adoption_requests.json");
        RequestParams params = new RequestParams();
        client.post(url, params, handler);
    }

    public void markAsAdopted(String petId, String adopterId, AsyncHttpResponseHandler handler) {
        String url = getApiUrl("/pets/" + petId + "/adoption_requests/" + adopterId + "/accept.json");
        RequestParams params = new RequestParams();
        client.post(url, params, handler);
    }

    public void reportPet(Pet pet, JsonHttpResponseHandler handler) {
        try {
            String url = getApiUrl("/pets/" + pet.getId() + "/report.json");
            UTF8StringEntity entity = new UTF8StringEntity("");
            client.put(ActivityContext, url, entity, "application/json", handler);
        } catch (UnsupportedEncodingException e) {
            Log.e("Error in put request", e.getLocalizedMessage());
        }
    }

    public void reportQuestion(Pet pet, Question question, AsyncHttpResponseHandler handler) {
        try {
            String url = getApiUrl("/pets/" + pet.getId() + "/questions/" + question.getId() + "/report.json");
            UTF8StringEntity entity = new UTF8StringEntity("");
            client.put(ActivityContext, url, entity, "application/json", handler);
        } catch (UnsupportedEncodingException e) {
            Log.e("Error in put request", e.getLocalizedMessage());
        }
    }
}
