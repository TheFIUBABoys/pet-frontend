package com.fiuba.tdp.petadopt.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.fiuba.tdp.petadopt.R;
import com.fiuba.tdp.petadopt.activities.MainActivity;
import com.fiuba.tdp.petadopt.model.Adopter;
import com.fiuba.tdp.petadopt.model.Pet;
import com.fiuba.tdp.petadopt.service.AdopterConfirmDelegate;
import com.fiuba.tdp.petadopt.service.AdopterListItemAdapter;
import com.fiuba.tdp.petadopt.service.PetsClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdopterResultFragment extends Fragment {

    protected ListView lv;
    protected List<Adopter> adopters = null;
    protected Pet pet;

    public AdopterResultFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_results, container, false);
        lv = (ListView) rootView.findViewById(R.id.pet_list);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        renderResults();
    }

    public void setResults(JSONArray body) {
        adopters = parseAdopters(body);
    }

    private void renderResults() {
        if (adopters != null && adopters.size() != 0) {
            final AdopterListItemAdapter adapter = new AdopterListItemAdapter(getActivity(), adopters);
            adapter.setAdopterConfirmDelegate(new AdopterConfirmDelegate() {
                @Override
                public void confirmAdoption(Adopter adopter) {
                    PetsClient client = PetsClient.instance();
                    client.markAsAdopted(pet.getId(), adopter.getId(), new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            String message = "";
                            if (pet.getPublicationType() == Pet.PublicationType.ADOPTION) {
                                message = getString(R.string.mark_adoption_success);
                            } else {
                                message = getString(R.string.mark_find_success);
                            }
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            MainActivity a = (MainActivity) getActivity();
                            a.goBackToHome();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Toast.makeText(getContext(),R.string.mark_adoption_error,Toast.LENGTH_LONG).show();
                        }
                    });

                }
            });
            lv.setAdapter(adapter);
            getActivity().findViewById(R.id.no_results).setVisibility(View.INVISIBLE);
        } else {
            ArrayAdapter adapter = new AdopterListItemAdapter(getActivity(), new ArrayList<Adopter>());
            lv.setAdapter(adapter);
            getActivity().findViewById(R.id.no_results).setVisibility(View.VISIBLE);
        }
    }

    private ArrayList<Adopter> parseAdopters(JSONArray adoptersArray) {
        ArrayList<Adopter> adopters = new ArrayList<>(adoptersArray.length());
        for(int i = 0; i < adoptersArray.length(); i++) {
            try {
                JSONObject adoptantJSON = adoptersArray.getJSONObject(i);
                Adopter adopter = new Adopter();
                adopter.loadFromJSON(adoptantJSON);
                adopters.add(i, adopter);
            } catch (JSONException e) {
                // FIXME
                Log.e("Error parsing pet",e.getLocalizedMessage());
            }
        }

        return adopters;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
    }
}
