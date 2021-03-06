package com.fiuba.tdp.petadopt.model;


import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;


public class Pet {
    private String id;
    private String userId;
    private String name;
    private String age;
    private Type type;
    private Gender gender;
    private String description;
    private Boolean vaccinated;
    private Boolean published;
    private Boolean needs_transit_home;
    private Boolean pet_friendly;
    private Boolean children_friendly;
    private LatLng location;
    private String firstColor;
    private String secondColor;
    private ArrayList<String> colors;
    private Date createdAt;
    ArrayList<Image> images;
    ArrayList<String> videos;
    private ArrayList<Question> questions;
    private PublicationType publicationType;


    public Pet() {
        published = true;
        colors = new ArrayList<>();
        videos = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public Type getType() {
        return type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getNeedsTransitHome() {
        return needs_transit_home;
    }

    public void setNeedsTransitHome(Boolean needs_transit_home) {
        this.needs_transit_home = needs_transit_home;
    }

    public Boolean getPetFriendly() {
        return pet_friendly;
    }

    public void setPetFriendly(Boolean pet_friendly) {
        this.pet_friendly = pet_friendly;
    }

    public Boolean getChildrenFriendly() {
        return children_friendly;
    }

    public void setChildrenFriendly(Boolean children_friendly) {
        this.children_friendly = children_friendly;
    }

    public ArrayList<String> getVideos() {
        return videos;
    }

    public void setVideos(ArrayList<String> videos) {
        this.videos = videos;
    }

    //TODO - improve
    public void setType(String type) {
        if (type.equals("Gato")) {
            this.type = Type.Cat;
        } else {
            this.type = Type.Dog;
        }
    }

    public Gender getGender() {
        return gender;
    }

    //TODO - improve
    public void setGender(String gender) {
        if (gender.equals("Macho")) {
            this.gender = Gender.male;
        } else {
            this.gender = Gender.female;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getVaccinated() {
        return vaccinated;
    }

    public void setVaccinated(Boolean vaccinated) {
        this.vaccinated = vaccinated;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public String getColors() {
        String colors = "";
        for (int i = 0; i < this.colors.size(); i++) {
            colors = colors + this.colors.get(i);
            if (i < this.colors.size() - 1) {
                if (this.colors.get(i).lastIndexOf(",") < 0) {
                    colors = colors + ", ";
                } else {
                    colors = colors + " ";
                }
            }
        }
        return colors;
    }

    public void setFirstColor(String firstColor) {
        this.firstColor = firstColor;
        if (colors.size() < 1) {
            colors.add(0, firstColor);
        } else {
            colors.set(0, firstColor);
        }
    }

    public void setSecondColor(String secondColor) {
        this.secondColor = secondColor;
        if (colors.size() < 2) {
            colors.add(1, secondColor);
        } else {
            colors.set(1, secondColor);
        }
    }


    public String toJson() {
        Gson gson = new Gson();
        JsonElement je = gson.toJsonTree(this);
        JsonObject jo = je.getAsJsonObject();
        jo.remove("id");
        jo.remove("images");
        jo.remove("firstColor");
        jo.remove("secondColor");
        jo.remove("publicationType");

        jo.addProperty("location", String.valueOf(this.location.latitude) + "," + String.valueOf(this.location.longitude));
        jo.addProperty("colors", this.getColors());
        JsonObject result = new JsonObject();
        result.add("pet", jo);
        jo.addProperty("publication_type", publicationType.toString());
        return result.toString();
    }

    public ArrayList<Image> getImages() {
        return images;
    }

    public void loadFromJSON(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("id");
        this.userId = jsonObject.getString("user_id");
        this.name = jsonObject.getString("name");
        if (this.name.equals("null")) {
            this.name  = "";
        }
        this.description = jsonObject.getString("description");
        this.vaccinated = jsonObject.getBoolean("vaccinated");
        this.published = jsonObject.getBoolean("published");
        this.needs_transit_home = jsonObject.getBoolean("needs_transit_home");
        this.pet_friendly = jsonObject.getBoolean("pet_friendly");
        this.children_friendly = jsonObject.getBoolean("children_friendly");
        this.type = parseType(jsonObject.getString("type"));
        this.gender = parseGender(jsonObject.getString("gender"));
        this.age = jsonObject.getString("age");
        this.colors = parseColors(jsonObject.getString("colors"));
        this.images = parseImages(jsonObject.getJSONArray("images"));
        this.videos = parseVideos(jsonObject.getJSONArray("videos"));
        this.createdAt = parseDate(jsonObject.getString("created_at"));
        if (!jsonObject.getString("location").equals("")) {
            this.location = parseLocation(jsonObject.getString("location"));
        }
        this.publicationType = PublicationType.fromString(jsonObject.getString("publication_type"));
    }

    public void loadQuestionsFromJson(JSONArray questionArray) throws JSONException {
        this.questions = parseQuestions(questionArray);
    }

    private ArrayList<Question> parseQuestions(JSONArray questionArray) throws JSONException {
        ArrayList<Question> questions = new ArrayList<>(questionArray.length());
        for (int i = 0; i < questionArray.length(); i++) {
            questions.add(Question.fromJson(questionArray.getJSONObject(i)));
        }
        return questions;
    }

    @NonNull
    private LatLng parseLocation(String locationStr) throws JSONException {
        String[] coords = locationStr.split(",");
        return new LatLng(Double.parseDouble(coords[0]), Double.parseDouble((coords[1])));
    }

    private Date parseDate(String date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat parserSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        ParsePosition p = new ParsePosition(0);
        parserSDF.setTimeZone(tz);
        return parserSDF.parse(date, p);

    }

    private ArrayList<String> parseColors(String colors) {
        ArrayList<String> a = new ArrayList<>(Arrays.asList(colors.split("\\s* \\s*")));
        if (a.size() > 0) {
            firstColor = a.get(0);
        }
        if (a.size() > 1) {
            secondColor = a.get(1);
        }
        return a;
    }

    private ArrayList<Image> parseImages(JSONArray imagesArray) throws JSONException {
        ArrayList<Image> images = new ArrayList<>(imagesArray.length());
        for (int i = 0; i < imagesArray.length(); i++) {
            JSONObject imageObject = imagesArray.getJSONObject(i);
            Image image = new Image();
            image.fromJson(imageObject);
            images.add(image);
        }

        return images;
    }

    private ArrayList<String> parseVideos(JSONArray imagesArray) throws JSONException {
        ArrayList<String> videos = new ArrayList<>(imagesArray.length());
        for (int i = 0; i < imagesArray.length(); i++) {
            JSONObject imageObject = imagesArray.getJSONObject(i);
            String url = imageObject.getString("url");
            videos.add(url);
        }

        return videos;
    }

    private Gender parseGender(String gender) {
        if (gender.equals("male")) {
            return Gender.male;
        } else {
            return Gender.female;
        }
    }

    private Type parseType(String type) {
        if (type.toLowerCase().equals("cat")) {
            return Type.Cat;
        } else if (type.toLowerCase().equals("dog")) {
            return Type.Dog;
        }
        return Type.Unknown;
    }

    @Override
    public String toString() {
        String gender;
        String type;
        if (this.gender == Gender.male) {
            gender = "Macho";
        } else {
            gender = "Hembra";
        }
        if (this.type == Type.Cat) {
            type = "Gato";
        } else {
            type = "Perro";
        }

        return this.name + ": " + type + " " + gender;
    }

    public Image getFirstImage() {
        Image firstImage;
        try {
            firstImage = this.images.get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

        return firstImage;
    }

    public String getTypeString() {
        if (this.type == Type.Cat) {
            return "Gato";
        } else if (this.type == Type.Dog) {
            return "Perro";
        }
        return "Unknown";
    }

    public String getGenderString() {
        if (this.gender == Gender.male) {
            return "Macho";
        } else {
            return "Hembra";
        }
    }

    public String getId() {
        return id;
    }

    public ArrayList<Question> getQuestions() {
        return questions;
    }

    public String getUserId() {
        return userId;
    }

    public PublicationType getPublicationType() {
        return publicationType;
    }

    public void setPublicationType(PublicationType publicationType) {
        this.publicationType = publicationType;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }


    public enum Type {
        Cat,
        Dog,
        Unknown
    }


    public enum Gender {
        male,
        female
    }

    public class Image {
        String thumbUrl;
        String mediumUrl;
        String originalUrl;

        public Image() {
        }

        public String getMediumUrl() {
            return mediumUrl;
        }

        public void setMediumUrl(String mediumUrl) {
            this.mediumUrl = mediumUrl;
        }

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public String getThumbUrl() {
            return thumbUrl;
        }

        public void setThumbUrl(String thumbUrl) {
            this.thumbUrl = thumbUrl;
        }

        public void fromJson(JSONObject object) throws JSONException {
            thumbUrl = object.getString("thumb_url");
            mediumUrl = object.getString("medium_url");
            originalUrl = object.getString("original_url");
        }
    }

    public enum PublicationType {
        ADOPTION("adoption"), LOSS("lost"), FOUND("found");

        private String name;

        PublicationType(String s) {
            name = s;
        }

        public static PublicationType fromString(String s) {
            if (s != null) {
                if (s.equals("lost")) {
                    return LOSS;
                } else {
                    if (s.equals("found")) {
                        return FOUND;
                    }
                }
            }
            return ADOPTION;
        }

        public String toString() {
            return this.name;
        }

        public boolean equalsName(String otherName) {
            return (otherName != null) && name.equals(otherName);
        }
    }
}
