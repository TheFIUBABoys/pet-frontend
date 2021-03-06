package com.fiuba.tdp.petadopt.fragments.detail;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.fiuba.tdp.petadopt.R;
import com.fiuba.tdp.petadopt.activities.MainActivity;
import com.fiuba.tdp.petadopt.fragments.AdopterResultFragment;
import com.fiuba.tdp.petadopt.fragments.detail.questions.AskQuestionFragment;
import com.fiuba.tdp.petadopt.fragments.detail.questions.QuestionsFragment;
import com.fiuba.tdp.petadopt.fragments.dialog.ConfirmDialogDelegate;
import com.fiuba.tdp.petadopt.fragments.dialog.ConfirmDialogFragment;
import com.fiuba.tdp.petadopt.model.Pet;
import com.fiuba.tdp.petadopt.model.Question;
import com.fiuba.tdp.petadopt.model.User;
import com.fiuba.tdp.petadopt.service.PetsClient;
import com.fiuba.tdp.petadopt.util.DateUtils;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.rey.material.widget.EditText;
import com.rey.material.widget.FloatingActionButton;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PetDetailFragment extends Fragment {
    private Pet pet;
    private ArrayList<ImageView> imageViews = new ArrayList<>();
    private HorizontalGridView mHorizontalGridView;
    private int mScrollState = RecyclerView.SCROLL_STATE_IDLE;
    private ProgressDialog progress;
    FloatingActionButton askAdoptionButton;
    FloatingActionButton shareButton;
    Button askQuestionButton;


    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            mScrollState = newState;
        }
    };
    private View rootView;
    private RelativeLayout sampleQuestionLayout;


    public PetDetailFragment() {
    }

    public Pet getPet() {
        return pet;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_pet_detail, container, false);
        this.rootView = rootView;

        setupGridView(rootView);
        updateViewsForOwnership(rootView);

        setAdoptionButton();

        setupTextViews(rootView);

        Button showMapButton = (Button) rootView.findViewById(R.id.show_map_button);
        showMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowLocationMapFragment mapFragment = new ShowLocationMapFragment();
                mapFragment.setPetLocation(pet.getLocation());
                FragmentTransaction ft = getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.add(R.id.content_frame, mapFragment, "Questions");
                ft.addToBackStack(null);
                ft.commit();
            }
        });

        shareButton = (FloatingActionButton) rootView.findViewById(R.id.share_pet);
        shareButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = buildShareTitle(pet);
                String description = buildShareDescription(pet);
                ShareLinkContent linkContent = new ShareLinkContent.Builder()
                        .setContentTitle(title)
                        .setContentDescription(description)
                        .setImageUrl(Uri.parse(pet.getFirstImage().getMediumUrl()))
                        .setContentUrl(Uri.parse(getString(R.string.google_play_app_url)))
                        .build();

                ShareDialog.show(PetDetailFragment.this, linkContent);
            }
        });

        loadQuestions(rootView);
        ((MainActivity) getActivity()).setShouldShowReportButton(true);
        getActivity().invalidateOptionsMenu();
        return rootView;
    }


    private String buildShareTitle(Pet pet) {
        String title = "";
        switch (pet.getPublicationType()) {
            case ADOPTION:
                title = String.format(getString(R.string.adoption_share_title), pet.getName());
                break;
            case LOSS:
                title = String.format(getString(R.string.loss_share_title), pet.getName());
                break;
            case FOUND:
                if (pet.getName() == null || pet.getName().isEmpty()) {
                    title = String.format(getString(R.string.found_share_title), "esta mascota");
                } else {
                    title = String.format(getString(R.string.found_share_title), pet.getName());
                }
                break;
        }
        return title;
    }

    private String buildShareDescription(Pet pet) {
        String description =  buildShareTitle(pet) + ". ";
        switch (pet.getPublicationType()) {
            case ADOPTION:
                description += getString(R.string.adoption_share_description);
                break;
            case LOSS:
                description = getString(R.string.loss_share_description);
                break;
            case FOUND:
                description = getString(R.string.found_share_description);
                break;
        }
        return description;
    }

    private void setupGridView(View rootView) {
        RecyclerView.Adapter adapter = new HorizontalGridViewAdapter(pet.getImages());
        mHorizontalGridView = (HorizontalGridView) rootView.findViewById(R.id.horizontal_gridView);
        mHorizontalGridView.setAdapter(adapter);
        mHorizontalGridView.setWindowAlignment(HorizontalGridView.WINDOW_ALIGN_BOTH_EDGE);
        mHorizontalGridView.setWindowAlignmentOffsetPercent(35);
        mHorizontalGridView.addOnScrollListener(mScrollListener);
        mHorizontalGridView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    private void updateViewsForOwnership(View rootView) {
        askQuestionButton = (Button) rootView.findViewById(R.id.ask_question);
        askAdoptionButton = (FloatingActionButton) rootView.findViewById(R.id.adopt_pet);
        askAdoptionButton.setVisibility(View.VISIBLE);
        if ((pet != null) && User.user().ownsPet(pet)) {
            askQuestionButton.setVisibility(View.GONE);
        }
    }

    private void loadQuestions(final View rootView) {
        progress = new ProgressDialog(getActivity());
        progress.setTitle(R.string.loading);
        progress.show();

        // TODO: OMG scope issues
        final PetDetailFragment previous_fragment = this;
        PetsClient.instance().getQuestions(pet.getId(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                progress.dismiss();
                try {
                    pet.loadQuestionsFromJson(response);
                    setupSampleQuestion(rootView);
                    final Button askQuestionButton = (Button) rootView.findViewById(R.id.ask_question);
                    askQuestionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AskQuestionFragment askQuestionFragment = new AskQuestionFragment();
                            askQuestionFragment.setPreviousFragment(previous_fragment);
                            askQuestionFragment.setPet(pet);
                            FragmentTransaction ft = getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            ft.add(R.id.content_frame, askQuestionFragment, "Choose location");
                            ft.addToBackStack(null);
                            ft.commit();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("Error parsing pet", e.getLocalizedMessage());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                progress.dismiss();
                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    Toast.makeText(getActivity(), R.string.auth_error, Toast.LENGTH_LONG).show();
                    ((MainActivity) getActivity()).goBackToLogin();
                } else {
                    Toast.makeText(getActivity(), R.string.try_again, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                progress.dismiss();
                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    Toast.makeText(getActivity(), R.string.auth_error, Toast.LENGTH_LONG).show();
                    ((MainActivity) getActivity()).goBackToLogin();
                } else {
                    Toast.makeText(getActivity(), R.string.try_again, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                progress.dismiss();
                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    Toast.makeText(getActivity(), R.string.auth_error, Toast.LENGTH_LONG).show();
                    ((MainActivity) getActivity()).goBackToLogin();
                } else {
                    Toast.makeText(getActivity(), R.string.try_again, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void setAdoptionButton() {
        if (!pet.getPublished()) {
            askAdoptionButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), R.string.pet_already_adopted, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            if (User.user().ownsPet(pet)) {
                askAdoptionButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final AdopterResultFragment adopterResultFragment = new AdopterResultFragment();
                        adopterResultFragment.setPet(pet);
                        PetsClient client = PetsClient.instance();
                        progress = new ProgressDialog(getActivity());
                        progress.setTitle(R.string.loading);
                        progress.show();
                        client.getAdoptersForPet(pet.getId(), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int code, Header[] headers, JSONArray body) {
                                progress.dismiss();
                                adopterResultFragment.setResults(body);
                                adopterResultFragment.onStart();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                progress.dismiss();
                                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                                    Toast.makeText(getActivity(), R.string.auth_error, Toast.LENGTH_LONG).show();
                                    ((MainActivity) getActivity()).goBackToLogin();
                                } else {
                                    Toast.makeText(getActivity(), R.string.try_again, Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                                progress.dismiss();
                                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                                    Toast.makeText(getActivity(), R.string.auth_error, Toast.LENGTH_LONG).show();
                                    ((MainActivity) getActivity()).goBackToLogin();
                                } else {
                                    Toast.makeText(getActivity(), R.string.try_again, Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                progress.dismiss();
                                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                                    Toast.makeText(getActivity(), R.string.auth_error, Toast.LENGTH_LONG).show();
                                    ((MainActivity) getActivity()).goBackToLogin();
                                } else {
                                    Toast.makeText(getActivity(), R.string.try_again, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        getActivity().setTitle(pet.getName() + " - " + getActivity().getString(R.string.requesters_title));
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        ft.add(R.id.content_frame, adopterResultFragment, "Adopter Result Fragment");
                        ft.addToBackStack(null);
                        ft.commit();
                    }
                });
            } else {
                askAdoptionButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String dialogMessage;
                        if (pet.getPublicationType() == Pet.PublicationType.ADOPTION) {
                            dialogMessage = getString(R.string.confirm_adoption_request);
                        } else {
                            dialogMessage = getString(R.string.confirm_find_notification);
                        }
                        ConfirmDialogFragment dialog = new ConfirmDialogFragment(dialogMessage, new ConfirmDialogDelegate() {
                            @Override
                            public void onConfirm(DialogInterface dialog, int id) {
                                if (User.user().missingInfo()) {
                                    Toast.makeText(getActivity(), R.string.user_missing_info, Toast.LENGTH_LONG).show();
                                } else {
                                    PetsClient client = PetsClient.instance();
                                    progress.show();
                                    client.askForAdoption(pet.getId(), new AsyncHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                            progress.dismiss();
                                            Toast.makeText(getActivity(), R.string.ask_for_adoption_success, Toast.LENGTH_LONG).show();
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                            if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                                                Toast.makeText(getActivity(), R.string.auth_error, Toast.LENGTH_LONG).show();
                                                ((MainActivity) getActivity()).goBackToLogin();
                                            } else {
                                                Toast.makeText(getActivity(), R.string.ask_for_adoption_error, Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onReject(DialogInterface dialog, int id) {
                                Log.d("RequestAdoptionDialog", "Rejected");
                            }

                            @Override
                            public String getConfirmMessage() {
                                return getString(R.string.confirm_adoption_request_message);
                            }

                            @Override
                            public String getRejectMessage() {
                                return getString(R.string.reject_adoption_request_message);
                            }
                        });
                        dialog.show(getFragmentManager(), "RequestAdoptionDialog");
                    }
                });
            }
        }
    }

    public void setupSampleQuestion(final View rootView) {
        //FIXME - Copypasted from Adapter, see if we can join both pieces
        RelativeLayout questionLayout = (RelativeLayout) rootView.findViewById(R.id.question_layout);
        if (pet.getQuestions() != null && pet.getQuestions().size() > 0) {
            TextView questionTextView = (TextView) questionLayout.findViewById(R.id.question);
            TextView questionDateTextView = (TextView) questionLayout.findViewById(R.id.question_date);
            TextView questionAskerTextView = (TextView) questionLayout.findViewById(R.id.asker_name);
            TextView answerTextView = (TextView) questionLayout.findViewById(R.id.answer);
            TextView answerDateTextView = (TextView) questionLayout.findViewById(R.id.answer_date);
            Button answerButton = (Button) questionLayout.findViewById(R.id.answer_question_button);

            Question question = pet.getQuestions().get(0);
            questionTextView.setText(question.getText());
            questionDateTextView.setText(DateUtils.stringFromDateForQuestionList(question.getCreatedAt()));
            questionAskerTextView.setText(question.getAsker());

            if (question.getAnswer() == null) {
                answerTextView.setVisibility(View.GONE);
                answerDateTextView.setVisibility(View.GONE);
                answerButton.setVisibility(View.GONE);
            } else {
                answerTextView.setVisibility(View.VISIBLE);
                answerDateTextView.setVisibility(View.VISIBLE);
                answerTextView.setText(question.getAnswer().getText());
                answerDateTextView.setText(DateUtils.stringFromDateForQuestionList(question.getAnswer().getCreatedAt()));
                questionLayout.removeView(answerButton);
            }

            Button showQuestionsButton = (Button) rootView.findViewById(R.id.show_questions);
            showQuestionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    QuestionsFragment questionsFragment = new QuestionsFragment();
                    questionsFragment.petDetailFragment = PetDetailFragment.this;
                    questionsFragment.setPet(pet);
                    FragmentTransaction ft = getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.add(R.id.content_frame, questionsFragment, "Choose location");
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });

            Button reportButton = (Button) questionLayout.findViewById(R.id.report_button);
            reportButton.setVisibility(View.GONE);
        } else {
            this.sampleQuestionLayout = (RelativeLayout) rootView.findViewById(R.id.sample_question_layout);
            RelativeLayout rootLayout = (RelativeLayout) rootView.findViewById(R.id.root_layout);

            rootLayout.removeView(sampleQuestionLayout);
        }
    }

    private void setupTextViews(View rootView) {
        EditText nameField = (EditText) rootView.findViewById(R.id.name);
        setPetField(nameField, pet.getName());

        EditText typeField = (EditText) rootView.findViewById(R.id.type);
        setPetField(typeField, pet.getTypeString());

        EditText ageField = (EditText) rootView.findViewById(R.id.age);
        setPetField(ageField, pet.getAge());

        EditText genderField = (EditText) rootView.findViewById(R.id.gender);
        setPetField(genderField, pet.getGenderString());

        EditText colorsField = (EditText) rootView.findViewById(R.id.colors);
        setPetField(colorsField, pet.getColors());

        EditText descriptionField = (EditText) rootView.findViewById(R.id.description);
        setPetField(descriptionField, pet.getDescription());

        EditText videoField = (EditText) rootView.findViewById(R.id.videos);
        setVideoField(videoField, pet.getVideos());

        EditText vaccinesField = (EditText) rootView.findViewById(R.id.vaccines);
        EditText petsRelationshipField = (EditText) rootView.findViewById(R.id.pets_relationship);
        EditText kidsRelationshipField = (EditText) rootView.findViewById(R.id.kids_relationship);
        EditText transitHomeField = (EditText) rootView.findViewById(R.id.transit);
        if (pet.getPublicationType() == Pet.PublicationType.ADOPTION) {
            setBooleanField(vaccinesField, pet.getVaccinated());
            setBooleanField(petsRelationshipField, pet.getPetFriendly());
            setBooleanField(kidsRelationshipField, pet.getChildrenFriendly());
            setBooleanField(transitHomeField, pet.getNeedsTransitHome());
        } else {
            vaccinesField.setVisibility(View.GONE);
            petsRelationshipField.setVisibility(View.GONE);
            kidsRelationshipField.setVisibility(View.GONE);
            transitHomeField.setVisibility(View.GONE);
        }
    }

    private void setBooleanField(EditText field, Boolean condition) {
        if (condition) {
            field.setText(R.string.true_value);
        } else {
            field.setText(R.string.false_value);
        }
        field.setKeyListener(null);

    }

    private void setVideoField(EditText videoField, ArrayList<String> videos) {
        if (videos.size() == 0) {
            videoField.setVisibility(View.GONE);
        } else {
            String videoString = "";
            for (int i = 0; i < videos.size(); i++) {
                if (!videoString.isEmpty()) {
                    videoString += "\n";
                }
                videoString += videos.get(i);
            }
            videoField.setText(videoString);
        }
        videoField.setKeyListener(null);
    }

    private void setPetField(EditText field, String text) {
        if (text.isEmpty()) {
            field.setVisibility(View.GONE);
        } else {
            field.setText(text);
        }
        field.setKeyListener(null);
    }


    public void setPet(Pet pet) {
        this.pet = pet;
    }


    public void reload() {
        if (this.pet.getQuestions().size() == 0) {
            RelativeLayout rootLayout = (RelativeLayout) this.rootView.findViewById(R.id.root_layout);
            rootLayout.addView(sampleQuestionLayout);
        }

        loadQuestions(this.rootView);
    }


    private class HorizontalGridViewAdapter extends RecyclerView.Adapter {
        private ArrayList<Pet.Image> images;

        public HorizontalGridViewAdapter(ArrayList<Pet.Image> images) {
            super();
            this.images = images;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            ImageView imageView = new ImageView(getContext());
            imageView.setAdjustViewBounds(true);
            imageView.setPadding(8, 8, 8, 8);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FullScreenImageFragment fullScreenImageFragment = new FullScreenImageFragment();
                    ImageView view = (ImageView) v;
                    fullScreenImageFragment.setImageDrawable(view.getDrawable());
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.add(R.id.content_frame, fullScreenImageFragment, "Image fullscreen");
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });
            imageViews.add(imageView);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            ImageView imageView = ((ImageView) viewHolder.itemView);
            Picasso.with(getContext())
                    .load(images.get(i).getOriginalUrl())
                    .placeholder(R.drawable.icon)
                    .error(R.drawable.icon)
                    .into(imageView);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }
    }

    private class ImageViewHolder extends ViewHolder {
        public ImageViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
